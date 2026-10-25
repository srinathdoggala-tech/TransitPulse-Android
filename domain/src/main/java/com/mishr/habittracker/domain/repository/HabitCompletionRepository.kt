package com.mishr.habittracker.domain.repository

import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository interface for managing habit completions.
 */
interface HabitCompletionRepository {
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>>
    fun getAllCompletions(): Flow<List<HabitCompletion>>
    suspend fun markComplete(completion: HabitCompletion): AppResult<Unit>
    suspend fun unmarkComplete(habitId: String, date: LocalDate): AppResult<Unit>
}
