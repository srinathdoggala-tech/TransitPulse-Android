package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelReminderUseCaseTest {

    private val fakeScheduler = FakeReminderScheduler()
    private val classUnderTest = CancelReminderUseCase(fakeScheduler)

    @Test
    fun `cancel calls scheduler with correct parameters`() {
        val result = classUnderTest("habit_456")
        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeScheduler.cancelled.size)
        assertEquals("habit_456", fakeScheduler.cancelled.first())
    }
}
