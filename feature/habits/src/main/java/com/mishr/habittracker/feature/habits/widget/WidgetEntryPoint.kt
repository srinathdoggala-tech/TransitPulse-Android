package com.mishr.habittracker.feature.habits.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.mishr.habittracker.domain.usecase.GetHabitsWithStatusUseCase
import com.mishr.habittracker.domain.usecase.ToggleHabitCompletionUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getHabitsWithStatusUseCase(): GetHabitsWithStatusUseCase
    fun getToggleHabitCompletionUseCase(): ToggleHabitCompletionUseCase
}
