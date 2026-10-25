package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.repository.ReminderScheduler
import com.mishr.habittracker.domain.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleReminderUseCaseTest {

    private val fakeScheduler = FakeReminderScheduler()
    private val classUnderTest = ScheduleReminderUseCase(fakeScheduler)

    @Test
    fun `schedule calls scheduler with correct parameters`() {
        val result = classUnderTest("habit_123", 8, 30)
        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeScheduler.scheduled.size)
        assertEquals("habit_123", fakeScheduler.scheduled.first().habitId)
        assertEquals(8, fakeScheduler.scheduled.first().hour)
        assertEquals(30, fakeScheduler.scheduled.first().minute)
    }
}

class FakeReminderScheduler : ReminderScheduler {
    
    data class ScheduledReminder(val habitId: String, val hour: Int, val minute: Int)
    
    val scheduled = mutableListOf<ScheduledReminder>()
    val cancelled = mutableListOf<String>()
    
    override fun schedule(habitId: String, hour: Int, minute: Int): AppResult<Unit> {
        scheduled.add(ScheduledReminder(habitId, hour, minute))
        return AppResult.Success(Unit)
    }

    override fun cancel(habitId: String): AppResult<Unit> {
        cancelled.add(habitId)
        return AppResult.Success(Unit)
    }

    override fun rescheduleAll(habits: List<Habit>): AppResult<Unit> {
        return AppResult.Success(Unit)
    }
}
