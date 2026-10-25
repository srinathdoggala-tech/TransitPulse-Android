package com.mishr.habittracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mishr.habittracker.core.database.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for HabitCompletion entities.
 */
@Dao
interface HabitCompletionDao {

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedDate DESC")
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    /**
     * Checks if a completion already exists for a given habit and date.
     */
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completedDate = :date LIMIT 1")
    fun getCompletionByHabitAndDate(habitId: String, date: String): HabitCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completedDate = :date")
    fun deleteCompletionByHabitAndDate(habitId: String, date: String)

    @Query("SELECT * FROM habit_completions WHERE id = :id")
    fun getCompletionSyncById(id: String): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllCompletionsSync(): List<HabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: HabitCompletionEntity)

    @Delete
    fun delete(entity: HabitCompletionEntity)

    @Query("SELECT * FROM habit_completions ORDER BY completedDate DESC")
    fun getAllCompletionsFlow(): Flow<List<HabitCompletionEntity>>

}
