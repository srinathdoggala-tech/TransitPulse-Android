package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.repository.HabitRepository
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class UpdateHabitNotesUseCase @Inject constructor(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(habitId: String, notes: String?): AppResult<Unit> {
        val habit = habitRepository.getHabitById(habitId).firstOrNull() 
            ?: return AppResult.Error(com.mishr.habittracker.domain.result.AppError.UnknownError(Exception("Habit not found")))
        
        val updatedHabit = habit.copy(notes = notes?.trim())
        return habitRepository.updateHabit(updatedHabit)
    }
}
