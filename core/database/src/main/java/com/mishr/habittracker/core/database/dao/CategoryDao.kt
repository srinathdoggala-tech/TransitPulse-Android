package com.mishr.habittracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mishr.habittracker.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    fun deleteCategoryById(id: String)

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategorySyncById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: CategoryEntity)

    @Delete
    fun delete(entity: CategoryEntity)

}
