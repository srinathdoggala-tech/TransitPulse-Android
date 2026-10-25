package com.mishr.habittracker.feature.settings

import com.mishr.habittracker.domain.model.Category
import com.mishr.habittracker.domain.model.ThemeMode
import com.mishr.habittracker.domain.model.AuthState

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageCode: String = "en",
    val categories: List<Category> = emptyList(),
    val authState: AuthState = AuthState.Loading,
    val pendingSyncCount: Int = 0
)

sealed class SettingsIntent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsIntent()
    data class SetLanguage(val code: String) : SettingsIntent()
    data class DeleteCategory(val id: String) : SettingsIntent()
    object SignOut : SettingsIntent()
    object DeleteAccount : SettingsIntent()
    data class ReauthenticateAndDelete(val idToken: String) : SettingsIntent()
}

sealed class SettingsEffect {
    object NavigateToOnboarding : SettingsEffect()
    data class ShowError(val message: String) : SettingsEffect()
    object RequiresReauthForDeletion : SettingsEffect()
}
