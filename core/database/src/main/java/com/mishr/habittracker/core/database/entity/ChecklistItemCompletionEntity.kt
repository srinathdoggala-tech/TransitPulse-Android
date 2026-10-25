package com.mishr.habittracker.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "checklist_item_completions",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["checklistItemId"])]
)
data class ChecklistItemCompletionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val checklistItemId: String,
    val completedDate: String
)
