package com.mishr.habittracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.mishr.habittracker.core.database.entity.HabitEntity
import com.mishr.habittracker.core.database.entity.ChecklistItemEntity
import com.mishr.habittracker.core.database.entity.HabitWithChecklist
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Transaction
    @Query("SELECT * FROM habits")
    fun getAllHabitsWithChecklist(): Flow<List<HabitWithChecklist>>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitWithChecklistById(id: String): Flow<HabitWithChecklist?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChecklistItems(items: List<ChecklistItemEntity>)
    
    @Query("DELETE FROM checklist_items WHERE habitId = :habitId")
    fun deleteChecklistItemsForHabit(habitId: String)

    @Update
    fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    fun deleteHabitById(id: String)

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItemsSync(): List<ChecklistItemEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitSyncById(id: String): HabitEntity?

    @Query("SELECT * FROM checklist_items WHERE habitId = :habitId")
    fun getChecklistItemsForHabitSync(habitId: String): List<ChecklistItemEntity>
    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsSync(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: HabitEntity)

    @Delete
    fun delete(entity: HabitEntity)

}
