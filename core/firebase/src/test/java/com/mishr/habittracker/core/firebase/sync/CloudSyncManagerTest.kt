package com.mishr.habittracker.core.firebase.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.mishr.habittracker.core.common.dispatcher.DispatcherProvider
import com.mishr.habittracker.core.database.HabitTrackerDatabase
import com.mishr.habittracker.core.database.dao.CategoryDao
import com.mishr.habittracker.core.database.dao.HabitCompletionDao
import com.mishr.habittracker.core.database.dao.HabitDao
import com.mishr.habittracker.core.database.dao.ChecklistItemCompletionDao
import com.mishr.habittracker.core.database.dao.PendingSyncDao
import com.mishr.habittracker.core.database.entity.PendingSyncEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloudSyncManagerTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: HabitTrackerDatabase
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var categoryDao: CategoryDao
    private lateinit var habitDao: HabitDao
    private lateinit var completionDao: HabitCompletionDao
    private lateinit var checklistCompletionDao: ChecklistItemCompletionDao
    private lateinit var pendingSyncDao: PendingSyncDao

    private lateinit var cloudSyncManager: CloudSyncManager

    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        database = mockk(relaxed = true)
        dispatcherProvider = mockk(relaxed = true)
        categoryDao = mockk(relaxed = true)
        habitDao = mockk(relaxed = true)
        completionDao = mockk(relaxed = true)
        checklistCompletionDao = mockk(relaxed = true)
        pendingSyncDao = mockk(relaxed = true)

        cloudSyncManager = CloudSyncManager(
            firestore,
            database,
            dispatcherProvider,
            categoryDao,
            habitDao,
            completionDao,
            checklistCompletionDao,
            pendingSyncDao
        )
    }

    @Test
    fun `shouldOverwrite - no pending sync events - returns true`() = runTest {
        coEvery { pendingSyncDao.getSyncEventsForEntity("habit123") } returns emptyList()

        val result = cloudSyncManager.shouldOverwrite("habit123", 1000L)
        assertTrue(result)
    }

    @Test
    fun `shouldOverwrite - incoming is newer than pending - returns true`() = runTest {
        val pendingEvent = PendingSyncEntity(
            entityId = "habit123",
            entityType = "HABIT",
            operation = "UPSERT",
            timestamp = 1000L
        )
        coEvery { pendingSyncDao.getSyncEventsForEntity("habit123") } returns listOf(pendingEvent)

        val result = cloudSyncManager.shouldOverwrite("habit123", 2000L) // 2000 > 1000
        assertTrue(result)
    }

    @Test
    fun `shouldOverwrite - incoming is older than pending - returns false`() = runTest {
        val pendingEvent = PendingSyncEntity(
            entityId = "habit123",
            entityType = "HABIT",
            operation = "UPSERT",
            timestamp = 2000L
        )
        coEvery { pendingSyncDao.getSyncEventsForEntity("habit123") } returns listOf(pendingEvent)

        val result = cloudSyncManager.shouldOverwrite("habit123", 1000L) // 1000 < 2000
        assertFalse(result)
    }
}
