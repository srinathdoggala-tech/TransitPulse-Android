package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.DayOfWeek
import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.model.ScheduleType
import com.mishr.habittracker.domain.repository.HabitRepository
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateHabitUseCaseTest {

    private lateinit var classUnderTest: CreateHabitUseCase
    private lateinit var fakeRepository: FakeHabitRepository
    private lateinit var fakeScheduler: FakeReminderScheduler
    private lateinit var scheduleReminderUseCase: ScheduleReminderUseCase

    @Before
    fun setup() {
        fakeRepository = FakeHabitRepository()
        fakeScheduler = FakeReminderScheduler()
        scheduleReminderUseCase = ScheduleReminderUseCase(fakeScheduler)
        classUnderTest = CreateHabitUseCase(fakeRepository, scheduleReminderUseCase)
    }

    @Test
    fun `blank name returns error`() = runBlocking {
        val result = classUnderTest(
            name = "   ",
            description = "",
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#000000",
            iconKey = "star"
        )
        
        assertTrue(result is AppResult.Error)
        assertEquals(0, fakeRepository.habits.value.size)
    }

    @Test
    fun `weekly schedule with no days returns error`() = runBlocking {
        val result = classUnderTest(
            name = "Read",
            description = "",
            scheduleType = ScheduleType.WEEKLY,
            scheduledDays = emptySet(), // Invalid for WEEKLY
            colorTag = "#000000",
            iconKey = "star"
        )
        
        assertTrue(result is AppResult.Error)
        assertEquals(0, fakeRepository.habits.value.size)
    }

    @Test
    fun `valid daily habit is created successfully`() = runBlocking {
        val result = classUnderTest(
            name = "Read",
            description = "",
            scheduleType = ScheduleType.DAILY,
            scheduledDays = setOf(DayOfWeek.MONDAY), // Should be ignored/cleared for DAILY
            colorTag = "#000000",
            iconKey = "star"
        )
        
        assertTrue(result is AppResult.Success<*>)
        assertEquals(1, fakeRepository.habits.value.size)
        val created = fakeRepository.habits.value.first()
        assertEquals("Read", created.name)
        assertEquals(ScheduleType.DAILY, created.scheduleType)
        assertTrue(created.scheduledDays.isEmpty()) // Should have cleared the days
    }

    @Test
    fun `valid weekly habit is created successfully`() = runBlocking {
        val result = classUnderTest(
            name = "Gym",
            description = "",
            scheduleType = ScheduleType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            colorTag = "#FF0000",
            iconKey = "star"
        )
        
        assertTrue(result is AppResult.Success<*>)
        assertEquals(1, fakeRepository.habits.value.size)
        val created = fakeRepository.habits.value.first()
        assertEquals("Gym", created.name)
        assertEquals(ScheduleType.WEEKLY, created.scheduleType)
        assertEquals(2, created.scheduledDays.size)
    }

    @Test
    fun `reminder enabled with null time returns error`() = runBlocking {
        val result = classUnderTest(
            name = "Gym",
            description = "",
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#FF0000",
            iconKey = "star",
            reminderEnabled = true,
            reminderHour = null,
            reminderMinute = null
        )
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `reminder enabled with invalid hour returns error`() = runBlocking {
        val result = classUnderTest(
            name = "Gym",
            description = "",
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#FF0000",
            iconKey = "star",
            reminderEnabled = true,
            reminderHour = 24,
            reminderMinute = 30
        )
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `reminder enabled with valid time is created successfully`() = runBlocking {
        val result = classUnderTest(
            name = "Gym",
            description = "",
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#FF0000",
            iconKey = "star",
            reminderEnabled = true,
            reminderHour = 8,
            reminderMinute = 30
        )
        assertTrue(result is AppResult.Success<*>)
        val created = fakeRepository.habits.value.first()
        assertTrue(created.reminderEnabled)
        assertEquals(8, created.reminderHour)
        assertEquals(30, created.reminderMinute)
    }
}

class FakeHabitRepository : HabitRepository {
    val habits = MutableStateFlow<List<Habit>>(emptyList())

    override fun getAllHabits(): Flow<List<Habit>> = habits

    override fun getHabitById(id: String): Flow<Habit?> = MutableStateFlow(null)

    override suspend fun createHabit(habit: Habit): AppResult<Unit> {
        habits.update { it + habit }
        return AppResult.Success(Unit)
    }

    override suspend fun updateHabit(habit: Habit): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override suspend fun deleteHabit(id: String): AppResult<Unit> {
        return AppResult.Success(Unit)
    }
}
