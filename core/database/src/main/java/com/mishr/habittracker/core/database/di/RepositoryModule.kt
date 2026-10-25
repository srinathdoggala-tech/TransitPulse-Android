package com.mishr.habittracker.core.database.di

import com.mishr.habittracker.core.database.repository.HabitCompletionRepositoryImpl
import com.mishr.habittracker.core.database.repository.HabitRepositoryImpl
import com.mishr.habittracker.core.database.repository.CategoryRepositoryImpl
import com.mishr.habittracker.core.database.repository.ChecklistItemCompletionRepositoryImpl
import com.mishr.habittracker.domain.repository.HabitCompletionRepository
import com.mishr.habittracker.domain.repository.HabitRepository
import com.mishr.habittracker.domain.repository.CategoryRepository
import com.mishr.habittracker.domain.repository.ChecklistItemCompletionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindHabitRepository(
        habitRepositoryImpl: HabitRepositoryImpl
    ): HabitRepository

    @Binds
    abstract fun bindHabitCompletionRepository(
        habitCompletionRepositoryImpl: HabitCompletionRepositoryImpl
    ): HabitCompletionRepository

    @Binds
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    abstract fun bindChecklistItemCompletionRepository(
        checklistItemCompletionRepositoryImpl: ChecklistItemCompletionRepositoryImpl
    ): ChecklistItemCompletionRepository

    @Binds
    abstract fun bindSyncRepository(
        syncRepositoryImpl: com.mishr.habittracker.core.database.repository.SyncRepositoryImpl
    ): com.mishr.habittracker.domain.repository.SyncRepository
}
