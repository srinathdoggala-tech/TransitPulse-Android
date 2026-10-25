package com.mishr.habittracker.core.database.repository

import com.mishr.habittracker.core.database.HabitTrackerDatabase
import com.mishr.habittracker.core.database.entity.CategoryEntity
import com.mishr.habittracker.core.database.entity.PendingSyncEntity
import com.mishr.habittracker.core.database.toDomain
import com.mishr.habittracker.domain.model.Category
import com.mishr.habittracker.domain.sync.SyncTrigger
import com.mishr.habittracker.domain.repository.CategoryRepository
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import com.mishr.habittracker.core.common.dispatcher.DispatcherProvider
import kotlinx.coroutines.withContext

class CategoryRepositoryImpl @Inject constructor(
    private val database: HabitTrackerDatabase,
    private val dispatcherProvider: DispatcherProvider,
    private val syncTrigger: SyncTrigger
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> {
        return database.categoryDao().getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createCategory(name: String, colorTag: String): AppResult<Category> = withContext(dispatcherProvider.io) {
        try {
            val cat = CategoryEntity(id = UUID.randomUUID().toString(), name = name, colorTag = colorTag)
            database.runInTransaction {
                database.categoryDao().insertCategory(cat)
                database.pendingSyncDao().insertSyncEvent(
                    PendingSyncEntity(entityId = cat.id, entityType = "CATEGORY", operation = "UPSERT")
                )
            }
            syncTrigger.scheduleSync()
            AppResult.Success(cat.toDomain())
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    override suspend fun deleteCategory(id: String): AppResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            database.runInTransaction {
                database.categoryDao().deleteCategoryById(id)
                database.pendingSyncDao().insertSyncEvent(
                    PendingSyncEntity(entityId = id, entityType = "CATEGORY", operation = "DELETE")
                )
            }
            syncTrigger.scheduleSync()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }
}
