package com.mishr.habittracker.core.database.repository

import android.database.sqlite.SQLiteException
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.core.database.HabitTrackerDatabase
import com.mishr.habittracker.core.database.converter.Converters
import com.mishr.habittracker.core.database.entity.PendingSyncEntity
import com.mishr.habittracker.core.database.toDomain
import com.mishr.habittracker.core.database.toEntity
import com.mishr.habittracker.domain.model.HabitCompletion
import com.mishr.habittracker.domain.sync.SyncTrigger
import com.mishr.habittracker.domain.repository.HabitCompletionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject

import com.mishr.habittracker.core.common.dispatcher.DispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Implementation of HabitCompletionRepository backed by Room Database.
 */
class HabitCompletionRepositoryImpl @Inject constructor(
    private val database: HabitTrackerDatabase,
    private val dispatcherProvider: DispatcherProvider,
    private val syncTrigger: SyncTrigger
) : HabitCompletionRepository {

    private val converters = Converters()

    override fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>> {
        return database.habitCompletionDao().getCompletionsForHabit(habitId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllCompletions(): Flow<List<HabitCompletion>> {
        return database.habitCompletionDao().getAllCompletionsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Marks a habit as complete for a given date.
     * Checks for duplicates first; returns AppResult.Error if already completed.
     */
    override suspend fun markComplete(completion: HabitCompletion): AppResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            val dateStr = converters.fromLocalDate(completion.completedDate)
            val existing = database.habitCompletionDao().getCompletionByHabitAndDate(completion.habitId, dateStr)
            
            if (existing != null) {
                // Return clear "already completed" error
                return@withContext AppResult.Error(AppError.DatabaseError("Habit is already marked complete for this date."))
            }

            database.runInTransaction {
                database.habitCompletionDao().insertCompletion(completion.toEntity())
                database.pendingSyncDao().insertSyncEvent(
                    PendingSyncEntity(entityId = completion.id, parentEntityId = completion.habitId, entityType = "HABIT_COMPLETION", operation = "UPSERT")
                )
            }
            syncTrigger.scheduleSync()
            AppResult.Success(Unit)
        } catch (e: SQLiteException) {
            AppResult.Error(AppError.DatabaseError("Failed to mark completion: ${e.message}"))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    override suspend fun unmarkComplete(habitId: String, date: LocalDate): AppResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            val dateStr = converters.fromLocalDate(date)
            // Fetch the existing completion to get its ID before we delete it
            val existing = database.habitCompletionDao().getCompletionByHabitAndDate(habitId, dateStr)
            
            if (existing != null) {
                database.runInTransaction {
                    database.habitCompletionDao().deleteCompletionByHabitAndDate(habitId, dateStr)
                    database.pendingSyncDao().insertSyncEvent(
                        PendingSyncEntity(entityId = existing.id, parentEntityId = existing.habitId, entityType = "HABIT_COMPLETION", operation = "DELETE")
                    )
                }
            }
            syncTrigger.scheduleSync()
            AppResult.Success(Unit)
        } catch (e: SQLiteException) {
            AppResult.Error(AppError.DatabaseError("Failed to unmark completion: ${e.message}"))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }
}
