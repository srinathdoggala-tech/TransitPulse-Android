package com.mishr.habittracker.domain.repository

import com.mishr.habittracker.domain.model.Category
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun createCategory(name: String, colorTag: String): AppResult<Category>
    suspend fun deleteCategory(id: String): AppResult<Unit>
}
