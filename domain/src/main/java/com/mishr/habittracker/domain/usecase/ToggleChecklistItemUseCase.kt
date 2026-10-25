package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.ChecklistItemCompletion
import com.mishr.habittracker.domain.repository.ChecklistItemCompletionRepository
import com.mishr.habittracker.domain.repository.HabitCompletionRepository
import com.mishr.habittracker.domain.repository.HabitRepository
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Inject

class ToggleChecklistItemUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val checklistItemCompletionRepository: ChecklistItemCompletionRepository,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase
) {
    suspend operator fun invoke(habitId: String, checklistItemId: String, isCompleted: Boolean): AppResult<Unit> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
                // Fetch the current completions BEFORE we modify them, to avoid Room Flow invalidation race conditions
        val currentCompletions = checklistItemCompletionRepository.getCompletionsForHabitAndDate(habitId, today).firstOrNull() ?: emptyList()
        val completedItemIds = currentCompletions.map { it.checklistItemId }.toMutableSet()
        
        if (isCompleted) {
            completedItemIds.add(checklistItemId)
            val completion = ChecklistItemCompletion(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                checklistItemId = checklistItemId,
                completedDate = today
            )
            val result = checklistItemCompletionRepository.markItemComplete(completion)
            if (result is AppResult.Error) return result
        } else {
            completedItemIds.remove(checklistItemId)
            val result = checklistItemCompletionRepository.unmarkItemComplete(habitId, checklistItemId, today)
            if (result is AppResult.Error) return result
        }

        // Check if ALL items are now complete
        val habit = habitRepository.getHabitById(habitId).firstOrNull() ?: return AppResult.Success(Unit)
        if (!habit.checklistEnabled || habit.checklistItems.isEmpty()) return AppResult.Success(Unit)

        val allItemIds = habit.checklistItems.map { it.id }.toSet()
        val allCompleted = completedItemIds.containsAll(allItemIds)
        
        // Use ToggleHabitCompletionUseCase to handle the underlying habit completion record
        // Wait, toggleHabitCompletionUseCase toggles it based on current state.
        // It might be better to explicitly mark complete / unmark complete.
        // Let's invoke it with the desired state.
        return toggleHabitCompletionUseCase(habitId, today, allCompleted)
    }
}
