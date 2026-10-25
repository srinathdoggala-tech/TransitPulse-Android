package com.mishr.habittracker.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class HabitWithChecklist(
    @Embedded val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val checklistItems: List<ChecklistItemEntity>
)
