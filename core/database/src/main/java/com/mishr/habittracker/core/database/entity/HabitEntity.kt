package com.mishr.habittracker.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity representing a Habit.
 */
@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val scheduleType: String,
    val scheduledDays: String,
    val colorTag: String,
    val isActive: Boolean,
    val createdAt: Long,
    val reminderEnabled: Boolean,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val icon: String,
    val categoryId: String?,
    val notes: String?,
    val checklistEnabled: Boolean
)
