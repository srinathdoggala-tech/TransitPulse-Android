package com.mishr.habittracker.core.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mishr.habittracker.core.database.HabitTrackerDatabase
import com.mishr.habittracker.core.database.entity.*
import com.mishr.habittracker.domain.model.MigrationStatus
import com.mishr.habittracker.domain.repository.MigrationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

class MigrationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: HabitTrackerDatabase,
    @ApplicationContext private val context: Context
) : MigrationRepository {

    private fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ─── SHARED PULL LOGIC ──────────────────────────────────────────────────────
    // This private function is the single implementation of "fetch Firestore and
    // write into Room". It is called both from migrateGuestDataToCloud (Step 4 of
    // the push flow) and directly from pullCloudDataToLocal.
    // Returns the number of habits fetched (0 means Firestore was also empty).
    private suspend fun fetchFirestoreAndPopulateRoom(userId: String): Int {
        val userRef = firestore.collection("users").document(userId)

        val pulledCategories = userRef.collection("categories").get().await()
        val pulledHabits = userRef.collection("habits").get().await()

        val newCatEntities = pulledCategories.documents.mapNotNull { doc ->
            CategoryEntity(
                id = doc.getString("id") ?: doc.id,
                name = doc.getString("name") ?: return@mapNotNull null,
                colorTag = doc.getString("colorTag") ?: "#FF000000"
            )
        }

        val newHabitEntities = mutableListOf<HabitEntity>()
        val newChecklistItems = mutableListOf<ChecklistItemEntity>()
        val newCompletions = mutableListOf<HabitCompletionEntity>()
        val newChecklistCompletions = mutableListOf<ChecklistItemCompletionEntity>()

        for (doc in pulledHabits.documents) {
            val hId = doc.getString("id") ?: doc.id
            val createdAtTs = doc.getTimestamp("createdAt")
            val createdMs = if (createdAtTs != null)
                createdAtTs.seconds * 1000 + createdAtTs.nanoseconds / 1_000_000
            else System.currentTimeMillis()

            val rawCatId = doc.getString("categoryId")
            val safeCatId = if (newCatEntities.any { it.id == rawCatId }) rawCatId else null

            newHabitEntities.add(
                HabitEntity(
                    id = hId,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description"),
                    scheduleType = doc.getString("scheduleType") ?: "DAILY",
                    scheduledDays = (doc.get("scheduledDays") as? List<String>)
                        ?.joinToString(",") ?: "",
                    colorTag = doc.getString("colorTag") ?: "#FF000000",
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = createdMs,
                    reminderEnabled = doc.getBoolean("reminderEnabled") ?: false,
                    reminderHour = doc.getLong("reminderHour")?.toInt(),
                    reminderMinute = doc.getLong("reminderMinute")?.toInt(),
                    icon = doc.getString("icon") ?: "star",
                    categoryId = safeCatId,
                    notes = doc.getString("notes"),
                    checklistEnabled = doc.getBoolean("checklistEnabled") ?: false
                )
            )

            (doc.get("checklistItems") as? List<Map<String, Any>>)?.forEach { itemMap ->
                newChecklistItems.add(
                    ChecklistItemEntity(
                        id = itemMap["id"] as? String ?: "",
                        habitId = hId,
                        text = itemMap["text"] as? String ?: "",
                        itemOrder = (itemMap["order"] as? Number)?.toInt() ?: 0
                    )
                )
            }

            doc.reference.collection("completions").get().await().documents.forEach { cDoc ->
                val cTs = cDoc.getTimestamp("completedAt")
                val cMs = if (cTs != null) cTs.seconds * 1000 + cTs.nanoseconds / 1_000_000
                          else System.currentTimeMillis()
                newCompletions.add(
                    HabitCompletionEntity(
                        id = cDoc.getString("id") ?: cDoc.id,
                        habitId = hId,
                        completedDate = cDoc.getString("completedDate") ?: "",
                        completedAt = cMs
                    )
                )
            }

            doc.reference.collection("checklistCompletions").get().await().documents.forEach { ccDoc ->
                val ccId = ccDoc.getString("id") ?: ccDoc.id
                val cItemId = ccDoc.getString("checklistItemId") ?: ""
                // Only add if the checklist item actually exists
                if (newChecklistItems.any { it.id == cItemId }) {
                    newChecklistCompletions.add(
                        ChecklistItemCompletionEntity(
                            id = ccId,
                            habitId = hId,
                            checklistItemId = cItemId,
                            completedDate = ccDoc.getString("completedDate") ?: ""
                        )
                    )
                }
            }
        }

        // Atomically replace local tables
        database.runInTransaction {
            val db = database.openHelper.writableDatabase
            db.execSQL("DELETE FROM checklist_item_completions")
            db.execSQL("DELETE FROM habit_completions")
            db.execSQL("DELETE FROM checklist_items")
            db.execSQL("DELETE FROM habits")
            db.execSQL("DELETE FROM categories")

            newCatEntities.forEach { database.categoryDao().insertCategory(it) }
            newHabitEntities.forEach { database.habitDao().insertHabit(it) }
            if (newChecklistItems.isNotEmpty()) {
                database.habitDao().insertChecklistItems(newChecklistItems)
            }
            newCompletions.forEach { database.habitCompletionDao().insertCompletion(it) }
            newChecklistCompletions.forEach { database.checklistItemCompletionDao().insertCompletion(it) }
        }

        return newHabitEntities.size
    }

    // ─── OPERATION 1: PUSH (guest migration) ────────────────────────────────────
    override fun migrateGuestDataToCloud(userId: String): Flow<MigrationStatus> = flow {
        if (!isOnline()) {
            emit(MigrationStatus.Failed("No internet connection", canRetry = true))
            return@flow
        }

        emit(MigrationStatus.InProgress(0.0f))

        try {
            val habits = database.habitDao().getAllHabitsSync()
            val categories = database.categoryDao().getAllCategoriesSync().filter { it.id.isNotBlank() }

            // If completely empty (no guest data), nothing to push – just complete.
            if (habits.isEmpty() && categories.isEmpty()) {
                emit(MigrationStatus.Completed)
                return@flow
            }

            val checklistItems = database.habitDao().getAllChecklistItemsSync()
            val habitCompletions = database.habitCompletionDao().getAllCompletionsSync()
            val checklistItemCompletions =
                database.checklistItemCompletionDao().getAllChecklistItemCompletionsSync()

            val userRef = firestore.collection("users").document(userId)
            val operations = mutableListOf<FirestoreOp>()

            categories.forEach { cat ->
                val data = mapOf(
                    "id" to cat.id,
                    "name" to cat.name,
                    "colorTag" to cat.colorTag,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                operations.add(FirestoreOp(userRef.collection("categories").document(cat.id), data))
            }

            val itemsByHabit = checklistItems.groupBy { it.habitId }
            habits.forEach { habit ->
                val habitItems = itemsByHabit[habit.id]?.map { item ->
                    mapOf("id" to item.id, "text" to item.text, "order" to item.itemOrder)
                } ?: emptyList()

                val data = mutableMapOf<String, Any>(
                    "id" to habit.id,
                    "name" to habit.name,
                    "scheduleType" to habit.scheduleType,
                    "scheduledDays" to habit.scheduledDays.split(",").filter { it.isNotEmpty() },
                    "colorTag" to habit.colorTag,
                    "isActive" to habit.isActive,
                    "createdAt" to com.google.firebase.Timestamp(
                        habit.createdAt / 1000,
                        ((habit.createdAt % 1000) * 1_000_000).toInt()
                    ),
                    "reminderEnabled" to habit.reminderEnabled,
                    "icon" to habit.icon,
                    "checklistEnabled" to habit.checklistEnabled,
                    "checklistItems" to habitItems,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                habit.description?.let { data["description"] = it }
                habit.categoryId?.let { data["categoryId"] = it }
                habit.notes?.let { data["notes"] = it }
                habit.reminderHour?.let { data["reminderHour"] = it }
                habit.reminderMinute?.let { data["reminderMinute"] = it }

                // Explicitly .set() with the local UUID — never .add() — ensures idempotent retries.
                operations.add(FirestoreOp(userRef.collection("habits").document(habit.id), data))
            }

            habitCompletions.forEach { comp ->
                val data = mapOf(
                    "id" to comp.id,
                    "habitId" to comp.habitId,
                    "completedDate" to comp.completedDate,
                    "completedAt" to com.google.firebase.Timestamp(
                        comp.completedAt / 1000,
                        ((comp.completedAt % 1000) * 1_000_000).toInt()
                    ),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                operations.add(
                    FirestoreOp(
                        userRef.collection("habits").document(comp.habitId)
                            .collection("completions").document(comp.id),
                        data
                    )
                )
            }

            checklistItemCompletions.forEach { comp ->
                val data = mapOf(
                    "id" to comp.id,
                    "habitId" to comp.habitId,
                    "checklistItemId" to comp.checklistItemId,
                    "completedDate" to comp.completedDate,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                operations.add(
                    FirestoreOp(
                        userRef.collection("habits").document(comp.habitId)
                            .collection("checklistCompletions").document(comp.id),
                        data
                    )
                )
            }

            // ── Chunk into WriteBatches of max 500 ──
            val chunks = operations.chunked(500)
            chunks.forEachIndexed { index, chunk ->
                val batch = firestore.batch()
                chunk.forEach { op ->
                    batch.set(op.ref, op.data, SetOptions.merge()) // idempotent .set()
                }
                batch.commit().await()
                emit(MigrationStatus.InProgress(0.5f * ((index + 1).toFloat() / max(1, chunks.size).toFloat())))
            }

            // ── Step 4: Pull final cloud state back into local Room ──
            // NOTE: clearAllTables() only happens inside fetchFirestoreAndPopulateRoom's
            // withTransaction block. If THIS call throws (Scenario D), no local data is lost.
            emit(MigrationStatus.InProgress(0.75f))
            fetchFirestoreAndPopulateRoom(userId)

            emit(MigrationStatus.Completed)
        } catch (e: Exception) {
            // This catches BOTH mid-push failures (Scenario C) and post-push pull failures (Scenario D).
            // In both cases clearAllTables() was never reached, so local data is safe.
            emit(MigrationStatus.Failed(e.message ?: "Unknown error", canRetry = true))
        }
    }.flowOn(Dispatchers.IO)

    // ─── OPERATION 2: PULL (sign-in with empty local Room) ──────────────────────
    // Called on EVERY successful Google Sign-In when local Room is empty.
    // Completely independent of the guest-migration-pending flag.
    override suspend fun pullCloudDataToLocal(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            if (!isOnline()) {
                return@withContext Result.failure(Exception("No internet connection"))
            }
            if (database.pendingSyncDao().getAllPendingSyncEvents().isNotEmpty()) {
                // Defensive guard: prevent data wipe if there are unsynced offline edits
                val msg = "Skipping destructive pull: unsynced local data exists."
                android.util.Log.w("MigrationRepository", msg)
                return@withContext Result.failure(Exception(msg))
            }
            try {
                val habitsFound = fetchFirestoreAndPopulateRoom(userId)
                Result.success(habitsFound > 0)
            } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.failure(e)
            }
        }
    }

    // ─── SIGN-OUT ────────────────────────────────────────────────────────────────
    override suspend fun wipeLocalData() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
    }

    private data class FirestoreOp(
        val ref: com.google.firebase.firestore.DocumentReference,
        val data: Map<String, Any>
    )

    override suspend fun deleteCloudData(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userRef = firestore.collection("users").document(userId)
                val batch = firestore.batch()
                
                // Delete categories
                val cats = userRef.collection("categories").get().await()
                cats.forEach { batch.delete(it.reference) }
                
                // Delete habits and their subcollections
                val habits = userRef.collection("habits").get().await()
                for (habit in habits) {
                    val comps = habit.reference.collection("completions").get().await()
                    comps.forEach { batch.delete(it.reference) }
                    val checks = habit.reference.collection("checklistCompletions").get().await()
                    checks.forEach { batch.delete(it.reference) }
                    batch.delete(habit.reference)
                }
                
                // Delete main user doc
                batch.delete(userRef)
                
                // Commit the batch
                batch.commit().await()
                Result.success(Unit)
            } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.failure(e)
            }
        }
    }
}
