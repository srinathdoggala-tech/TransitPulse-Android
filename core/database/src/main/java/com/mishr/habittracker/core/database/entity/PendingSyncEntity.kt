package com.mishr.habittracker.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a pending sync operation in the Outbox queue.
 */
@Entity(tableName = "pending_sync_events")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityId: String,       // ID of the habit, completion, or category
    val parentEntityId: String? = null, // Habit ID for nested completions
    val entityType: String,     // "HABIT", "CATEGORY", "HABIT_COMPLETION", "CHECKLIST_COMPLETION"
    val operation: String,      // "UPSERT" or "DELETE"
    val timestamp: Long = System.currentTimeMillis() // To process in order
)
