package com.mishr.habittracker.core.database.repository

import com.mishr.habittracker.core.common.dispatcher.DispatcherProvider
import com.mishr.habittracker.core.database.HabitTrackerDatabase
import com.mishr.habittracker.core.database.dao.HabitDao
import com.mishr.habittracker.core.database.dao.PendingSyncDao
import com.mishr.habittracker.core.database.entity.PendingSyncEntity
import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.model.ScheduleType
import com.mishr.habittracker.domain.sync.SyncTrigger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitRepositorySyncTest {

    private lateinit var database: HabitTrackerDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var pendingSyncDao: PendingSyncDao
    private lateinit var syncTrigger: SyncTrigger
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var repository: HabitRepositoryImpl

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        database = mockk(relaxed = true)
        habitDao = mockk(relaxed = true)
        pendingSyncDao = mockk(relaxed = true)
        syncTrigger = mockk(relaxed = true)

        coEvery { database.habitDao() } returns habitDao
        coEvery { database.pendingSyncDao() } returns pendingSyncDao
        
        // Mock runInTransaction to just execute the block immediately
        coEvery { database.runInTransaction(any<suspend () -> Unit>()) } coAnswers {
            val block = arg<suspend () -> Unit>(0)
            block.invoke()
        }

        dispatcherProvider = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
        }

        repository = HabitRepositoryImpl(database, dispatcherProvider, syncTrigger)
    }

    @Test
    fun `createHabit queues UPSERT pending sync event and triggers sync`() = runTest {
        val habit = Habit(
            id = "habit-123",
            name = "Test Habit",
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#000000",
            createdAt = kotlinx.datetime.Clock.System.now(),
            icon = "icon"
        )

        repository.createHabit(habit)

        val syncEventSlot = slot<PendingSyncEntity>()
        coVerify(exactly = 1) { pendingSyncDao.insertSyncEvent(capture(syncEventSlot)) }
        coVerify(exactly = 1) { syncTrigger.scheduleSync() }

        val capturedEvent = syncEventSlot.captured
        assertEquals("habit-123", capturedEvent.entityId)
        assertEquals("HABIT", capturedEvent.entityType)
        assertEquals("UPSERT", capturedEvent.operation)
    }

    @Test
    fun `deleteHabit queues DELETE pending sync event and triggers sync`() = runTest {
        repository.deleteHabit("habit-456")

        val syncEventSlot = slot<PendingSyncEntity>()
        coVerify(exactly = 1) { pendingSyncDao.insertSyncEvent(capture(syncEventSlot)) }
        coVerify(exactly = 1) { syncTrigger.scheduleSync() }

        val capturedEvent = syncEventSlot.captured
        assertEquals("habit-456", capturedEvent.entityId)
        assertEquals("HABIT", capturedEvent.entityType)
        assertEquals("DELETE", capturedEvent.operation)
    }
}
