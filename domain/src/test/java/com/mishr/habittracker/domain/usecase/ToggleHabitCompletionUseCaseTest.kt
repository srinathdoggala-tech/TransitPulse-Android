package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.HabitCompletion
import com.mishr.habittracker.domain.repository.HabitCompletionRepository
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleHabitCompletionUseCaseTest {

    private lateinit var classUnderTest: ToggleHabitCompletionUseCase
    private lateinit var fakeRepository: FakeHabitCompletionRepository
    private val today = LocalDate(2026, 8, 12)

    @Before
    fun setup() {
        fakeRepository = FakeHabitCompletionRepository()
        classUnderTest = ToggleHabitCompletionUseCase(fakeRepository)
    }

    @Test
    fun `toggle on uncompleted date marks complete`() = runBlocking {
        val result = classUnderTest("habit1", today)
        
        assertTrue(result is AppResult.Success<*>)
        assertEquals(1, fakeRepository.completions.value.size)
        assertEquals("habit1", fakeRepository.completions.value.first().habitId)
        assertEquals(today, fakeRepository.completions.value.first().completedDate)
    }

    @Test
    fun `toggle on completed date unmarks complete`() = runBlocking {
        // First mark it complete
        classUnderTest("habit1", today)
        assertEquals(1, fakeRepository.completions.value.size)
        
        // Then toggle again to unmark
        val result = classUnderTest("habit1", today)
        
        assertTrue(result is AppResult.Success<*>)
        assertEquals(0, fakeRepository.completions.value.size)
    }
}

class FakeHabitCompletionRepository : HabitCompletionRepository {
    val completions = MutableStateFlow<List<HabitCompletion>>(emptyList())

    override fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>> {
        return completions
    }

    override suspend fun markComplete(completion: HabitCompletion): AppResult<Unit> {
        completions.update { it + completion }
        return AppResult.Success(Unit)
    }

    override suspend fun unmarkComplete(habitId: String, date: LocalDate): AppResult<Unit> {
        completions.update { list -> 
            list.filterNot { it.habitId == habitId && it.completedDate == date }
        }
        return AppResult.Success(Unit)
    }
}
