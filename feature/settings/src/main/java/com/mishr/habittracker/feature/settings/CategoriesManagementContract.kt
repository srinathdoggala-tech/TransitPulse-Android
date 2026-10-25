package com.mishr.habittracker.feature.settings

import com.mishr.habittracker.domain.model.Category

data class CategoriesManagementState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet()
)

sealed interface CategoriesManagementIntent {
    data class ToggleSelection(val categoryId: String) : CategoriesManagementIntent
    data object ClearSelection : CategoriesManagementIntent
    data object DeleteSelected : CategoriesManagementIntent
}
