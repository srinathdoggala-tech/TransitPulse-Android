package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.DayOfWeek
import com.mishr.habittracker.domain.model.HabitCompletion
import com.mishr.habittracker.domain.model.ScheduleType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Pure function to calculate the current streak of a habit.
 * 
 * Algorithm:
 * 1. Extract a set of unique `LocalDate`s from the completion list.
 * 2. Start iterating backward from `today`.
 * 3. Check if `today` is a "scheduled day" (for DAILY, every day is scheduled; for WEEKLY, only those in scheduledDays).
 * 4. If `today` is scheduled, check if it's completed.
 *    - If completed, streak++ and continue to yesterday.
 *    - If NOT completed, it means the streak might be 0, UNLESS the only missing day is `today`. A streak shouldn't break just because the user hasn't completed today's habit yet. So we allow `today` to be missing without breaking the streak, but we don't increment the streak. We then proceed to yesterday.
 * 5. For all days BEFORE today:
 *    - If the day is scheduled and completed -> streak++
 *    - If the day is scheduled and NOT completed -> streak is broken, return current streak.
 *    - If the day is NOT scheduled -> skip it (don't increment, don't break streak), continue to the day before.
 */
import javax.inject.Inject

class CalculateStreakUseCase @Inject constructor() {

    operator fun invoke(
        completions: List<HabitCompletion>,
        scheduleType: ScheduleType,
        scheduledDays: Set<DayOfWeek>,
        today: LocalDate
    ): Int {
        if (completions.isEmpty()) return 0

        val completedDates = completions.map { it.completedDate }.toSet()
        var streak = 0
        var currentDate = today
        
        // Safety limit to avoid infinite loops in case of unexpected input
        val maxDaysToLookBack = 10000 
        var daysLookedBack = 0

        while (daysLookedBack < maxDaysToLookBack) {
            val isScheduled = isDayScheduled(currentDate, scheduleType, scheduledDays)
            
            if (isScheduled) {
                if (completedDates.contains(currentDate)) {
                    streak++
                } else {
                    // It's a scheduled day and not completed.
                    // Grace logic: only allow missing TODAY (daysLookedBack == 0) without breaking the streak.
                    // Any missed scheduled day in the past breaks it immediately.
                    if (daysLookedBack != 0) {
                        break
                    }
                }
            }
            
            // Move to the previous day
            currentDate = currentDate.minus(1, DateTimeUnit.DAY)
            daysLookedBack++
        }

        return streak
    }

    private fun isDayScheduled(
        date: LocalDate,
        scheduleType: ScheduleType,
        scheduledDays: Set<DayOfWeek>
    ): Boolean {
        if (scheduleType == ScheduleType.DAILY) return true
        
        val ktxDayOfWeek = date.dayOfWeek
        val domainDayOfWeek = when (ktxDayOfWeek) {
            kotlinx.datetime.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
            kotlinx.datetime.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            kotlinx.datetime.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            kotlinx.datetime.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            kotlinx.datetime.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            kotlinx.datetime.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            kotlinx.datetime.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
            else -> throw IllegalStateException("Unknown DayOfWeek: $ktxDayOfWeek")
        }
        
        return scheduledDays.contains(domainDayOfWeek)
    }

    fun calculateBestStreak(
        completions: List<HabitCompletion>,
        scheduleType: ScheduleType,
        scheduledDays: Set<DayOfWeek>,
        today: LocalDate
    ): Int {
        if (completions.isEmpty()) return 0

        val completedDates = completions.map { it.completedDate }.toSet()
        if (completedDates.isEmpty()) return 0

        val sortedDates = completedDates.sorted()
        val firstDate = sortedDates.first()

        var currentStreak = 0
        var maxStreak = 0
        var currentDate = firstDate

        while (currentDate <= today) {
            val isScheduled = isDayScheduled(currentDate, scheduleType, scheduledDays)

            if (isScheduled) {
                if (completedDates.contains(currentDate)) {
                    currentStreak++
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak
                    }
                } else {
                    currentStreak = 0
                }
            }
            
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }

        return maxStreak
    }
}
