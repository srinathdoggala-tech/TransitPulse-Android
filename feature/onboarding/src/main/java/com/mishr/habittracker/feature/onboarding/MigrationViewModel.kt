package com.mishr.habittracker.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mishr.habittracker.core.datastore.UserPreferencesDataStore
import com.mishr.habittracker.domain.model.MigrationStatus
import com.mishr.habittracker.domain.usecase.MigrateGuestDataUseCase
import com.mishr.habittracker.domain.usecase.ObserveAuthStateUseCase
import com.mishr.habittracker.domain.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val migrateGuestDataUseCase: MigrateGuestDataUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _migrationStatus = MutableStateFlow<MigrationStatus>(MigrationStatus.NotStarted)
    val migrationStatus: StateFlow<MigrationStatus> = _migrationStatus.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().first { state ->
                if (state is AuthState.Authenticated) {
                    currentUserId = state.userId
                    startMigration(state.userId)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun startMigration(userId: String) {
        viewModelScope.launch {
            migrateGuestDataUseCase(userId).collect { status ->
                _migrationStatus.value = status
                if (status is MigrationStatus.Completed) {
                    userPreferencesDataStore.setGuestMigrationPending(false)
                }
            }
        }
    }

    fun retry() {
        currentUserId?.let {
            startMigration(it)
        }
    }
}
