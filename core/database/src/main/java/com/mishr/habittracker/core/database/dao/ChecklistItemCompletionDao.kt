package com.mishr.habittracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mishr.habittracker.core.database.entity.ChecklistItemCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemCompletionDao {
    @Query("SELECT * FROM checklist_item_completions WHERE habitId = :habitId AND completedDate = :date")
    fun getCompletionsForHabitAndDate(habitId: String, date: String): Flow<List<ChecklistItemCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCompletion(completion: ChecklistItemCompletionEntity)

    @Query("SELECT * FROM checklist_item_completions WHERE habitId = :habitId AND checklistItemId = :checklistItemId AND completedDate = :date")
    fun getCompletionSync(habitId: String, checklistItemId: String, date: String): ChecklistItemCompletionEntity?

    @Query("DELETE FROM checklist_item_completions WHERE habitId = :habitId AND checklistItemId = :checklistItemId AND completedDate = :date")
    fun deleteCompletion(habitId: String, checklistItemId: String, date: String)

    @Query("SELECT * FROM checklist_item_completions WHERE id = :id")
    fun getCompletionSyncById(id: String): ChecklistItemCompletionEntity?

    @Query("SELECT * FROM checklist_item_completions")
    suspend fun getAllChecklistItemCompletionsSync(): List<ChecklistItemCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: ChecklistItemCompletionEntity)

    @Delete
    fun delete(entity: ChecklistItemCompletionEntity)

    @Query("SELECT * FROM checklist_item_completions WHERE completedDate = :date")
    fun getCompletionsForDateFlow(date: String): Flow<List<ChecklistItemCompletionEntity>>
}
