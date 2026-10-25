package com.mishr.habittracker.domain.repository

import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user preferences.
 */
interface UserPreferencesRepository {
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit>
    fun getLanguage(): Flow<String>
    suspend fun setLanguage(languageCode: String): AppResult<Unit>
}
