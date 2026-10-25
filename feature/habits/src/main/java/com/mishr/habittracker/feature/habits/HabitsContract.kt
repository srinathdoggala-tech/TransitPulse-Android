package com.mishr.habittracker.feature.habits

import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.model.DayOfWeek
import com.mishr.habittracker.domain.model.HabitWithStatus
import com.mishr.habittracker.domain.model.ScheduleType
import com.mishr.habittracker.domain.model.ThemeMode
import com.mishr.habittracker.domain.model.Category

data class HabitsState(
    val habits: List<HabitWithStatus> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val showCreateSheet: Boolean = false,
    val error: AppError? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

sealed interface HabitsIntent {
    data object LoadHabits : HabitsIntent
    data object Refresh : HabitsIntent
    data class ToggleCompletion(val habitId: String) : HabitsIntent
    data class ToggleChecklistItem(val habitId: String, val checklistItemId: String, val isCompleted: Boolean) : HabitsIntent
    data class UpdateHabit(val habit: com.mishr.habittracker.domain.model.Habit) : HabitsIntent
    data class UpdateHabitNotes(val habitId: String, val notes: String?) : HabitsIntent
    data class DeleteHabit(val id: String) : HabitsIntent
    data object OpenCreateSheet : HabitsIntent
    data object DismissCreateSheet : HabitsIntent
    data object ToggleThemeMode : HabitsIntent
    data class SetCategoryFilter(val categoryId: String?) : HabitsIntent
    data class CreateCategory(val name: String, val colorTag: String) : HabitsIntent
    data class CreateHabit(
        val name: String,
        val description: String,
        val scheduleType: ScheduleType,
        val scheduledDays: Set<DayOfWeek>,
        val colorTag: String,
        val iconKey: String,
        val reminderEnabled: Boolean = false,
        val reminderHour: Int? = null,
        val reminderMinute: Int? = null,
        val categoryId: String? = null,
        val checklistEnabled: Boolean = false,
        val checklistItems: List<String> = emptyList()
    ) : HabitsIntent
}

sealed interface HabitsEffect {
    data class ShowError(val message: String) : HabitsEffect
    data object RequestExactAlarmPermission : HabitsEffect
}

