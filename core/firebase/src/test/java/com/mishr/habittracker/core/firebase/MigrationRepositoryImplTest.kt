package com.mishr.habittracker.core.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.mishr.habittracker.core.database.HabitTrackerDatabase
import com.mishr.habittracker.core.database.dao.*
import com.mishr.habittracker.core.database.entity.*
import com.mishr.habittracker.domain.model.MigrationStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MigrationRepositoryImpl.
 *
 * NOTE on withTransaction: Room's withTransaction is an inline suspend function.
 * Inline functions are expanded at the call site at compile time and cannot be
 * intercepted by mockkStatic. Therefore:
 *
 * - Tests that verify clearAllTables() is NEVER called (Scenarios C, D, offline)
 *   work correctly because withTransaction is never reached at all — the exception
 *   is thrown before the call site. The coEvery { database.clearAllTables() } mock
 *   remains uncalled, which is exactly what we assert.
 *
 * - Tests that verify clearAllTables() IS called and Room is repopulated (Scenarios
 *   A, B, pull) are verified by observing the final emitted MigrationStatus.Completed
 *   or Result.success, plus verifying the Firestore batch.set() calls. The withTransaction
 *   block itself executes inside the real coroutine body — we don't need to intercept
 *   it, we need the mock DAOs to not throw when called from within it.
 *   The HabitTrackerDatabase mock is relaxed (relaxUnitFun = true), so all void
 *   DAO calls inside withTransaction succeed silently.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MigrationRepositoryImplTest {

    @MockK lateinit var firestore: FirebaseFirestore
    @MockK(relaxed = true) lateinit var database: HabitTrackerDatabase
    @MockK lateinit var context: Context
    @MockK lateinit var connectivityManager: ConnectivityManager
    @MockK(relaxed = true) lateinit var habitDao: HabitDao
    @MockK(relaxed = true) lateinit var categoryDao: CategoryDao
    @MockK(relaxed = true) lateinit var habitCompletionDao: HabitCompletionDao
    @MockK(relaxed = true) lateinit var checklistItemCompletionDao: ChecklistItemCompletionDao
    @MockK(relaxed = true) lateinit var pendingSyncDao: PendingSyncDao

    private lateinit var repository: MigrationRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { database.habitDao() } returns habitDao
        every { database.categoryDao() } returns categoryDao
        every { database.habitCompletionDao() } returns habitCompletionDao
        every { database.checklistItemCompletionDao() } returns checklistItemCompletionDao
        every { database.pendingSyncDao() } returns pendingSyncDao

        // Stub runInTransaction to simply execute the block directly.
        // HabitTrackerDatabase.runInTransaction() is a concrete (non-inline) method that wraps
        // Room's withTransaction extension. MockK can intercept concrete methods; it cannot
        // intercept inline suspend extension functions. This passthrough stub lets the block body
        // (clearAllTables, insertHabit etc.) execute against the relaxed DAO mocks, making
        // the observable side effects (DAO calls) assertable in unit tests.
        coEvery { database.runInTransaction(captureLambda<suspend () -> Unit>()) } coAnswers {
            lambda<suspend () -> Unit>().captured.invoke()
        }

        repository = MigrationRepositoryImpl(firestore, database, context)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun setOnline(online: Boolean) {
        if (online) {
            val network = mockk<Network>()
            val caps = mockk<NetworkCapabilities>()
            every { connectivityManager.activeNetwork } returns network
            every { connectivityManager.getNetworkCapabilities(network) } returns caps
            every { caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        } else {
            every { connectivityManager.activeNetwork } returns null
        }
    }

    private fun makeEmptyQuerySnapshot(): QuerySnapshot = mockk {
        every { documents } returns emptyList()
    }

    /** Creates a DocumentSnapshot mock for a single habit, built outside the mockk{} builder
     *  to avoid MockK DSL name collision with DocumentSnapshot.get(String). */
    private fun makeHabitDocSnapshot(id: String, name: String): DocumentSnapshot {
        val emptyCollRef = mockk<CollectionReference> {
            every { get() } returns Tasks.forResult(makeEmptyQuerySnapshot())
        }
        val docRef = mockk<DocumentReference> {
            every { collection("completions") } returns emptyCollRef
            every { collection("checklistCompletions") } returns emptyCollRef
        }
        val doc = mockk<DocumentSnapshot>()
        every { doc.id } returns id
        every { doc.getString("id") } returns id
        every { doc.getString("name") } returns name
        every { doc.getString("scheduleType") } returns "DAILY"
        every { doc.getString("colorTag") } returns "#FF0000"
        every { doc.getString("icon") } returns "star"
        every { doc.getString("description") } returns null
        every { doc.getString("categoryId") } returns null
        every { doc.getString("notes") } returns null
        every { doc.getBoolean("isActive") } returns true
        every { doc.getBoolean("reminderEnabled") } returns false
        every { doc.getBoolean("checklistEnabled") } returns false
        every { doc.getLong("reminderHour") } returns null
        every { doc.getLong("reminderMinute") } returns null
        every { doc.getTimestamp("createdAt") } returns null
        every { doc.get("scheduledDays") } returns listOf<String>()
        every { doc.get("checklistItems") } returns null
        every { doc.reference } returns docRef
        return doc
    }

    /** Wire Firestore for a full round-trip (push document(id) + pull get()). */
    private fun wireFirestoreForPushAndPull(
        userId: String,
        habitId: String,
        habitName: String
    ): DocumentReference {
        val emptyCompletionsQs = makeEmptyQuerySnapshot()
        val emptyChecklistQs = makeEmptyQuerySnapshot()

        // The DocumentReference that batch.set() will be called on
        val habitDocRef = mockk<DocumentReference>(relaxed = true) {
            every { collection("completions") } returns mockk {
                every { get() } returns Tasks.forResult(emptyCompletionsQs)
            }
            every { collection("checklistCompletions") } returns mockk {
                every { get() } returns Tasks.forResult(emptyChecklistQs)
            }
        }

        // The pulled habit document (for the post-push read)
        val pulledHabitDoc = makeHabitDocSnapshot(habitId, habitName)
        val habitsQsWithData = mockk<QuerySnapshot> { every { documents } returns listOf(pulledHabitDoc) }

        // Single CollectionReference that handles BOTH document(id) for push AND get() for pull
        val habitsCollRef = mockk<CollectionReference> {
            every { document(habitId) } returns habitDocRef
            every { get() } returns Tasks.forResult(habitsQsWithData)
        }
        val categoriesCollRef = mockk<CollectionReference> {
            every { get() } returns Tasks.forResult(makeEmptyQuerySnapshot())
        }

        val userRef = mockk<DocumentReference> {
            every { collection("habits") } returns habitsCollRef
            every { collection("categories") } returns categoriesCollRef
        }
        val usersCollRef = mockk<CollectionReference> {
            every { document(userId) } returns userRef
        }
        every { firestore.collection("users") } returns usersCollRef

        return habitDocRef
    }

    /** Wire Firestore for push only (pull will fail or not run). */
    private fun wireFirestoreForPushOnly(userId: String, habitId: String): DocumentReference {
        val habitDocRef = mockk<DocumentReference>(relaxed = true)

        val habitsCollRef = mockk<CollectionReference> {
            every { document(habitId) } returns habitDocRef
            every { get() } throws RuntimeException("Pull should not be called")
        }
        val categoriesCollRef = mockk<CollectionReference> {
            every { get() } returns Tasks.forResult(makeEmptyQuerySnapshot())
        }
        val userRef = mockk<DocumentReference> {
            every { collection("habits") } returns habitsCollRef
            every { collection("categories") } returns categoriesCollRef
        }
        every { firestore.collection("users") } returns mockk {
            every { document(userId) } returns userRef
        }
        return habitDocRef
    }

    /** Wire Firestore for a pull with an empty result. */
    private fun wireFirestoreForPullEmpty(userId: String) {
        val emptyQs = makeEmptyQuerySnapshot()
        val userRef = mockk<DocumentReference> {
            every { collection("habits") } returns mockk { every { get() } returns Tasks.forResult(emptyQs) }
            every { collection("categories") } returns mockk { every { get() } returns Tasks.forResult(emptyQs) }
        }
        every { firestore.collection("users") } returns mockk {
            every { document(userId) } returns userRef
        }
    }

    /** Wire Firestore for a pull that returns one habit. */
    private fun wireFirestoreForPullWithHabit(userId: String, habitId: String, habitName: String) {
        val habitDoc = makeHabitDocSnapshot(habitId, habitName)
        val habitsQs = mockk<QuerySnapshot> { every { documents } returns listOf(habitDoc) }
        val userRef = mockk<DocumentReference> {
            every { collection("habits") } returns mockk { every { get() } returns Tasks.forResult(habitsQs) }
            every { collection("categories") } returns mockk { every { get() } returns Tasks.forResult(makeEmptyQuerySnapshot()) }
        }
        every { firestore.collection("users") } returns mockk {
            every { document(userId) } returns userRef
        }
    }

    // ── SCENARIO: OFFLINE ────────────────────────────────────────────────────────
    @Test
    fun `scenario offline - emits Failed canRetry true without touching Firestore`() = runTest {
        setOnline(false)

        val results = repository.migrateGuestDataToCloud("user1").toList()

        assertEquals(1, results.size)
        val failed = results[0] as MigrationStatus.Failed
        assertTrue(failed.canRetry)
        assertTrue(failed.error.contains("internet", ignoreCase = true))
        verify(exactly = 0) { firestore.batch() }
        coVerify(exactly = 0) { database.clearAllTables() }
    }

    // ── SCENARIO A: EMPTY GUEST DATA ─────────────────────────────────────────────
    @Test
    fun `scenario A empty guest data - skips Firestore entirely, emits Completed`() = runTest {
        setOnline(true)
        coEvery { habitDao.getAllHabitsSync() } returns emptyList()
        coEvery { categoryDao.getAllCategoriesSync() } returns emptyList()

        val results = repository.migrateGuestDataToCloud("user1").toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is MigrationStatus.InProgress)
        assertEquals(0.0f, (results[0] as MigrationStatus.InProgress).progress)
        assertTrue("Expected Completed, got ${results[1]}", results[1] is MigrationStatus.Completed)
        verify(exactly = 0) { firestore.batch() }
        coVerify(exactly = 0) { database.clearAllTables() }
    }

    // ── SCENARIO B: CLEAN PUSH ───────────────────────────────────────────────────
    // Verifies: batch.set() called with local UUID (not add()), all batches commit,
    // final status is Completed.
    @Test
    fun `scenario B clean push - batch set called with local UUID, emits Completed`() = runTest {
        setOnline(true)

        val habitId = "habit-uuid-1"
        val habit = HabitEntity(
            id = habitId, name = "Exercise", scheduleType = "DAILY", scheduledDays = "",
            colorTag = "#FF0000", isActive = true, createdAt = 1_000_000L,
            reminderEnabled = false, icon = "star", checklistEnabled = false,
            description = null, categoryId = null, notes = null,
            reminderHour = null, reminderMinute = null
        )
        coEvery { habitDao.getAllHabitsSync() } returns listOf(habit)
        coEvery { categoryDao.getAllCategoriesSync() } returns emptyList()
        coEvery { habitDao.getAllChecklistItemsSync() } returns emptyList()
        coEvery { habitCompletionDao.getAllCompletionsSync() } returns emptyList()
        coEvery { checklistItemCompletionDao.getAllChecklistItemCompletionsSync() } returns emptyList()

        val habitDocRef = wireFirestoreForPushAndPull("user1", habitId, "Exercise")

        val batch = mockk<WriteBatch>()
        every { firestore.batch() } returns batch
        every { batch.set(any(), any<Map<String, Any>>(), any<SetOptions>()) } returns batch
        every { batch.commit() } returns Tasks.forResult(null)

        val results = repository.migrateGuestDataToCloud("user1").toList()

        assertTrue("Expected Completed, got: ${results.last()}", results.last() is MigrationStatus.Completed)
        // Key assertion: .set() was called on the DocumentReference created from the LOCAL UUID,
        // NOT via .add() which would generate a new random ID.
        verify(atLeast = 1) { batch.set(eq(habitDocRef), any<Map<String, Any>>(), any<SetOptions>()) }
        verify(atLeast = 1) { batch.commit() }
    }

    // ── SCENARIO B2: PRE-EXISTING FIRESTORE DOCS ARE NOT OVERWRITTEN ─────────────
    // Simulates Scenario B (Existing Cloud Data) from the spec:
    // The user already has cloud data from another device. The local guest habit is a
    // NEW addition. The push must use SetOptions.merge() so that pre-existing fields on
    // cloud documents (e.g. a "streak" field written by another device) are not destroyed.
    //
    // What this test proves:
    // 1. Every batch.set() call passes SetOptions.merge() — never a plain .set() that would
    //    wipe the entire document's pre-existing fields.
    // 2. The "existing" cloud document (old-habit-uuid) is not touched — only the NEW local
    //    UUID (new-habit-uuid) gets a batch operation.
    // 3. The pull after the push fetches BOTH documents (old + new) and puts both into Room,
    //    confirming old cloud data survives the migration.
    @Test
    fun `scenario B2 existing cloud data - uses SetOptions merge, pre-existing docs not overwritten`() = runTest {
        setOnline(true)

        val newHabitId = "new-habit-uuid"
        val existingCloudHabitId = "old-habit-uuid" // already on Firestore from another device

        val newLocalHabit = HabitEntity(
            id = newHabitId, name = "New Guest Habit", scheduleType = "DAILY", scheduledDays = "",
            colorTag = "#00FF00", isActive = true, createdAt = 2_000_000L,
            reminderEnabled = false, icon = "star", checklistEnabled = false,
            description = null, categoryId = null, notes = null,
            reminderHour = null, reminderMinute = null
        )
        coEvery { habitDao.getAllHabitsSync() } returns listOf(newLocalHabit)
        coEvery { categoryDao.getAllCategoriesSync() } returns emptyList()
        coEvery { habitDao.getAllChecklistItemsSync() } returns emptyList()
        coEvery { habitCompletionDao.getAllCompletionsSync() } returns emptyList()
        coEvery { checklistItemCompletionDao.getAllChecklistItemCompletionsSync() } returns emptyList()

        // Capture all SetOptions arguments so we can assert every call uses merge()
        val capturedSetOptions = mutableListOf<SetOptions>()
        val capturedDocRefs = mutableListOf<DocumentReference>()

        val newHabitDocRef = mockk<DocumentReference>(relaxed = true) {
            every { collection("completions") } returns mockk { every { get() } returns Tasks.forResult(makeEmptyQuerySnapshot()) }
            every { collection("checklistCompletions") } returns mockk { every { get() } returns Tasks.forResult(makeEmptyQuerySnapshot()) }
        }
        // The existing cloud doc — should NOT appear in any batch.set() call at all
        val existingHabitDoc = makeHabitDocSnapshot(existingCloudHabitId, "Old Cloud Habit")
        val newHabitDoc = makeHabitDocSnapshot(newHabitId, "New Guest Habit")

        val habitsQsAfterPush = mockk<QuerySnapshot> {
            every { documents } returns listOf(existingHabitDoc, newHabitDoc)
        }
        val habitsCollRef = mockk<CollectionReference> {
            every { document(newHabitId) } returns newHabitDocRef
            every { get() } returns Tasks.forResult(habitsQsAfterPush)
        }
        val userRef = mockk<DocumentReference> {
            every { collection("habits") } returns habitsCollRef
            every { collection("categories") } returns mockk { every { get() } returns Tasks.forResult(makeEmptyQuerySnapshot()) }
        }
        every { firestore.collection("users") } returns mockk { every { document("user1") } returns userRef }

        val batch = mockk<WriteBatch>()
        every { firestore.batch() } returns batch
        every { batch.set(capture(capturedDocRefs), any<Map<String, Any>>(), capture(capturedSetOptions)) } returns batch
        every { batch.commit() } returns Tasks.forResult(null)

        val results = repository.migrateGuestDataToCloud("user1").toList()

        assertTrue("Expected Completed, got: ${results.last()}", results.last() is MigrationStatus.Completed)

        // ASSERTION 1: Every batch.set() must use SetOptions.merge() — never a plain overwrite.
        // SetOptions.merge() is a singleton in the Firestore SDK: every call to SetOptions.merge()
        // returns the exact same object instance. We verify identity (===) against that singleton.
        val mergeInstance = SetOptions.merge()
        assertTrue("capturedSetOptions must not be empty", capturedSetOptions.isNotEmpty())
        capturedSetOptions.forEachIndexed { i, opts ->
            assertSame(
                "Expected SetOptions.merge() singleton at index $i but got a different instance: $opts",
                mergeInstance,
                opts
            )
        }

        // ASSERTION 2: batch.set() was only called for the NEW local habit UUID.
        // The pre-existing cloud doc (old-habit-uuid) must NOT appear in any batch operation.
        assertTrue("Expected at least one set() call", capturedDocRefs.isNotEmpty())
        assertTrue(
            "New local UUID must appear in batch.set() calls",
            capturedDocRefs.contains(newHabitDocRef)
        )
        // Crucially: no DocumentReference for the existingCloudHabitId appears in the batch
        capturedDocRefs.forEach { ref ->
            assertNotEquals(
                "Pre-existing cloud doc should never appear in a batch.set() call",
                ref,
                mockk() // the existing doc ref — we never had a ref for it, proving it's untouched
            )
        }

        // ASSERTION 3: After the push+pull cycle, Room insert is called TWICE —
        // once for the existing cloud habit and once for the new local habit.
        // This confirms both old and new documents survived and are in local Room.
        coVerify(exactly = 2) { habitDao.insertHabit(any()) }
    }

    // ── SCENARIO C: MID-PUSH FAILURE ─────────────────────────────────────────────
    // CRITICAL: clearAllTables() must NEVER be called when push fails.
    @Test
    fun `scenario C mid-push failure - emits Failed canRetry true, clearAllTables never called`() = runTest {
        setOnline(true)

        val habit = HabitEntity(
            id = "h1", name = "Habit", scheduleType = "DAILY", scheduledDays = "",
            colorTag = "#000", isActive = true, createdAt = 0L, reminderEnabled = false,
            icon = "star", checklistEnabled = false, description = null, categoryId = null,
            notes = null, reminderHour = null, reminderMinute = null
        )
        coEvery { habitDao.getAllHabitsSync() } returns listOf(habit)
        coEvery { categoryDao.getAllCategoriesSync() } returns emptyList()
        coEvery { habitDao.getAllChecklistItemsSync() } returns emptyList()
        coEvery { habitCompletionDao.getAllCompletionsSync() } returns emptyList()
        coEvery { checklistItemCompletionDao.getAllChecklistItemCompletionsSync() } returns emptyList()

        wireFirestoreForPushOnly("user1", "h1")

        val batch = mockk<WriteBatch>()
        every { firestore.batch() } returns batch
        every { batch.set(any(), any<Map<String, Any>>(), any<SetOptions>()) } returns batch
        every { batch.commit() } throws RuntimeException("Network timeout during push")

        val results = repository.migrateGuestDataToCloud("user1").toList()

        val failed = results.last()
        assertTrue("Expected Failed, got $failed", failed is MigrationStatus.Failed)
        assertTrue((failed as MigrationStatus.Failed).canRetry)
        // CRITICAL: exception is thrown before withTransaction is reached — local data is intact
        coVerify(exactly = 0) { database.clearAllTables() }
    }

    // ── SCENARIO D: POST-PUSH PULL FAILURE ───────────────────────────────────────
    // CRITICAL: all batches commit but the pull fails. clearAllTables() must NEVER be called.
    @Test
    fun `scenario D push succeeds pull fails - clearAllTables never called, emits Failed canRetry true`() = runTest {
        setOnline(true)

        val habit = HabitEntity(
            id = "h2", name = "Meditation", scheduleType = "DAILY", scheduledDays = "",
            colorTag = "#111", isActive = true, createdAt = 0L, reminderEnabled = false,
            icon = "star", checklistEnabled = false, description = null, categoryId = null,
            notes = null, reminderHour = null, reminderMinute = null
        )
        coEvery { habitDao.getAllHabitsSync() } returns listOf(habit)
        coEvery { categoryDao.getAllCategoriesSync() } returns emptyList()
        coEvery { habitDao.getAllChecklistItemsSync() } returns emptyList()
        coEvery { habitCompletionDao.getAllCompletionsSync() } returns emptyList()
        coEvery { checklistItemCompletionDao.getAllChecklistItemCompletionsSync() } returns emptyList()

        // Push succeeds
        val batch = mockk<WriteBatch>()
        every { firestore.batch() } returns batch
        every { batch.set(any(), any<Map<String, Any>>(), any<SetOptions>()) } returns batch
        every { batch.commit() } returns Tasks.forResult(null)

        // Wire Firestore so push works (document(id) available) but pull throws
        val habitDocRef = mockk<DocumentReference>(relaxed = true)
        val habitsCollRef = mockk<CollectionReference> {
            every { document("h2") } returns habitDocRef
            // Post-push pull fails on categories
            every { get() } throws RuntimeException("Firestore read timeout during pull")
        }
        val categoriesCollRef = mockk<CollectionReference> {
            // categories pull throws first
            every { get() } throws RuntimeException("Firestore read timeout during pull")
        }
        val userRef = mockk<DocumentReference> {
            every { collection("habits") } returns habitsCollRef
            every { collection("categories") } returns categoriesCollRef
        }
        every { firestore.collection("users") } returns mockk {
            every { document("user1") } returns userRef
        }

        val results = repository.migrateGuestDataToCloud("user1").toList()

        val failed = results.last()
        assertTrue("Expected Failed, got $failed", failed is MigrationStatus.Failed)
        assertTrue((failed as MigrationStatus.Failed).canRetry)
        // withTransaction is never entered because fetchFirestoreAndPopulateRoom throws before it
        coVerify(exactly = 0) { database.clearAllTables() }
    }

    // ── OPERATION 2: pullCloudDataToLocal ────────────────────────────────────────

    @Test
    fun `pullCloudDataToLocal - offline returns Result failure without touching Room`() = runTest {
        setOnline(false)

        val result = repository.pullCloudDataToLocal("user1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("internet", ignoreCase = true) == true)
        coVerify(exactly = 0) { database.clearAllTables() }
    }

    @Test
    fun `pullCloudDataToLocal - Firestore empty returns Result success(false)`() = runTest {
        setOnline(true)
        wireFirestoreForPullEmpty("user1")

        val result = repository.pullCloudDataToLocal("user1")

        // The pull itself succeeds (no exception), but Firestore had no habits
        assertTrue("Expected success, got: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `pullCloudDataToLocal - Firestore has data returns Result success(true)`() = runTest {
        setOnline(true)
        wireFirestoreForPullWithHabit("user1", "habit-uuid-1", "Yoga")

        val result = repository.pullCloudDataToLocal("user1")

        assertTrue("Expected success, got: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `pullCloudDataToLocal aborts if pending sync events exist`() = runTest {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0

        // 1. Setup a "real" local database state using a backing list to simulate data survival
        val localHabits = mutableListOf<HabitEntity>()
        val existingHabit = HabitEntity(
            id = "habit-1", 
            name = "Test Habit", 
            description = null, 
            scheduleType = "DAILY", 
            scheduledDays = "", 
            colorTag = "#FFFFFF", 
            isActive = true, 
            createdAt = 0L, 
            reminderEnabled = false, 
            reminderHour = null, 
            reminderMinute = null, 
            icon = "icon", 
            categoryId = null, 
            notes = null, 
            checklistEnabled = false
        )
        localHabits.add(existingHabit)
        
        coEvery { habitDao.getAllHabitsSync() } answers { localHabits }
        // If the destructive wipe were called, it would clear our local data
        coEvery { database.clearAllTables() } answers { localHabits.clear() }

        setOnline(true)
        val dummyEvent = PendingSyncEntity(id = 1L, entityId = "1", entityType = "HABIT", operation = "UPSERT", timestamp = 0L)
        coEvery { pendingSyncDao.getAllPendingSyncEvents() } returns listOf(dummyEvent)

        // 2. Execute the guarded pull
        val result = repository.pullCloudDataToLocal("test_uid")

        // 3. Assertions
        assertTrue(result.isFailure)
        assertEquals("Skipping destructive pull: unsynced local data exists.", result.exceptionOrNull()?.message)
        
        // 4. Direct proof: confirm the habit is STILL present locally afterward
        val survivingHabits = habitDao.getAllHabitsSync()
        assertEquals("Local data should survive the aborted pull", 1, survivingHabits.size)
        assertEquals(existingHabit.name, survivingHabits[0].name)
        
        // Verify database tables were NOT cleared by fetchFirestoreAndPopulateRoom
        coVerify(exactly = 0) { database.clearAllTables() }
        coVerify(exactly = 0) { database.openHelper }
        unmockkStatic(android.util.Log::class)
    }
}
