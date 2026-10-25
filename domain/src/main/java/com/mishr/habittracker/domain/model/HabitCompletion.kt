package com.mishr.habittracker.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Represents a single completion record for a habit on a specific date.
 *
 * @param id UUID of this completion record.
 * @param habitId The ID of the habit this completion belongs to.
 * @param completedDate The calendar day this completion counts for.
 * @param completedAt The actual timestamp when it was completed.
 */
data class HabitCompletion(
    val id: String,
    val habitId: String,
    val completedDate: LocalDate,
    val completedAt: Instant
)
