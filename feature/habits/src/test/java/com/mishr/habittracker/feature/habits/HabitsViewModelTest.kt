package com.mishr.habittracker.feature.habits


import com.mishr.habittracker.domain.model.DayOfWeek
import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.model.HabitWithStatus
import com.mishr.habittracker.domain.model.ScheduleType
import com.mishr.habittracker.domain.model.AuthState
import com.mishr.habittracker.domain.repository.MigrationRepository
import com.mishr.habittracker.domain.repository.SyncRepository
import com.mishr.habittracker.domain.sync.SyncTrigger
import com.mishr.habittracker.domain.usecase.ObserveAuthStateUseCase
import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.domain.usecase.CreateHabitUseCase
import com.mishr.habittracker.domain.usecase.GetHabitsWithStatusUseCase
import com.mishr.habittracker.domain.usecase.ToggleHabitCompletionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockGetHabitsUseCase: GetHabitsWithStatusUseCase = mockk()
    private val mockToggleUseCase: ToggleHabitCompletionUseCase = mockk()
    private val mockCreateUseCase: CreateHabitUseCase = mockk()
    private val mockCreateCategoryUseCase: com.mishr.habittracker.domain.usecase.CreateCategoryUseCase = mockk()
    private val mockUpdateHabitNotesUseCase: com.mishr.habittracker.domain.usecase.UpdateHabitNotesUseCase = mockk()
    private val mockToggleChecklistItemUseCase: com.mishr.habittracker.domain.usecase.ToggleChecklistItemUseCase = mockk()
    private val mockCategoryRepository: com.mishr.habittracker.domain.repository.CategoryRepository = mockk()
    private val mockHabitRepository: com.mishr.habittracker.domain.repository.HabitRepository = mockk()
    private val mockUserPreferencesRepository: com.mishr.habittracker.domain.repository.UserPreferencesRepository = mockk()
    private val mockScheduleReminderUseCase: com.mishr.habittracker.domain.usecase.ScheduleReminderUseCase = mockk()
    private val mockCancelReminderUseCase: com.mishr.habittracker.domain.usecase.CancelReminderUseCase = mockk()
    private val mockContext: android.content.Context = mockk()

    private val mockSyncRepository: SyncRepository = mockk()
    private val mockMigrationRepository: MigrationRepository = mockk()
    private val mockObserveAuthStateUseCase: ObserveAuthStateUseCase = mockk()
    private val mockSyncTrigger: SyncTrigger = mockk()
    private val mockStartCloudSyncUseCase: com.mishr.habittracker.domain.usecase.StartCloudSyncUseCase = mockk()
    private val mockStopCloudSyncUseCase: com.mishr.habittracker.domain.usecase.StopCloudSyncUseCase = mockk()

    private val habitsFlow = MutableStateFlow<List<HabitWithStatus>>(emptyList())

    private lateinit var viewModel: HabitsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { mockGetHabitsUseCase() } returns habitsFlow
        every { mockCategoryRepository.getAllCategories() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        every { mockUserPreferencesRepository.getThemeMode() } returns kotlinx.coroutines.flow.flowOf(com.mishr.habittracker.domain.model.ThemeMode.SYSTEM)
        every { mockObserveAuthStateUseCase() } returns kotlinx.coroutines.flow.flowOf(AuthState.Unauthenticated)
        every { mockSyncRepository.getPendingSyncCount() } returns kotlinx.coroutines.flow.flowOf(0)
        every { mockStartCloudSyncUseCase(any()) } returns Unit
        every { mockStopCloudSyncUseCase() } returns Unit
        
        viewModel = HabitsViewModel(
            getHabitsWithStatusUseCase = mockGetHabitsUseCase,
            toggleHabitCompletionUseCase = mockToggleUseCase,
            createHabitUseCase = mockCreateUseCase,
            createCategoryUseCase = mockCreateCategoryUseCase,
            updateHabitNotesUseCase = mockUpdateHabitNotesUseCase,
            toggleChecklistItemUseCase = mockToggleChecklistItemUseCase,
            categoryRepository = mockCategoryRepository,
            habitRepository = mockHabitRepository,
            userPreferencesRepository = mockUserPreferencesRepository,
            scheduleReminderUseCase = mockScheduleReminderUseCase,
            cancelReminderUseCase = mockCancelReminderUseCase,
            syncRepository = mockSyncRepository,
            migrationRepository = mockMigrationRepository,
            observeAuthStateUseCase = mockObserveAuthStateUseCase,
            syncTrigger = mockSyncTrigger,
            startCloudSyncUseCase = mockStartCloudSyncUseCase,
            stopCloudSyncUseCase = mockStopCloudSyncUseCase,
            context = mockContext
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `LoadHabits updates state with habits`() = runTest {
        val testHabit = HabitWithStatus(
            habit = Habit(
                id = UUID.randomUUID().toString(),
                name = "Read",
                description = null,
                scheduleType = ScheduleType.DAILY,
                scheduledDays = emptySet(),
                colorTag = "#FFFFFF",
                createdAt = Clock.System.now()
            ),
            isCompletedToday = false,
            currentStreak = 0,
            bestStreak = 0,
            completedDates = emptySet()
        )
        

        habitsFlow.value = listOf(testHabit)
        testScheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.habits.size)
        assertEquals("Read", state.habits[0].habit.name)
    }

    @Test
    fun `processIntent ToggleThemeMode updates theme in preferences`() = runTest {
        coEvery { mockUserPreferencesRepository.setThemeMode(any()) } returns AppResult.Success(Unit)
        
        viewModel.processIntent(HabitsIntent.ToggleThemeMode)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { mockUserPreferencesRepository.setThemeMode(com.mishr.habittracker.domain.model.ThemeMode.DARK) }
    }

    @Test
    fun `processIntent Refresh triggers stop and start cloud sync usecases`() = runTest {
        every { mockObserveAuthStateUseCase() } returns kotlinx.coroutines.flow.flowOf(AuthState.Authenticated("user1", null, null))
        
        viewModel.processIntent(HabitsIntent.Refresh)
        
        // Wait for coroutine to process
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { mockStopCloudSyncUseCase() }
        coVerify { mockStartCloudSyncUseCase("user1") }
        // Ensure pullCloudDataToLocal is NOT called
        coVerify(exactly = 0) { mockMigrationRepository.pullCloudDataToLocal(any()) }
    }

    @Test
    fun `ToggleCompletion calls use case`() = runTest {
        coEvery { mockToggleUseCase(any(), any()) } returns AppResult.Success(Unit)
        
        viewModel.processIntent(HabitsIntent.ToggleCompletion("habit_1"))
        testScheduler.advanceUntilIdle()
        
        coVerify { mockToggleUseCase("habit_1", any()) }
    }

    @Test
    fun `CreateHabit calls use case and dismisses sheet`() = runTest {
        coEvery { mockCreateUseCase(any(), any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        
        viewModel.processIntent(HabitsIntent.OpenCreateSheet)
        assertTrue(viewModel.state.value.showCreateSheet)
        
        viewModel.processIntent(HabitsIntent.CreateHabit(
            name = "New Habit",
            description = "",
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#FFFFFF",
            iconKey = "star",
            reminderEnabled = false,
            reminderHour = null,
            reminderMinute = null
        ))
        testScheduler.advanceUntilIdle()
        
        coVerify { mockCreateUseCase(
            name = "New Habit",
            description = "",
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#FFFFFF",
            iconKey = "star",
            reminderEnabled = false,
            reminderHour = null,
            reminderMinute = null
        ) }
        assertFalse(viewModel.state.value.showCreateSheet)
    }

    @Test
    fun `CreateHabit with error emits ShowError effect`() = runTest {
        val errorMessage = "Habit name cannot be blank."
        coEvery { mockCreateUseCase(any(), any(), any(), any(), any(), any(), any()) } returns AppResult.Error(com.mishr.habittracker.domain.result.AppError.UnknownError(Exception(errorMessage)))
        
        val effects = mutableListOf<HabitsEffect>()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        
        viewModel.processIntent(HabitsIntent.CreateHabit(
            name = "",
            description = "",
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#FFFFFF",
            iconKey = "star",
            reminderEnabled = false,
            reminderHour = null,
            reminderMinute = null
        ))
        testScheduler.advanceUntilIdle()
        
        assertEquals(1, effects.size)
        assertTrue(effects[0] is HabitsEffect.ShowError)
        assertEquals(errorMessage, (effects[0] as HabitsEffect.ShowError).message)
    }

    @Test
    fun `ToggleCompletion with error emits ShowError effect`() = runTest {
        val errorMessage = "Database error"
        coEvery { mockToggleUseCase(any(), any()) } returns AppResult.Error(com.mishr.habittracker.domain.result.AppError.DatabaseError(errorMessage))
        
        val effects = mutableListOf<HabitsEffect>()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        
        viewModel.processIntent(HabitsIntent.ToggleCompletion("habit_1"))
        testScheduler.advanceUntilIdle()
        
        assertEquals(1, effects.size)
        assertTrue(effects[0] is HabitsEffect.ShowError)
        assertEquals("Failed to update habit status", (effects[0] as HabitsEffect.ShowError).message)
    }
}
