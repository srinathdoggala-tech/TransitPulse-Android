package com.mishr.habittracker.domain.model

/**
 * Custom DayOfWeek enum to keep the domain module platform-agnostic
 * and independent of java.time.DayOfWeek.
 */
enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}
