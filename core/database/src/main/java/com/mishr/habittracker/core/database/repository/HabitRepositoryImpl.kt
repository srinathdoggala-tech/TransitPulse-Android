package com.mishr.habittracker.core.database.repository

import com.mishr.habittracker.core.database.HabitTrackerDatabase
import com.mishr.habittracker.core.database.entity.PendingSyncEntity
import com.mishr.habittracker.core.database.toDomain
import com.mishr.habittracker.core.database.toEntity
import com.mishr.habittracker.core.database.entity.ChecklistItemEntity
import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.sync.SyncTrigger
import com.mishr.habittracker.domain.repository.HabitRepository
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.mishr.habittracker.core.common.dispatcher.DispatcherProvider
import kotlinx.coroutines.withContext

class HabitRepositoryImpl @Inject constructor(
    private val database: HabitTrackerDatabase,
    private val dispatcherProvider: DispatcherProvider,
    private val syncTrigger: SyncTrigger
) : HabitRepository {
    override fun getAllHabits(): Flow<List<Habit>> {
        return database.habitDao().getAllHabitsWithChecklist().map { list -> list.map { it.toDomain() } }
    }

    override fun getHabitById(id: String): Flow<Habit?> {
        return database.habitDao().getHabitWithChecklistById(id).map { it?.toDomain() }
    }

    override suspend fun createHabit(habit: Habit): AppResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            database.runInTransaction {
                database.habitDao().insertHabit(habit.toEntity())
            if (habit.checklistEnabled && habit.checklistItems.isNotEmpty()) {
                val itemEntities = habit.checklistItems.map {
                    ChecklistItemEntity(
                        id = it.id,
                        habitId = habit.id,
                        text = it.text,
                        itemOrder = it.order
                    )
                }
                database.habitDao().insertChecklistItems(itemEntities)
            }
                database.pendingSyncDao().insertSyncEvent(PendingSyncEntity(entityId = habit.id, entityType = "HABIT", operation = "UPSERT"))
            }
            syncTrigger.scheduleSync()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    override suspend fun updateHabit(habit: Habit): AppResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            database.runInTransaction {
                database.habitDao().updateHabit(habit.toEntity())
            
            // Re-insert checklist items (we delete all and re-insert for simplicity)
            database.habitDao().deleteChecklistItemsForHabit(habit.id)
            if (habit.checklistEnabled && habit.checklistItems.isNotEmpty()) {
                val itemEntities = habit.checklistItems.map {
                    ChecklistItemEntity(
                        id = it.id,
                        habitId = habit.id,
                        text = it.text,
                        itemOrder = it.order
                    )
                }
                database.habitDao().insertChecklistItems(itemEntities)
            }
                database.pendingSyncDao().insertSyncEvent(PendingSyncEntity(entityId = habit.id, entityType = "HABIT", operation = "UPSERT"))
            }
            syncTrigger.scheduleSync()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    override suspend fun deleteHabit(id: String): AppResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            database.runInTransaction {
                database.habitDao().deleteHabitById(id)
                database.pendingSyncDao().insertSyncEvent(PendingSyncEntity(entityId = id, entityType = "HABIT", operation = "DELETE"))
            }
            syncTrigger.scheduleSync()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }
}
