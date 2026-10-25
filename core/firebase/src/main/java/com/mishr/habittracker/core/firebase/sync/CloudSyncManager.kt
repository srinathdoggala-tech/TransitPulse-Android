package com.mishr.habittracker.core.firebase.sync

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mishr.habittracker.core.common.dispatcher.DispatcherProvider
import com.mishr.habittracker.core.database.dao.CategoryDao
import com.mishr.habittracker.core.database.dao.HabitDao
import com.mishr.habittracker.core.database.dao.HabitCompletionDao
import com.mishr.habittracker.core.database.dao.ChecklistItemCompletionDao
import com.mishr.habittracker.core.database.dao.PendingSyncDao
import com.mishr.habittracker.core.database.HabitTrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: HabitTrackerDatabase,
    private val dispatcherProvider: DispatcherProvider,
    private val categoryDao: CategoryDao,
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
        private val checklistCompletionDao: ChecklistItemCompletionDao,
    private val pendingSyncDao: PendingSyncDao
) {
    private var categoryListener: ListenerRegistration? = null
    private var habitListener: ListenerRegistration? = null
    private var completionListeners = mutableMapOf<String, ListenerRegistration>()
    private var checklistCompletionListeners = mutableMapOf<String, ListenerRegistration>()

    private var _userId: String? = null
    private var _scope: CoroutineScope? = null

    fun startListening(userId: String, scope: CoroutineScope) {
        if (categoryListener != null) return
        _userId = userId
        _scope = scope
        val userRef = firestore.collection("users").document(userId)

        categoryListener = userRef.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            scope.launch(dispatcherProvider.io) {
                for (change in snapshot.documentChanges) {
                    val data = change.document.data
                    val id = data["id"] as? String ?: continue
                    val incomingTs = (data["updatedAt"] as? com.google.firebase.Timestamp)?.let { it.seconds * 1000 + it.nanoseconds / 1000000 } ?: 0L
                    if (!shouldOverwrite(id, incomingTs)) continue
                    
                    val entity = EntityMapper.mapToCategory(data) ?: continue
                    if (change.type == DocumentChange.Type.REMOVED) {
                        categoryDao.delete(entity)
                    } else {
                        categoryDao.upsert(entity)
                    }
                }
            }
        }

        habitListener = userRef.collection("habits").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            scope.launch(dispatcherProvider.io) {
                for (change in snapshot.documentChanges) {
                    val data = change.document.data
                    val habitId = data["id"] as? String ?: continue
                    val incomingTs = (data["updatedAt"] as? com.google.firebase.Timestamp)?.let { it.seconds * 1000 + it.nanoseconds / 1000000 } ?: 0L
                    
                    if (change.type == DocumentChange.Type.REMOVED) {
                        if (shouldOverwrite(habitId, incomingTs)) {
                            habitDao.getHabitSyncById(habitId)?.let { habitDao.delete(it) }
                        }
                        stopHabitSubListeners(habitId)
                    } else {
                        if (shouldOverwrite(habitId, incomingTs)) {
                            val habit = EntityMapper.mapToHabit(data) ?: continue
                            val items = EntityMapper.mapToChecklistItems(data, habitId)
                            database.runInTransaction {
                                habit.categoryId?.takeIf { it.isNotBlank() }?.let { catId ->
                                    if (categoryDao.getCategorySyncById(catId) == null) {
                                        categoryDao.insertCategory(
                                            com.mishr.habittracker.core.database.entity.CategoryEntity(
                                                id = catId,
                                                name = "Syncing...",
                                                colorTag = "#FF000000"
                                            )
                                        )
                                    }
                                }
                                try {
                                    habitDao.upsert(habit)
                                    val currentItems = habitDao.getChecklistItemsForHabitSync(habitId)
                                    habitDao.deleteChecklistItemsForHabit(habitId)
                                    habitDao.insertChecklistItems(items)
                                } catch (e: android.database.sqlite.SQLiteConstraintException) {
                                    // Ignore constraint violations to prevent crash
                                }
                            }
                        }
                        startHabitSubListeners(userId, habitId, scope)
                    }
                }
            }
        }
    }

    private fun startHabitSubListeners(userId: String, habitId: String, scope: CoroutineScope) {
        val userRef = firestore.collection("users").document(userId)
        if (!completionListeners.containsKey(habitId)) {
            val reg = userRef.collection("habits").document(habitId).collection("completions").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(dispatcherProvider.io) {
                    for (change in snapshot.documentChanges) {
                        val data = change.document.data
                        val id = data["id"] as? String ?: continue
                        val incomingTs = (data["updatedAt"] as? com.google.firebase.Timestamp)?.let { it.seconds * 1000 + it.nanoseconds / 1000000 } ?: 0L
                        if (!shouldOverwrite(id, incomingTs)) continue
                        val entity = EntityMapper.mapToHabitCompletion(data) ?: continue
                        try {
                            if (change.type == DocumentChange.Type.REMOVED) {
                                completionDao.delete(entity)
                            } else {
                                completionDao.upsert(entity)
                            }
                        } catch (e: android.database.sqlite.SQLiteConstraintException) {
                            // Habit or parent might have been deleted concurrently, ignore
                        }
                    }
                }
            }
            completionListeners[habitId] = reg
        }

        if (!checklistCompletionListeners.containsKey(habitId)) {
            val reg = userRef.collection("habits").document(habitId).collection("checklistCompletions").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(dispatcherProvider.io) {
                    for (change in snapshot.documentChanges) {
                        val data = change.document.data
                        val id = data["id"] as? String ?: continue
                        val incomingTs = (data["updatedAt"] as? com.google.firebase.Timestamp)?.let { it.seconds * 1000 + it.nanoseconds / 1000000 } ?: 0L
                        if (!shouldOverwrite(id, incomingTs)) continue
                        val entity = EntityMapper.mapToChecklistCompletion(data) ?: continue
                        try {
                            if (change.type == DocumentChange.Type.REMOVED) {
                                checklistCompletionDao.delete(entity)
                            } else {
                                checklistCompletionDao.upsert(entity)
                            }
                        } catch (e: android.database.sqlite.SQLiteConstraintException) {
                            // Habit or checklist item might have been deleted, ignore
                        }
                    }
                }
            }
            checklistCompletionListeners[habitId] = reg
        }
    }

    private fun stopHabitSubListeners(habitId: String) {
        completionListeners[habitId]?.remove()
        completionListeners.remove(habitId)
        checklistCompletionListeners[habitId]?.remove()
        checklistCompletionListeners.remove(habitId)
    }

    internal suspend fun shouldOverwrite(entityId: String, incomingTimestamp: Long): Boolean {
        // Find if we have a pending outbox write for this entity.
        // If we do, and its timestamp is NEWER than the incoming data, we REJECT the incoming data.
        val pendingEvents = pendingSyncDao.getSyncEventsForEntity(entityId)
        if (pendingEvents.isEmpty()) return true
        val newestLocalPending = pendingEvents.maxOf { it.timestamp }
        return incomingTimestamp >= newestLocalPending
    }

    fun stopListening() {
        categoryListener?.remove()
        categoryListener = null
        habitListener?.remove()
        habitListener = null
        completionListeners.values.forEach { it.remove() }
        completionListeners.clear()
        checklistCompletionListeners.values.forEach { it.remove() }
        checklistCompletionListeners.clear()
        _userId = null
        _scope = null
    }
}
