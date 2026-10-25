package com.mishr.habittracker.core.database

import android.content.Context
import androidx.room.Room
import com.mishr.habittracker.core.database.dao.HabitCompletionDao
import com.mishr.habittracker.core.database.dao.HabitDao
import com.mishr.habittracker.core.database.dao.CategoryDao
import com.mishr.habittracker.core.database.dao.ChecklistItemCompletionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pending_sync_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `entityId` TEXT NOT NULL,
                    `parentEntityId` TEXT,
                    `entityType` TEXT NOT NULL,
                    `operation` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideHabitTrackerDatabase(@ApplicationContext context: Context): HabitTrackerDatabase {
        return Room.databaseBuilder(
            context,
            HabitTrackerDatabase::class.java,
            "habit_tracker_db"
        )
        .addMigrations(MIGRATION_3_4)
        
            // TODO: TECHNICAL DEBT - Remove fallbackToDestructiveMigration() before 1.0 release.
            // We should provide proper Migration objects for all future schema changes
            // to ensure no silent local data loss occurs.
            .fallbackToDestructiveMigration()
        
        // DECISION: Room Encryption (SQLCipher) is currently DEFERRED.
        // Reason: Since we are not storing highly sensitive PII, passwords, or financial data,
        // standard Android file-system encryption (which protects the app's sandboxed data)
        // is deemed sufficient for Phase 8. If required later, SQLCipher can be integrated.
            .build()
    }

    @Provides
    @Singleton
    fun provideHabitDao(database: HabitTrackerDatabase): HabitDao = database.habitDao()

    @Provides
    @Singleton
    fun provideHabitCompletionDao(database: HabitTrackerDatabase): HabitCompletionDao = database.habitCompletionDao()

    @Provides
    @Singleton
    fun provideCategoryDao(database: HabitTrackerDatabase): CategoryDao = database.categoryDao()

    @Provides
    @Singleton
    fun provideChecklistItemCompletionDao(database: HabitTrackerDatabase): ChecklistItemCompletionDao = database.checklistItemCompletionDao()

    @Provides
    @Singleton
    fun providePendingSyncDao(database: HabitTrackerDatabase): com.mishr.habittracker.core.database.dao.PendingSyncDao = database.pendingSyncDao()
}
