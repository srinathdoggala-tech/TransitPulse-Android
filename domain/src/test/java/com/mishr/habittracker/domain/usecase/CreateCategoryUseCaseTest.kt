package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.Category
import com.mishr.habittracker.domain.repository.CategoryRepository
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateCategoryUseCaseTest {

    private lateinit var createCategoryUseCase: CreateCategoryUseCase

    private class FakeCategoryRepository(var categories: List<Category>) : CategoryRepository {
        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
        override suspend fun createCategory(name: String, colorTag: String): AppResult<Category> {
            return AppResult.Success(Category("1", name, colorTag))
        }
        override suspend fun deleteCategory(id: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    private lateinit var repository: FakeCategoryRepository

    @Before
    fun setup() {
        repository = FakeCategoryRepository(emptyList())
        createCategoryUseCase = CreateCategoryUseCase(repository)
    }

    @Test
    fun `when category name already exists, returns error`() = runBlocking {
        repository.categories = listOf(Category(id = "1", name = "Fitness", colorTag = "#FFFFFF"))

        val result = createCategoryUseCase("fitness", "#000000") // different case
        
        assertTrue(result is AppResult.Error)
        val errorMsg = ((result as AppResult.Error).error as AppError.UnknownError).throwable?.message
        assertTrue(errorMsg == "A category with this name already exists.")
    }

    @Test
    fun `when category name is unique, calls repository and returns success`() = runBlocking {
        repository.categories = emptyList()

        val result = createCategoryUseCase("Reading", "#000000")
        
        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertTrue(data.name == "Reading")
    }
}
