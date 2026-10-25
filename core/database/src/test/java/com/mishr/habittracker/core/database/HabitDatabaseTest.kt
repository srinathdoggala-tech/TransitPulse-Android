package com.mishr.habittracker.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mishr.habittracker.core.database.dao.HabitCompletionDao
import com.mishr.habittracker.core.database.dao.HabitDao
import com.mishr.habittracker.core.database.entity.HabitCompletionEntity
import com.mishr.habittracker.core.database.entity.HabitEntity
import com.mishr.habittracker.core.database.repository.HabitCompletionRepositoryImpl
import com.mishr.habittracker.core.common.dispatcher.DispatcherProvider
import com.mishr.habittracker.domain.model.HabitCompletion
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import com.mishr.habittracker.domain.sync.SyncTrigger
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(manifest=Config.NONE)
class HabitDatabaseTest {

    private lateinit var habitDao: HabitDao
    private lateinit var completionDao: HabitCompletionDao
    private lateinit var db: HabitTrackerDatabase
    
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, HabitTrackerDatabase::class.java
        ).allowMainThreadQueries().build()
        habitDao = db.habitDao()
        completionDao = db.habitCompletionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeHabitAndReadInList() {
        runBlocking {
            val habit = HabitEntity(
                id = "test-uuid-1",
                name = "Test Habit",
                description = null,
                scheduleType = "DAILY",
                scheduledDays = "",
                colorTag = "#000000",
                isActive = true,
                createdAt = 1000L,
                reminderEnabled = false,
                icon = "star",
                reminderHour = null,
                reminderMinute = null,
                categoryId = null,
                notes = null,
                checklistEnabled = false
            )
            habitDao.insertHabit(habit)
            val fetched = habitDao.getHabitWithChecklistById("test-uuid-1").first()
            
            assertNotNull(fetched)
            assertEquals("Test Habit", fetched?.habit?.name)
        }
    }

    @Test
    fun cascadeDeleteRemovesCompletions() {
        runBlocking {
            val habitId = "test-uuid-2"
            val habit = HabitEntity(
                id = habitId,
                name = "Test Habit 2",
                description = null,
                scheduleType = "DAILY",
                scheduledDays = "",
                colorTag = "#000000",
                isActive = true,
                createdAt = 1000L,
                reminderEnabled = false,
                icon = "star",
                reminderHour = null,
                reminderMinute = null,
                categoryId = null,
                notes = null,
                checklistEnabled = false
            )
            habitDao.insertHabit(habit)
            
            val completion = HabitCompletionEntity(
                id = "comp-uuid-1",
                habitId = habitId,
                completedDate = "2026-08-11",
                completedAt = 2000L
            )
            completionDao.insertCompletion(completion)

            val completionsBefore = completionDao.getCompletionsForHabit(habitId).first()
            assertEquals(1, completionsBefore.size)

            // Delete habit to trigger cascade
            habitDao.deleteHabitById(habitId)

            val completionsAfter = completionDao.getCompletionsForHabit(habitId).first()
            assertTrue(completionsAfter.isEmpty())
        }
    }

    @Test
    fun preventDuplicateCompletions() {
        runBlocking {
            val habitId = "test-uuid-3"
            val habit = HabitEntity(
                id = habitId,
                name = "Test Habit 3",
                description = null,
                scheduleType = "DAILY",
                scheduledDays = "",
                colorTag = "#000000",
                isActive = true,
                createdAt = 1000L,
                reminderEnabled = false,
                icon = "star",
                reminderHour = null,
                reminderMinute = null,
                categoryId = null,
                notes = null,
                checklistEnabled = false
            )
            habitDao.insertHabit(habit)
            
            val repository = HabitCompletionRepositoryImpl(db, testDispatcherProvider, object : com.mishr.habittracker.domain.sync.SyncTrigger { override fun scheduleSync() {}
            override fun schedulePeriodicSync() {} })
            
            val completion = HabitCompletion(
                id = "comp-uuid-2",
                habitId = habitId,
                completedDate = LocalDate(2026, 8, 11),
                completedAt = Instant.fromEpochMilliseconds(2000L)
            )
            
            // 1. First call should succeed
            val firstResult = repository.markComplete(completion)
            assertTrue(firstResult is AppResult.Success)

            // 2. Second call should fail with DatabaseError indicating it's already completed
            val secondResult = repository.markComplete(completion)
            assertTrue(secondResult is AppResult.Error)
            
            // 3. Query the DAO afterward to confirm only ONE completion row exists
            val existing = completionDao.getCompletionByHabitAndDate(habitId, "2026-08-11")
            assertNotNull(existing)
            
            val allCompletions = completionDao.getCompletionsForHabit(habitId).first()
            assertEquals(1, allCompletions.size)
        }
    }

    @Test
    fun testReminderTimePersistence() {
        runBlocking {
            val repository = com.mishr.habittracker.core.database.repository.HabitRepositoryImpl(db, testDispatcherProvider, object : com.mishr.habittracker.domain.sync.SyncTrigger { override fun scheduleSync() {}
            override fun schedulePeriodicSync() {} })
            
            val habit = com.mishr.habittracker.domain.model.Habit(
                id = "test-uuid-reminder",
                name = "Reminder Habit",
                description = "Testing reminder persistence",
                scheduleType = com.mishr.habittracker.domain.model.ScheduleType.DAILY,
                scheduledDays = emptySet(),
                colorTag = "#FFFFFF",
                isActive = true,
                createdAt = kotlinx.datetime.Clock.System.now(),
                reminderEnabled = true,
                reminderHour = 14,
                reminderMinute = 30,
                icon = "star",
                categoryId = null,
                notes = null,
                checklistEnabled = false,
                checklistItems = emptyList()
            )
            
            repository.createHabit(habit)
            
            val fetchedHabit = repository.getAllHabits().first().find { it.id == "test-uuid-reminder" }
            assertNotNull(fetchedHabit)
            assertEquals(14, fetchedHabit?.reminderHour)
            assertEquals(30, fetchedHabit?.reminderMinute)
            assertEquals(true, fetchedHabit?.reminderEnabled)
        }
    }
}
