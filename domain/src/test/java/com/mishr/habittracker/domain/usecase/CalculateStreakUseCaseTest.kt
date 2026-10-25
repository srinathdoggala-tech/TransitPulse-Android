package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.DayOfWeek
import com.mishr.habittracker.domain.model.HabitCompletion
import com.mishr.habittracker.domain.model.ScheduleType
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CalculateStreakUseCaseTest {

    private lateinit var classUnderTest: CalculateStreakUseCase
    private val today = LocalDate(2026, 8, 12) // A Wednesday for reference

    @Before
    fun setup() {
        classUnderTest = CalculateStreakUseCase()
    }

    private fun createCompletion(date: LocalDate): HabitCompletion {
        return HabitCompletion(
            id = UUID.randomUUID().toString(),
            habitId = "test_habit",
            completedDate = date,
            completedAt = Clock.System.now()
        )
    }

    @Test
    fun `empty list returns 0`() {
        val streak = classUnderTest(
            completions = emptyList(),
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            today = today
        )
        assertEquals(0, streak)
    }

    @Test
    fun `single completion today returns 1 for DAILY`() {
        val completions = listOf(createCompletion(today))
        
        val streak = classUnderTest(
            completions = completions,
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            today = today
        )
        assertEquals(1, streak)
    }

    @Test
    fun `broken streak DAILY returns correct count`() {
        // Completed today, yesterday, and 4 days ago (missing 2 days ago)
        val completions = listOf(
            createCompletion(today),
            createCompletion(today.minus(1, DateTimeUnit.DAY)),
            createCompletion(today.minus(4, DateTimeUnit.DAY))
        )
        
        val streak = classUnderTest(
            completions = completions,
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            today = today
        )
        assertEquals(2, streak)
    }

    @Test
    fun `missing today on DAILY does not break yesterday streak`() {
        // Completed yesterday and day before, but NOT today.
        // The user still has a streak of 2, because today isn't over.
        val completions = listOf(
            createCompletion(today.minus(1, DateTimeUnit.DAY)),
            createCompletion(today.minus(2, DateTimeUnit.DAY))
        )
        
        val streak = classUnderTest(
            completions = completions,
            scheduleType = ScheduleType.DAILY,
            scheduledDays = emptySet(),
            today = today
        )
        assertEquals(2, streak)
    }

    @Test
    fun `WEEKLY habit with non-scheduled day gap does NOT break streak`() {
        // Today is Wednesday (2026-08-12)
        // Scheduled: MONDAY, WEDNESDAY, FRIDAY
        val scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        
        // Completed today (Wed), Monday, and last Friday
        val completions = listOf(
            createCompletion(today), // Wed
            createCompletion(today.minus(2, DateTimeUnit.DAY)), // Mon
            createCompletion(today.minus(5, DateTimeUnit.DAY)) // Last Fri
        )
        
        val streak = classUnderTest(
            completions = completions,
            scheduleType = ScheduleType.WEEKLY,
            scheduledDays = scheduledDays,
            today = today
        )
        // Streak should be 3, because Tuesday, Sunday, Saturday are not scheduled and don't break it
        assertEquals(3, streak)
    }

    @Test
    fun `WEEKLY habit with scheduled day gap DOES break streak`() {
        // Today is Wednesday
        // Scheduled: MONDAY, WEDNESDAY, FRIDAY
        val scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        
        // Completed today (Wed), and last Friday, but MISSED Monday
        val completions = listOf(
            createCompletion(today), // Wed
            createCompletion(today.minus(5, DateTimeUnit.DAY)) // Last Fri
        )
        
        val streak = classUnderTest(
            completions = completions,
            scheduleType = ScheduleType.WEEKLY,
            scheduledDays = scheduledDays,
            today = today
        )
        // Streak should be 1 (just today), because Monday was missed.
        assertEquals(1, streak)
    }

    @Test
    fun `WEEKLY habit with unscheduled today and a missed PAST scheduled day breaks streak`() {
        // Today is Sunday (2026-08-16)
        val testToday = LocalDate(2026, 8, 16)
        
        // Scheduled: MONDAY, FRIDAY
        val scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        
        // Missed last Friday (Aug 14), but completed Monday before it (Aug 10)
        val completions = listOf(
            createCompletion(testToday.minus(6, DateTimeUnit.DAY)) // Monday
        )
        
        val streak = classUnderTest(
            completions = completions,
            scheduleType = ScheduleType.WEEKLY,
            scheduledDays = scheduledDays,
            today = testToday
        )
        
        // Streak should be 0 because Friday (a scheduled day) was missed and is fully over.
        assertEquals(0, streak)
    }
}
