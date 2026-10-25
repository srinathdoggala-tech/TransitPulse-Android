package com.mishr.habittracker.domain.model

data class HabitWithStatus(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int,
    val completedDates: Set<kotlinx.datetime.LocalDate>,
    val todayChecklistCompletions: Set<String> = emptySet()
)
