package com.mishr.habittracker.core.datastore.repository

import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.core.datastore.UserPreferencesDataStore
import com.mishr.habittracker.domain.model.ThemeMode
import com.mishr.habittracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of UserPreferencesRepository backed by DataStore.
 */
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : UserPreferencesRepository {

    override fun getThemeMode(): Flow<ThemeMode> = dataStore.themeMode

    override suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit> {
        return dataStore.setThemeMode(mode)
    }

    override fun getLanguage(): Flow<String> = dataStore.languageCode

    override suspend fun setLanguage(languageCode: String): AppResult<Unit> {
        return dataStore.setLanguageCode(languageCode)
    }
}
