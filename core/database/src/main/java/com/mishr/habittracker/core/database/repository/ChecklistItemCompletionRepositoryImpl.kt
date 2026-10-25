package com.mishr.habittracker.core.database.repository

import com.mishr.habittracker.core.database.HabitTrackerDatabase
import com.mishr.habittracker.core.database.converter.Converters
import com.mishr.habittracker.core.database.entity.PendingSyncEntity
import com.mishr.habittracker.core.database.toDomain
import com.mishr.habittracker.core.database.toEntity
import com.mishr.habittracker.domain.model.ChecklistItemCompletion
import com.mishr.habittracker.domain.sync.SyncTrigger
import com.mishr.habittracker.domain.repository.ChecklistItemCompletionRepository
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import com.mishr.habittracker.core.common.dispatcher.DispatcherProvider
import kotlinx.coroutines.withContext

class ChecklistItemCompletionRepositoryImpl @Inject constructor(
    private val database: HabitTrackerDatabase,
    private val dispatcherProvider: DispatcherProvider,
    private val syncTrigger: SyncTrigger
) : ChecklistItemCompletionRepository {
    private val converters = Converters()

    override fun getCompletionsForHabitAndDate(habitId: String, date: LocalDate): Flow<List<ChecklistItemCompletion>> {
        return database.checklistItemCompletionDao().getCompletionsForHabitAndDate(habitId, converters.fromLocalDate(date)).map { list -> list.map { it.toDomain() } }
    }

    override fun getCompletionsForDate(date: LocalDate): Flow<List<ChecklistItemCompletion>> {
        return database.checklistItemCompletionDao().getCompletionsForDateFlow(converters.fromLocalDate(date)).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun markItemComplete(completion: ChecklistItemCompletion): AppResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            database.runInTransaction {
                database.checklistItemCompletionDao().insertCompletion(completion.toEntity())
                database.pendingSyncDao().insertSyncEvent(
                    PendingSyncEntity(entityId = completion.id, parentEntityId = completion.habitId, entityType = "CHECKLIST_COMPLETION", operation = "UPSERT")
                )
            }
            syncTrigger.scheduleSync()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    override suspend fun unmarkItemComplete(habitId: String, checklistItemId: String, date: LocalDate): AppResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            val dateStr = converters.fromLocalDate(date)
            val existing = database.checklistItemCompletionDao().getCompletionSync(habitId, checklistItemId, dateStr)
            if (existing != null) {
                database.runInTransaction {
                    database.checklistItemCompletionDao().deleteCompletion(habitId, checklistItemId, dateStr)
                    database.pendingSyncDao().insertSyncEvent(
                        PendingSyncEntity(entityId = existing.id, parentEntityId = existing.habitId, entityType = "CHECKLIST_COMPLETION", operation = "DELETE")
                    )
                }
            }
            syncTrigger.scheduleSync()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }
}
