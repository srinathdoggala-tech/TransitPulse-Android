package com.mishr.habittracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mishr.habittracker.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<CategoriesManagementState> = combine(
        categoryRepository.getAllCategories(),
        selectedIds
    ) { categories, selected ->
        CategoriesManagementState(
            categories = categories,
            selectedCategoryIds = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesManagementState()
    )

    fun processIntent(intent: CategoriesManagementIntent) {
        when (intent) {
            is CategoriesManagementIntent.ToggleSelection -> {
                selectedIds.update { current ->
                    if (current.contains(intent.categoryId)) {
                        current - intent.categoryId
                    } else {
                        current + intent.categoryId
                    }
                }
            }
            is CategoriesManagementIntent.ClearSelection -> {
                selectedIds.value = emptySet()
            }
            is CategoriesManagementIntent.DeleteSelected -> {
                viewModelScope.launch {
                    val idsToDelete = selectedIds.value
                    idsToDelete.forEach { id ->
                        categoryRepository.deleteCategory(id)
                    }
                    selectedIds.value = emptySet()
                }
            }
        }
    }
}
