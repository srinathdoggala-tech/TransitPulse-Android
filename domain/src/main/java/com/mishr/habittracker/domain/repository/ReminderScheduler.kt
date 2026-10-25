package com.mishr.habittracker.domain.repository

import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.result.AppResult

/**
 * Interface for scheduling and cancelling habit reminders.
 * The implementation (e.g., using AlarmManager) should reside in a platform-specific module.
 */
interface ReminderScheduler {
    
    /**
     * Schedules a daily/weekly reminder for the given habit.
     * @param habitId The ID of the habit.
     * @param hour The hour of the day (0-23).
     * @param minute The minute of the hour (0-59).
     */
    fun schedule(habitId: String, hour: Int, minute: Int): AppResult<Unit>

    /**
     * Cancels any active reminder for the given habit.
     * @param habitId The ID of the habit.
     */
    fun cancel(habitId: String): AppResult<Unit>

    /**
     * Reschedules all active reminders (useful after device boot).
     * @param habits The list of all habits (or active habits with reminders enabled) to reschedule.
     */
    fun rescheduleAll(habits: List<Habit>): AppResult<Unit>
}
