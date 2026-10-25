package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.Category
import com.mishr.habittracker.domain.repository.CategoryRepository
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(name: String, colorTag: String): AppResult<Category> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return AppResult.Error(AppError.UnknownError(Exception("Category name cannot be blank.")))
        }
        
        val existingCategories = categoryRepository.getAllCategories().first()
        if (existingCategories.any { it.name.equals(trimmedName, ignoreCase = true) }) {
            return AppResult.Error(AppError.UnknownError(Exception("A category with this name already exists.")))
        }

        return categoryRepository.createCategory(trimmedName, colorTag)
    }
}