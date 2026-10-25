package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.repository.ChecklistItemCompletionRepository
import com.mishr.habittracker.domain.repository.HabitCompletionRepository
import com.mishr.habittracker.domain.repository.HabitRepository
import com.mishr.habittracker.domain.model.ChecklistItemCompletion
import com.mishr.habittracker.domain.model.HabitCompletion
import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.datetime.LocalDate

class ToggleChecklistItemUseCaseTest {

    class FakeHabitRepo(val habit: Habit) : HabitRepository {
        override fun getAllHabits(): Flow<List<Habit>> = flowOf(listOf(habit))
        override fun getHabitById(id: String): Flow<Habit?> = flowOf(habit)
        override suspend fun createHabit(habit: Habit): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateHabit(habit: Habit): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteHabit(id: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    class FakeChecklistRepo(var completions: MutableList<ChecklistItemCompletion> = mutableListOf()) : ChecklistItemCompletionRepository {
        var addCalled = 0
        var removeCalled = 0
        override fun getCompletionsForHabitAndDate(habitId: String, date: LocalDate): Flow<List<ChecklistItemCompletion>> {
            return flowOf(completions.filter { it.habitId == habitId && it.completedDate == date })
        }
        override suspend fun markItemComplete(completion: ChecklistItemCompletion): AppResult<Unit> {
            addCalled++
            completions.add(completion)
            return AppResult.Success(Unit)
        }
        override suspend fun unmarkItemComplete(habitId: String, checklistItemId: String, date: LocalDate): AppResult<Unit> {
            removeCalled++
            completions.removeIf { it.habitId == habitId && it.checklistItemId == checklistItemId && it.completedDate == date }
            return AppResult.Success(Unit)
        }
    }

    class FakeHabitCompletionRepo : HabitCompletionRepository {
        var markCalled = 0
        var removeCalled = 0
        override fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>> = flowOf(emptyList())
        override suspend fun markComplete(completion: HabitCompletion): AppResult<Unit> {
            markCalled++
            return AppResult.Success(Unit)
        }
        override suspend fun unmarkComplete(habitId: String, date: LocalDate): AppResult<Unit> {
            removeCalled++
            return AppResult.Success(Unit)
        }
    }

    @Test
    fun checkingLastRemainingItemTriggersHabitCompletionWrite() = runBlocking {
        val habitId = "habit-1"
        val itemId1 = "item-1"
        val itemId2 = "item-2"
        
        val habit = Habit(
            id = habitId,
            name = "Test Habit",
            description = null,
            scheduleType = com.mishr.habittracker.domain.model.ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#000000",
            isActive = true,
            createdAt = Clock.System.now(),
            icon = "star",
            reminderEnabled = false,
            checklistEnabled = true,
            categoryId = null,
            notes = null,
            checklistItems = listOf(
                com.mishr.habittracker.domain.model.ChecklistItem(itemId1, "Item 1", 0),
                com.mishr.habittracker.domain.model.ChecklistItem(itemId2, "Item 2", 1)
            )
        )
        
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val existingCompletion = ChecklistItemCompletion("comp-1", habitId, itemId1, today)
        
        val fakeHabitRepo = FakeHabitRepo(habit)
        val fakeChecklistRepo = FakeChecklistRepo(mutableListOf(existingCompletion))
        val fakeHabitCompletionRepo = FakeHabitCompletionRepo()
        val realToggleUseCase = ToggleHabitCompletionUseCase(fakeHabitCompletionRepo)
        
        val useCase = ToggleChecklistItemUseCase(
            habitRepository = fakeHabitRepo,
            checklistItemCompletionRepository = fakeChecklistRepo,
            toggleHabitCompletionUseCase = realToggleUseCase
        )

        // Act: complete the second item
        val result = useCase(habitId, itemId2, true)
        
        // Assert
        assertTrue(result is AppResult.Success)
        assertTrue(fakeChecklistRepo.addCalled == 1)
        assertTrue(fakeHabitCompletionRepo.markCalled == 1) // Triggered a HabitCompletion
    }

    @Test
    fun uncheckingAnyItemAfterFullCompletionRemovesHabitCompletion() = runBlocking {
        val habitId = "habit-1"
        val itemId1 = "item-1"
        val itemId2 = "item-2"
        
        val habit = Habit(
            id = habitId,
            name = "Test Habit",
            description = null,
            scheduleType = com.mishr.habittracker.domain.model.ScheduleType.DAILY,
            scheduledDays = emptySet(),
            colorTag = "#000000",
            isActive = true,
            createdAt = Clock.System.now(),
            icon = "star",
            reminderEnabled = false,
            checklistEnabled = true,
            categoryId = null,
            notes = null,
            checklistItems = listOf(
                com.mishr.habittracker.domain.model.ChecklistItem(itemId1, "Item 1", 0),
                com.mishr.habittracker.domain.model.ChecklistItem(itemId2, "Item 2", 1)
            )
        )
        
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        val existingCompletions = mutableListOf(
            ChecklistItemCompletion("comp-1", habitId, itemId1, today),
            ChecklistItemCompletion("comp-2", habitId, itemId2, today)
        )
        
        val fakeHabitRepo = FakeHabitRepo(habit)
        val fakeChecklistRepo = FakeChecklistRepo(existingCompletions)
        
        // Setup repo to return 1 completion to simulate that the habit was fully completed
        val fakeHabitCompletionRepoWithData = object : HabitCompletionRepository {
            var removeCalled = 0
            override fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>> {
                return flowOf(listOf(HabitCompletion("uuid", habitId, today, kotlinx.datetime.Instant.fromEpochMilliseconds(0))))
            }
            override suspend fun markComplete(completion: HabitCompletion): AppResult<Unit> = AppResult.Success(Unit)
            override suspend fun unmarkComplete(habitId: String, date: LocalDate): AppResult<Unit> {
                removeCalled++
                return AppResult.Success(Unit)
            }
        }
        
        val realToggleUseCase = ToggleHabitCompletionUseCase(fakeHabitCompletionRepoWithData)
        
        val useCase = ToggleChecklistItemUseCase(
            habitRepository = fakeHabitRepo,
            checklistItemCompletionRepository = fakeChecklistRepo,
            toggleHabitCompletionUseCase = realToggleUseCase
        )

        // Act: uncheck the second item
        val result = useCase(habitId, itemId2, false)
        
        // Assert
        assertTrue(result is AppResult.Success)
        assertTrue(fakeChecklistRepo.removeCalled == 1)
        assertTrue(fakeHabitCompletionRepoWithData.removeCalled == 1) // Triggered a HabitRemoval
    }
}
