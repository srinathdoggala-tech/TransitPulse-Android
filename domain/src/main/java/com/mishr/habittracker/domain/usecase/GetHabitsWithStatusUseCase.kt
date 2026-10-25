package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.HabitWithStatus
import com.mishr.habittracker.domain.repository.HabitCompletionRepository
import com.mishr.habittracker.domain.repository.ChecklistItemCompletionRepository
import com.mishr.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class GetHabitsWithStatusUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val completionRepository: HabitCompletionRepository,
    private val checklistItemCompletionRepository: ChecklistItemCompletionRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<HabitWithStatus>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        return combine(
            habitRepository.getAllHabits(),
            completionRepository.getAllCompletions(),
            checklistItemCompletionRepository.getCompletionsForDate(today)
        ) { habits, allCompletions, allChecklistCompletions ->
            val activeHabits = habits.filter { it.isActive }
            
            if (activeHabits.isEmpty()) {
                return@combine emptyList()
            }
            
            val completionsByHabit = allCompletions.groupBy { it.habitId }
            val checklistCompletionsByHabit = allChecklistCompletions.groupBy { it.habitId }
            
            activeHabits.map { habit ->
                val completions = completionsByHabit[habit.id] ?: emptyList()
                val checklistCompletions = checklistCompletionsByHabit[habit.id] ?: emptyList()
                
                val isCompletedToday = completions.any { it.completedDate == today }
                val streak = calculateStreakUseCase(
                    completions = completions,
                    scheduleType = habit.scheduleType,
                    scheduledDays = habit.scheduledDays,
                    today = today
                )
                
                val completedDates = completions.map { it.completedDate }.toSet()
                val bestStreak = calculateStreakUseCase.calculateBestStreak(
                    completions = completions,
                    scheduleType = habit.scheduleType,
                    scheduledDays = habit.scheduledDays,
                    today = today
                )
                
                HabitWithStatus(
                    habit = habit,
                    isCompletedToday = isCompletedToday,
                    currentStreak = streak,
                    bestStreak = bestStreak,
                    completedDates = completedDates,
                    todayChecklistCompletions = checklistCompletions.map { it.checklistItemId }.toSet()
                )
            }
        }.flowOn(Dispatchers.Default)
    }
}
