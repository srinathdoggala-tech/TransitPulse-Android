package com.mishr.habittracker.core.firebase.sync

import com.mishr.habittracker.core.database.entity.CategoryEntity
import com.mishr.habittracker.core.database.entity.ChecklistItemCompletionEntity
import com.mishr.habittracker.core.database.entity.ChecklistItemEntity
import com.mishr.habittracker.core.database.entity.HabitCompletionEntity
import com.mishr.habittracker.core.database.entity.HabitEntity

object EntityMapper {

    fun categoryToMap(cat: CategoryEntity): Map<String, Any> {
        return mapOf(
            "id" to cat.id,
            "name" to cat.name,
            "colorTag" to cat.colorTag,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
    }

    fun mapToCategory(data: Map<String, Any>): CategoryEntity? {
        val id = data["id"] as? String ?: return null
        val name = data["name"] as? String ?: return null
        val colorTag = data["colorTag"] as? String ?: return null
        return CategoryEntity(id, name, colorTag)
    }

    fun mapToHabit(data: Map<String, Any>): HabitEntity? {
        val id = data["id"] as? String ?: return null
        val name = data["name"] as? String ?: return null
        val scheduleType = data["scheduleType"] as? String ?: return null
        val scheduledDays = (data["scheduledDays"] as? List<*>)?.joinToString(",") ?: ""
        val colorTag = data["colorTag"] as? String ?: return null
        val isActive = data["isActive"] as? Boolean ?: true
        val createdAtTs = data["createdAt"] as? com.google.firebase.Timestamp
        val createdAt = createdAtTs?.let { it.seconds * 1000 + it.nanoseconds / 1000000 } ?: System.currentTimeMillis()
        val reminderEnabled = data["reminderEnabled"] as? Boolean ?: false
        val reminderHour = (data["reminderHour"] as? Number)?.toInt()
        val reminderMinute = (data["reminderMinute"] as? Number)?.toInt()
        val icon = data["icon"] as? String ?: ""
        val categoryId = data["categoryId"] as? String
        val notes = data["notes"] as? String
        val description = data["description"] as? String
        val checklistEnabled = data["checklistEnabled"] as? Boolean ?: false

        return HabitEntity(
            id = id,
            name = name,
            description = description,
            scheduleType = scheduleType,
            scheduledDays = scheduledDays,
            colorTag = colorTag,
            isActive = isActive,
            createdAt = createdAt,
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            icon = icon,
            categoryId = categoryId,
            notes = notes,
            checklistEnabled = checklistEnabled
        )
    }

    fun mapToChecklistItems(data: Map<String, Any>, habitId: String): List<ChecklistItemEntity> {
        val itemsList = data["checklistItems"] as? List<Map<String, Any>> ?: return emptyList()
        return itemsList.mapNotNull { item ->
            val id = item["id"] as? String ?: return@mapNotNull null
            val text = item["text"] as? String ?: return@mapNotNull null
            val order = (item["order"] as? Number)?.toInt() ?: 0
            ChecklistItemEntity(id, habitId, text, order)
        }
    }

    fun mapToHabitCompletion(data: Map<String, Any>): HabitCompletionEntity? {
        val id = data["id"] as? String ?: return null
        val habitId = data["habitId"] as? String ?: return null
        val completedDate = data["completedDate"] as? String ?: return null
        val completedAtTs = data["completedAt"] as? com.google.firebase.Timestamp
        val completedAt = completedAtTs?.let { it.seconds * 1000 + it.nanoseconds / 1000000 } ?: System.currentTimeMillis()
        return HabitCompletionEntity(id, habitId, completedDate, completedAt)
    }

    fun mapToChecklistCompletion(data: Map<String, Any>): ChecklistItemCompletionEntity? {
        val id = data["id"] as? String ?: return null
        val habitId = data["habitId"] as? String ?: return null
        val checklistItemId = data["checklistItemId"] as? String ?: return null
        val completedDate = data["completedDate"] as? String ?: return null
        return ChecklistItemCompletionEntity(id, habitId, checklistItemId, completedDate)
    }

    fun habitToMap(habit: HabitEntity, checklistItems: List<ChecklistItemEntity>): Map<String, Any> {
        val habitItems = checklistItems.map { item ->
            mapOf("id" to item.id, "text" to item.text, "order" to item.itemOrder)
        }
        return mapOf(
            "id" to habit.id,
            "name" to habit.name,
            "description" to (habit.description ?: ""),
            "scheduleType" to habit.scheduleType,
            "scheduledDays" to habit.scheduledDays.split(",").filter { it.isNotBlank() },
            "colorTag" to habit.colorTag,
            "isActive" to habit.isActive,
            "createdAt" to com.google.firebase.Timestamp(habit.createdAt / 1000, ((habit.createdAt % 1000) * 1000000).toInt()),
            "reminderEnabled" to habit.reminderEnabled,
            "reminderHour" to (habit.reminderHour ?: 0),
            "reminderMinute" to (habit.reminderMinute ?: 0),
            "icon" to habit.icon,
            "categoryId" to (habit.categoryId ?: ""),
            "notes" to (habit.notes ?: ""),
            "checklistEnabled" to habit.checklistEnabled,
            "checklistItems" to habitItems,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
    }

    fun habitCompletionToMap(completion: HabitCompletionEntity): Map<String, Any> {
        return mapOf(
            "id" to completion.id,
            "habitId" to completion.habitId,
            "completedDate" to completion.completedDate,
            "completedAt" to com.google.firebase.Timestamp(completion.completedAt / 1000, ((completion.completedAt % 1000) * 1000000).toInt()),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
    }

    fun checklistCompletionToMap(completion: ChecklistItemCompletionEntity): Map<String, Any> {
        return mapOf(
            "id" to completion.id,
            "habitId" to completion.habitId,
            "checklistItemId" to completion.checklistItemId,
            "completedDate" to completion.completedDate,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
    }
}

