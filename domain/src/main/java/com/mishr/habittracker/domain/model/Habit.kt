package com.mishr.habittracker.domain.model

import kotlinx.datetime.Instant

/**
 * Represents a habit to be tracked.
 *
 * @param id UUID generated at creation time. Not auto-incrementing to prevent future cloud collision.
 * @param name The name of the habit.
 * @param description Optional description.
 * @param scheduleType Whether the habit is daily or weekly.
 * @param scheduledDays The days this habit is scheduled for (only meaningful for WEEKLY).
 * @param colorTag Hex color string.
 * @param isActive Boolean indicating if the habit is active (for soft deletes/archiving).
 * @param createdAt The timestamp of creation.
 * @param reminderEnabled Whether reminders are enabled.
 * @param categoryId Optional category ID.
 * @param notes Optional notes.
 * @param checklistEnabled Whether the habit uses a checklist.
 * @param checklistItems The checklist items.
 */
data class Habit(
    val id: String,
    val name: String,
    val description: String? = null,
    val scheduleType: ScheduleType,
    val scheduledDays: Set<DayOfWeek> = emptySet(),
    val colorTag: String = "#FF000000",
    val isActive: Boolean = true,
    val createdAt: Instant,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val icon: String = "star",
    val categoryId: String? = null,
    val notes: String? = null,
    val checklistEnabled: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList()
)
