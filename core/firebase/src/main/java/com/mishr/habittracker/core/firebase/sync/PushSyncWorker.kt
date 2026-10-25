package com.mishr.habittracker.core.firebase.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mishr.habittracker.core.database.HabitTrackerDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.tasks.await

@HiltWorker
class PushSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: HabitTrackerDatabase,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            // If not logged in, we can't push. Return success so it doesn't infinitely retry.
            // Alternatively, return failure. Since Guest mode shouldn't push, success is safe.
            return Result.success()
        }
        val userId = currentUser.uid

        val pendingEvents = database.pendingSyncDao().getAllPendingSyncEvents()
        if (pendingEvents.isEmpty()) {
            return Result.success()
        }

        val userRef = firestore.collection("users").document(userId)

        try {
            // Firestore limit is 500 writes per batch.
            val chunks = pendingEvents.chunked(400)
            
            for (chunk in chunks) {
                val batch = firestore.batch()
                
                for (event in chunk) {
                    if (event.entityId.isBlank()) continue
                    when (event.entityType) {
                        "HABIT" -> {
                            val docRef = userRef.collection("habits").document(event.entityId)
                            if (event.operation == "DELETE") {
                                batch.delete(docRef)
                            } else {
                                val habit = database.habitDao().getHabitSyncById(event.entityId)
                                if (habit != null) {
                                    val items = database.habitDao().getChecklistItemsForHabitSync(event.entityId)
                                    val map = EntityMapper.habitToMap(habit, items)
                                    batch.set(docRef, map, SetOptions.merge())
                                }
                            }
                        }
                        "CATEGORY" -> {
                            val docRef = userRef.collection("categories").document(event.entityId)
                            if (event.operation == "DELETE") {
                                batch.delete(docRef)
                            } else {
                                val cat = database.categoryDao().getCategorySyncById(event.entityId)
                                if (cat != null) {
                                    val map = EntityMapper.categoryToMap(cat)
                                    batch.set(docRef, map, SetOptions.merge())
                                }
                            }
                        }
                        "HABIT_COMPLETION" -> {
                            val habitId = event.parentEntityId ?: continue
                            val docRef = userRef.collection("habits").document(habitId)
                                .collection("completions").document(event.entityId)
                                
                            if (event.operation == "DELETE") {
                                batch.delete(docRef)
                            } else {
                                val comp = database.habitCompletionDao().getCompletionSyncById(event.entityId)
                                if (comp != null) {
                                    val map = EntityMapper.habitCompletionToMap(comp)
                                    batch.set(docRef, map, SetOptions.merge())
                                }
                            }
                        }
                        "CHECKLIST_COMPLETION" -> {
                            val habitId = event.parentEntityId ?: continue
                            val docRef = userRef.collection("habits").document(habitId)
                                .collection("checklistCompletions").document(event.entityId)
                                
                            if (event.operation == "DELETE") {
                                batch.delete(docRef)
                            } else {
                                val comp = database.checklistItemCompletionDao().getCompletionSyncById(event.entityId)
                                if (comp != null) {
                                    val map = EntityMapper.checklistCompletionToMap(comp)
                                    batch.set(docRef, map, SetOptions.merge())
                                }
                            }
                        }
                    }
                }
                
                // Commit this chunk
                batch.commit().await()
                
                // Delete processed events
                val idsToDelete = chunk.map { it.id }
                database.pendingSyncDao().deleteSyncEvents(idsToDelete)
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
