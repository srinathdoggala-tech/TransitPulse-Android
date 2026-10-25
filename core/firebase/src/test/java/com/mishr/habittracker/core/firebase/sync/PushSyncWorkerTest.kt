package com.mishr.habittracker.core.firebase.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.android.gms.tasks.Tasks
import com.mishr.habittracker.core.database.HabitTrackerDatabase
import com.mishr.habittracker.core.database.dao.HabitDao
import com.mishr.habittracker.core.database.dao.PendingSyncDao
import com.mishr.habittracker.core.database.entity.HabitEntity
import com.mishr.habittracker.core.database.entity.PendingSyncEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PushSyncWorkerTest {

    private lateinit var database: HabitTrackerDatabase
    private lateinit var pendingSyncDao: PendingSyncDao
    private lateinit var habitDao: HabitDao
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    
    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var worker: PushSyncWorker

    @Before
    fun setup() {
        database = mockk(relaxed = true)
        pendingSyncDao = mockk(relaxed = true)
        habitDao = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        firebaseUser = mockk(relaxed = true)
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)

        coEvery { database.pendingSyncDao() } returns pendingSyncDao
        coEvery { database.habitDao() } returns habitDao
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "test-user-123"

        worker = PushSyncWorker(context, workerParams, database, firestore, firebaseAuth)
    }

    @Test
    fun `doWork returns success immediately if not logged in`() = runTest {
        every { firebaseAuth.currentUser } returns null
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { pendingSyncDao.getAllPendingSyncEvents() }
    }

    @Test
    fun `doWork returns success immediately if no pending events`() = runTest {
        coEvery { pendingSyncDao.getAllPendingSyncEvents() } returns emptyList()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { firestore.batch() }
    }

    @Test
    fun `doWork batches events and deletes them upon success`() = runTest {
        val pendingEvents = listOf(
            PendingSyncEntity(id = 1L, entityId = "habit1", entityType = "HABIT", operation = "UPSERT"),
            PendingSyncEntity(id = 2L, entityId = "habit2", entityType = "HABIT", operation = "DELETE")
        )
        coEvery { pendingSyncDao.getAllPendingSyncEvents() } returns pendingEvents

        val habit1 = HabitEntity(
            id = "habit1", name = "Test", scheduleType = "DAILY", scheduledDays = "", 
            colorTag = "#000", isActive = true, createdAt = 0L, reminderEnabled = false, 
            reminderHour = null, reminderMinute = null, icon = "icon", categoryId = null, 
            notes = null, checklistEnabled = false, description = null
        )
        coEvery { habitDao.getHabitSyncById("habit1") } returns habit1
        coEvery { habitDao.getChecklistItemsForHabitSync("habit1") } returns emptyList()

        val userRef = mockk<DocumentReference>(relaxed = true)
        val habitsCollection = mockk<CollectionReference>(relaxed = true)
        val doc1 = mockk<DocumentReference>(relaxed = true)
        val doc2 = mockk<DocumentReference>(relaxed = true)
        val writeBatch = mockk<WriteBatch>(relaxed = true)

        every { firestore.collection("users").document("test-user-123") } returns userRef
        every { userRef.collection("habits") } returns habitsCollection
        every { habitsCollection.document("habit1") } returns doc1
        every { habitsCollection.document("habit2") } returns doc2
        every { firestore.batch() } returns writeBatch
        
        // Mock Tasks.await() equivalent via reflection/extension is hard in tests, 
        // but our PushSyncWorker uses kotlinx.coroutines.tasks.await(). 
        // We can just mock batch.commit() to return a completed task.
        every { writeBatch.commit() } returns Tasks.forResult(null)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        
        // Verify batch calls
        coVerify(exactly = 1) { writeBatch.set(doc1, any(), any()) }
        coVerify(exactly = 1) { writeBatch.delete(doc2) }
        coVerify(exactly = 1) { writeBatch.commit() }

        // Verify the processed events were deleted
        val deletedIdsSlot = slot<List<Long>>()
        coVerify(exactly = 1) { pendingSyncDao.deleteSyncEvents(capture(deletedIdsSlot)) }
        assertEquals(listOf(1L, 2L), deletedIdsSlot.captured)
    }
}
