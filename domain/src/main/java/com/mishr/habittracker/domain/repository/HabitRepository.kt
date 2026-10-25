package com.mishr.habittracker.domain.repository

import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.domain.model.Habit
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing habits.
 */
interface HabitRepository {
    fun getAllHabits(): Flow<List<Habit>>
    fun getHabitById(id: String): Flow<Habit?>
    suspend fun createHabit(habit: Habit): AppResult<Unit>
    suspend fun updateHabit(habit: Habit): AppResult<Unit>
    suspend fun deleteHabit(id: String): AppResult<Unit>
}
