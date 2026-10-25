package com.mishr.habittracker.core.database

import com.mishr.habittracker.core.database.converter.Converters
import com.mishr.habittracker.core.database.entity.HabitCompletionEntity
import com.mishr.habittracker.core.database.entity.HabitEntity
import com.mishr.habittracker.core.database.entity.HabitWithChecklist
import com.mishr.habittracker.core.database.entity.ChecklistItemEntity
import com.mishr.habittracker.core.database.entity.ChecklistItemCompletionEntity
import com.mishr.habittracker.core.database.entity.CategoryEntity
import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.model.HabitCompletion
import com.mishr.habittracker.domain.model.ChecklistItem
import com.mishr.habittracker.domain.model.ChecklistItemCompletion
import com.mishr.habittracker.domain.model.Category

private val converters = Converters()

fun HabitWithChecklist.toDomain(): Habit {
    return Habit(
        id = habit.id,
        name = habit.name,
        description = habit.description,
        scheduleType = converters.toScheduleType(habit.scheduleType),
        scheduledDays = converters.toDayOfWeekSet(habit.scheduledDays),
        colorTag = habit.colorTag,
        isActive = habit.isActive,
        createdAt = converters.toInstant(habit.createdAt),
        reminderEnabled = habit.reminderEnabled,
        reminderHour = habit.reminderHour,
        reminderMinute = habit.reminderMinute,
        icon = habit.icon,
        categoryId = habit.categoryId,
        notes = habit.notes,
        checklistEnabled = habit.checklistEnabled,
        checklistItems = checklistItems.sortedBy { it.itemOrder }.map {
            ChecklistItem(
                id = it.id,
                text = it.text,
                order = it.itemOrder
            )
        }
    )
}

fun Habit.toEntity(): HabitEntity {
    return HabitEntity(
        id = id,
        name = name,
        description = description,
        scheduleType = converters.fromScheduleType(scheduleType),
        scheduledDays = converters.fromDayOfWeekSet(scheduledDays),
        colorTag = colorTag,
        isActive = isActive,
        createdAt = converters.fromInstant(createdAt),
        reminderEnabled = reminderEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        icon = icon,
        categoryId = categoryId,
        notes = notes,
        checklistEnabled = checklistEnabled
    )
}

fun HabitCompletionEntity.toDomain(): HabitCompletion {
    return HabitCompletion(
        id = id,
        habitId = habitId,
        completedDate = converters.toLocalDate(completedDate),
        completedAt = converters.toInstant(completedAt)
    )
}

fun HabitCompletion.toEntity(): HabitCompletionEntity {
    return HabitCompletionEntity(
        id = id,
        habitId = habitId,
        completedDate = converters.fromLocalDate(completedDate),
        completedAt = converters.fromInstant(completedAt)
    )
}

fun ChecklistItemCompletionEntity.toDomain(): ChecklistItemCompletion {
    return ChecklistItemCompletion(
        id = id,
        habitId = habitId,
        checklistItemId = checklistItemId,
        completedDate = converters.toLocalDate(completedDate)
    )
}

fun ChecklistItemCompletion.toEntity(): ChecklistItemCompletionEntity {
    return ChecklistItemCompletionEntity(
        id = id,
        habitId = habitId,
        checklistItemId = checklistItemId,
        completedDate = converters.fromLocalDate(completedDate)
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(id = id, name = name, colorTag = colorTag)
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(id = id, name = name, colorTag = colorTag)
}
