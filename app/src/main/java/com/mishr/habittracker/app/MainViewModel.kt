package com.mishr.habittracker.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mishr.habittracker.core.datastore.UserPreferencesDataStore
import com.mishr.habittracker.domain.model.AuthState
import com.mishr.habittracker.domain.model.ThemeMode
import com.mishr.habittracker.domain.usecase.ObserveAuthStateUseCase
import com.mishr.habittracker.domain.usecase.StartCloudSyncUseCase
import com.mishr.habittracker.domain.usecase.StopCloudSyncUseCase
import com.mishr.habittracker.domain.sync.SyncTrigger
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    dataStore: UserPreferencesDataStore,
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    startCloudSync: StartCloudSyncUseCase,
    stopCloudSync: StopCloudSyncUseCase,
    syncTrigger: SyncTrigger
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = dataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    val hasSeenOnboarding: StateFlow<Boolean?> = dataStore.hasSeenOnboarding.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val isGuestMigrationPending: StateFlow<Boolean> = dataStore.isGuestMigrationPending.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val authState: StateFlow<AuthState> = observeAuthStateUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthState.Loading
    )

    init {
        viewModelScope.launch {
            authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        startCloudSync(state.userId)
                        syncTrigger.schedulePeriodicSync()
                    }
                    is AuthState.Unauthenticated -> stopCloudSync()
                    else -> {}
                }
            }
        }
    }
}

