package com.mishr.habittracker.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferencesDataStore(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LANGUAGE_CODE_KEY = stringPreferencesKey("language_code")
        val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        val IS_GUEST_MIGRATION_PENDING = booleanPreferencesKey("is_guest_migration_pending")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeName = preferences[THEME_MODE_KEY]
            try {
                if (themeName != null) ThemeMode.valueOf(themeName) else ThemeMode.SYSTEM
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[THEME_MODE_KEY] = mode.name
            }
            AppResult.Success(Unit)
        } catch (e: IOException) {
            AppResult.Error(AppError.DatabaseError("Failed to save theme mode: ${e.message}"))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    val languageCode: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[LANGUAGE_CODE_KEY] ?: "en"
        }

    suspend fun setLanguageCode(code: String): AppResult<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[LANGUAGE_CODE_KEY] = code
            }
            AppResult.Success(Unit)
        } catch (e: IOException) {
            AppResult.Error(AppError.DatabaseError("Failed to save language code: ${e.message}"))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    val hasSeenOnboarding: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
        }

    suspend fun setHasSeenOnboarding(hasSeen: Boolean): AppResult<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[HAS_SEEN_ONBOARDING_KEY] = hasSeen
            }
            AppResult.Success(Unit)
        } catch (e: IOException) {
            AppResult.Error(AppError.DatabaseError("Failed to save onboarding preference: ${e.message}"))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    val isGuestMigrationPending: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_GUEST_MIGRATION_PENDING] ?: false
        }

    suspend fun setGuestMigrationPending(isPending: Boolean): AppResult<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[IS_GUEST_MIGRATION_PENDING] = isPending
            }
            AppResult.Success(Unit)
        } catch (e: IOException) {
            AppResult.Error(AppError.DatabaseError("Failed to save migration pending preference: ${e.message}"))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e))
        }
    }
}
