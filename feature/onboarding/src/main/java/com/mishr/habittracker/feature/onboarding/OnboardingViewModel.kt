package com.mishr.habittracker.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mishr.habittracker.core.datastore.UserPreferencesDataStore
import com.mishr.habittracker.domain.model.AuthState
import com.mishr.habittracker.domain.repository.HabitRepository
import com.mishr.habittracker.domain.repository.MigrationRepository
import com.mishr.habittracker.domain.usecase.ObserveAuthStateUseCase
import com.mishr.habittracker.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val habitRepository: HabitRepository,
    private val migrationRepository: MigrationRepository,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<OnboardingEffect>()
    val effect = _effect.asSharedFlow()

    fun handleIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.ContinueAsGuest -> continueAsGuest()
            is OnboardingIntent.SignInWithGoogle -> signIn(intent.idToken)
            is OnboardingIntent.SignInFailed -> {
                _state.update { it.copy(isLoading = false, error = intent.error) }
            }
        }
    }

    private fun continueAsGuest() {
        viewModelScope.launch {
            userPreferencesDataStore.setHasSeenOnboarding(true)
            _effect.emit(OnboardingEffect.NavigateToHome)
        }
    }

    private fun signIn(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Snapshot local state BEFORE Firebase Auth changes anything.
            val localHabits = habitRepository.getAllHabits().first()
            val hasLocalGuestData = localHabits.isNotEmpty()

            val result = signInUseCase(idToken)
            result.onSuccess {
                userPreferencesDataStore.setHasSeenOnboarding(true)

                // Resolve the userId from the auth state that Firebase just emitted.
                val authState = observeAuthStateUseCase()
                    .filterIsInstance<AuthState.Authenticated>()
                    .first()
                val userId = authState.userId

                if (hasLocalGuestData) {
                    userPreferencesDataStore.setGuestMigrationPending(true)
                    _state.update { it.copy(isLoading = false) }
                    _effect.emit(OnboardingEffect.NavigateToMigration)
                } else {
                    migrationRepository.pullCloudDataToLocal(userId)
                    _state.update { it.copy(isLoading = false) }
                    _effect.emit(OnboardingEffect.NavigateToHome)
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message ?: "Sign in failed") }
            }
        }
    }
}
