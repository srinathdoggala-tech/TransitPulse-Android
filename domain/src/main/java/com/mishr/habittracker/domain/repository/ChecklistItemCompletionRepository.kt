package com.mishr.habittracker.domain.repository

import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.domain.model.ChecklistItemCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ChecklistItemCompletionRepository {
    fun getCompletionsForHabitAndDate(habitId: String, date: LocalDate): Flow<List<ChecklistItemCompletion>>
    fun getCompletionsForDate(date: LocalDate): Flow<List<ChecklistItemCompletion>>
    suspend fun markItemComplete(completion: ChecklistItemCompletion): AppResult<Unit>
    suspend fun unmarkItemComplete(habitId: String, checklistItemId: String, date: LocalDate): AppResult<Unit>
}
