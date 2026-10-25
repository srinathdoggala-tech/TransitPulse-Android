package com.mishr.habittracker.core.database

import com.mishr.habittracker.core.database.converter.Converters
import com.mishr.habittracker.domain.model.DayOfWeek
import com.mishr.habittracker.domain.model.ScheduleType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `DayOfWeek set converts correctly to and from string`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val stringDays = converters.fromDayOfWeekSet(days)
        assertEquals("MONDAY,WEDNESDAY,FRIDAY", stringDays)

        val convertedDays = converters.toDayOfWeekSet(stringDays)
        assertEquals(days, convertedDays)
    }

    @Test
    fun `Empty DayOfWeek set converts to empty string and back without containing empty string element`() {
        val days = emptySet<DayOfWeek>()
        val stringDays = converters.fromDayOfWeekSet(days)
        assertEquals("", stringDays)

        val convertedDays = converters.toDayOfWeekSet(stringDays)
        assertEquals(emptySet<DayOfWeek>(), convertedDays)
    }

    @Test
    fun `ScheduleType converts to and from string`() {
        val type = ScheduleType.WEEKLY
        val stringType = converters.fromScheduleType(type)
        assertEquals("WEEKLY", stringType)

        val convertedType = converters.toScheduleType(stringType)
        assertEquals(type, convertedType)
    }

    @Test
    fun `Instant converts to and from epoch milliseconds`() {
        val instant = Instant.fromEpochMilliseconds(1620000000000L)
        val millis = converters.fromInstant(instant)
        assertEquals(1620000000000L, millis)

        val convertedInstant = converters.toInstant(millis)
        assertEquals(instant, convertedInstant)
    }

    @Test
    fun `LocalDate converts to and from ISO-8601 string`() {
        val date = LocalDate(2026, 8, 11)
        val stringDate = converters.fromLocalDate(date)
        assertEquals("2026-08-11", stringDate)

        val convertedDate = converters.toLocalDate(stringDate)
        assertEquals(date, convertedDate)
    }
}
