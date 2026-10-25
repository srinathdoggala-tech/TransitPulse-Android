package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.HabitCompletion
import com.mishr.habittracker.domain.repository.HabitCompletionRepository
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import java.util.UUID
import javax.inject.Inject

class ToggleHabitCompletionUseCase @Inject constructor(
    private val completionRepository: HabitCompletionRepository
) {
    suspend operator fun invoke(habitId: String, date: LocalDate, forceState: Boolean? = null): AppResult<Unit> {
        val completions = completionRepository.getCompletionsForHabit(habitId).first()
        val isCurrentlyCompleted = completions.any { it.completedDate == date }

        val targetState = forceState ?: !isCurrentlyCompleted

        if (isCurrentlyCompleted == targetState) return AppResult.Success(Unit)

        return if (targetState) {
            val completion = HabitCompletion(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                completedDate = date,
                completedAt = Clock.System.now()
            )
            completionRepository.markComplete(completion)
        } else {
            completionRepository.unmarkComplete(habitId, date)
        }
    }
}
