package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.DayOfWeek
import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.model.ChecklistItem
import com.mishr.habittracker.domain.model.ScheduleType
import com.mishr.habittracker.domain.repository.HabitRepository
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

class CreateHabitUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val scheduleReminderUseCase: ScheduleReminderUseCase
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        scheduleType: ScheduleType,
        scheduledDays: Set<DayOfWeek>,
        colorTag: String,
        iconKey: String,
        reminderEnabled: Boolean = false,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        categoryId: String? = null,
        checklistEnabled: Boolean = false,
        checklistItems: List<String> = emptyList()
    ): AppResult<Unit> {
        if (name.isBlank()) {
            return AppResult.Error(AppError.UnknownError(Exception("Habit name cannot be blank.")))
        }

        if (scheduleType == ScheduleType.WEEKLY && scheduledDays.isEmpty()) {
            return AppResult.Error(AppError.UnknownError(Exception("Weekly habit must have at least one scheduled day.")))
        }

        if (reminderEnabled) {
            if (reminderHour == null || reminderMinute == null) {
                return AppResult.Error(AppError.UnknownError(Exception("Reminder time must be set when reminders are enabled.")))
            }
            if (reminderHour !in 0..23 || reminderMinute !in 0..59) {
                return AppResult.Error(AppError.UnknownError(Exception("Invalid reminder time.")))
            }
        }
        
        val parsedChecklist = if (checklistEnabled) {
            checklistItems.filter { it.isNotBlank() }.mapIndexed { index, text -> 
                ChecklistItem(id = UUID.randomUUID().toString(), text = text.trim(), order = index)
            }
        } else {
            emptyList()
        }

        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description.trim(),
            scheduleType = scheduleType,
            scheduledDays = if (scheduleType == ScheduleType.DAILY) emptySet() else scheduledDays,
            colorTag = colorTag,
            icon = iconKey,
            isActive = true,
            createdAt = Clock.System.now(),
            reminderEnabled = reminderEnabled,
            reminderHour = if (reminderEnabled) reminderHour else null,
            reminderMinute = if (reminderEnabled) reminderMinute else null,
            categoryId = categoryId,
            notes = null,
            checklistEnabled = checklistEnabled,
            checklistItems = parsedChecklist
        )

        val result = habitRepository.createHabit(habit)
        if (result is AppResult.Success && reminderEnabled && reminderHour != null && reminderMinute != null) {
            val scheduleResult = scheduleReminderUseCase(habit.id, reminderHour, reminderMinute)
            if (scheduleResult is AppResult.Error) {
                return scheduleResult
            }
        }
        return result
    }
}
