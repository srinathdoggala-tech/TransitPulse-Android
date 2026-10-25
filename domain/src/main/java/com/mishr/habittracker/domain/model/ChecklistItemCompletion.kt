package com.mishr.habittracker.domain.model

import kotlinx.datetime.LocalDate

data class ChecklistItemCompletion(
    val id: String,
    val habitId: String,
    val checklistItemId: String,
    val completedDate: LocalDate
)
