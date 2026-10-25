package com.mishr.habittracker.core.database.converter

import androidx.room.TypeConverter
import com.mishr.habittracker.domain.model.DayOfWeek
import com.mishr.habittracker.domain.model.ScheduleType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Unified TypeConverter class for Room database.
 */
class Converters {

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String {
        if (days.isEmpty()) return ""
        return days.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(data: String): Set<DayOfWeek> {
        if (data.isBlank()) return emptySet()
        return data.split(",").map { DayOfWeek.valueOf(it.trim()) }.toSet()
    }

    @TypeConverter
    fun fromScheduleType(type: ScheduleType): String = type.name

    @TypeConverter
    fun toScheduleType(data: String): ScheduleType = ScheduleType.valueOf(data)

    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(millis: Long): Instant = Instant.fromEpochMilliseconds(millis)

    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(data: String): LocalDate = LocalDate.parse(data)
}
