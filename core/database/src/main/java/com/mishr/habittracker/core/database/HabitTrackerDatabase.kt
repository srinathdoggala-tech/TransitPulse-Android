package com.mishr.habittracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import com.mishr.habittracker.core.database.converter.Converters
import com.mishr.habittracker.core.database.dao.HabitCompletionDao
import com.mishr.habittracker.core.database.dao.HabitDao
import com.mishr.habittracker.core.database.dao.CategoryDao
import com.mishr.habittracker.core.database.dao.ChecklistItemCompletionDao
import com.mishr.habittracker.core.database.entity.HabitCompletionEntity
import com.mishr.habittracker.core.database.entity.HabitEntity
import com.mishr.habittracker.core.database.entity.CategoryEntity
import com.mishr.habittracker.core.database.entity.ChecklistItemEntity
import com.mishr.habittracker.core.database.entity.ChecklistItemCompletionEntity
import com.mishr.habittracker.core.database.entity.PendingSyncEntity
import com.mishr.habittracker.core.database.dao.PendingSyncDao

/**
 * Room database for Habit Tracker app.
 */
@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        CategoryEntity::class,
        ChecklistItemEntity::class,
        ChecklistItemCompletionEntity::class,
        PendingSyncEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HabitTrackerDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun checklistItemCompletionDao(): ChecklistItemCompletionDao
    abstract fun pendingSyncDao(): PendingSyncDao

    /**
     * Non-inline wrapper around the Room KTX withTransaction extension.
     *
     * Room's withTransaction is an inline suspend function which cannot be intercepted
     * by MockK in unit tests. By delegating to this concrete method, test doubles can
     * stub runInTransaction() as a simple passthrough (executing the block directly)
     * without needing to mock the inline extension function.
     *
     * Production behaviour is identical — this just calls withTransaction internally.
     */
    open suspend fun runInTransaction(block: suspend () -> Unit) {
        withTransaction(block)
    }
}
