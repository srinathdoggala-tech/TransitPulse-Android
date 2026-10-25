package com.mishr.habittracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a completion record for a Habit.
 *
 * Foreign Key Cascade: When a habit is deleted, its completion history is inherently
 * tied to it and should be deleted as well to avoid orphaned records that waste
 * storage and have no meaning.
 */
@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId")]
)
data class HabitCompletionEntity(
    @PrimaryKey
    val id: String,
    val habitId: String,
    val completedDate: String,
    val completedAt: Long
)
