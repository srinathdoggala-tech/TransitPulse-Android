package com.mishr.habittracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mishr.habittracker.core.datastore.UserPreferencesDataStore
import com.mishr.habittracker.domain.repository.CategoryRepository
import com.mishr.habittracker.domain.repository.SyncRepository
import com.mishr.habittracker.domain.usecase.ObserveAuthStateUseCase
import com.mishr.habittracker.domain.usecase.SignOutUseCase
import com.mishr.habittracker.domain.usecase.DeleteAccountUseCase
import com.mishr.habittracker.domain.usecase.SignInUseCase
import com.mishr.habittracker.domain.exception.RequiresRecentLoginException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
    private val categoryRepository: CategoryRepository,
    private val syncRepository: SyncRepository,
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val signInUseCase: SignInUseCase
) : ViewModel() {

    private val _effect = MutableSharedFlow<SettingsEffect>()
    val effect = _effect.asSharedFlow()

    val state: StateFlow<SettingsState> = combine(
        dataStore.themeMode,
        dataStore.languageCode,
        categoryRepository.getAllCategories(),
        observeAuthStateUseCase(),
        syncRepository.getPendingSyncCount()
    ) { args ->
        val theme = args[0] as com.mishr.habittracker.domain.model.ThemeMode
        val lang = args[1] as String
        val categories = args[2] as List<com.mishr.habittracker.domain.model.Category>
        val auth = args[3] as com.mishr.habittracker.domain.model.AuthState
        val pendingSync = args[4] as Int
        
        SettingsState(
            themeMode = theme,
            languageCode = lang,
            categories = categories,
            authState = auth,
            pendingSyncCount = pendingSync
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetThemeMode -> {
                viewModelScope.launch {
                    dataStore.setThemeMode(intent.mode)
                }
            }
            is SettingsIntent.SetLanguage -> {
                viewModelScope.launch {
                    dataStore.setLanguageCode(intent.code)
                }
            }
            is SettingsIntent.DeleteCategory -> {
                viewModelScope.launch {
                    categoryRepository.deleteCategory(intent.id)
                }
            }
            is SettingsIntent.SignOut -> {
                viewModelScope.launch {
                    signOutUseCase()
                    dataStore.setGuestMigrationPending(false)
                }
            }
            is SettingsIntent.ReauthenticateAndDelete -> {
                viewModelScope.launch {
                    val signInResult = signInUseCase(intent.idToken)
                    if (signInResult.isSuccess) {
                        val delResult = deleteAccountUseCase()
                        if (delResult.isSuccess) {
                            dataStore.setGuestMigrationPending(false)
                        } else {
                            _effect.emit(SettingsEffect.ShowError(delResult.exceptionOrNull()?.message ?: "Failed to delete account"))
                        }
                    } else {
                        _effect.emit(SettingsEffect.ShowError("Re-authentication failed"))
                    }
                }
            }
            is SettingsIntent.DeleteAccount -> {
                viewModelScope.launch {
                    val result = deleteAccountUseCase()
                    if (result.isFailure && result.exceptionOrNull() is RequiresRecentLoginException) {
                        _effect.emit(SettingsEffect.RequiresReauthForDeletion)
                        return@launch
                    }
                    if (result.isSuccess) {
                        dataStore.setGuestMigrationPending(false)
                    } else {
                        _effect.emit(SettingsEffect.ShowError(result.exceptionOrNull()?.message ?: "Failed to delete account"))
                    }
                }
            }
        }
    }
}
