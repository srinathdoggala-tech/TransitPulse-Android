package com.mishr.habittracker.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.domain.usecase.CreateHabitUseCase
import com.mishr.habittracker.domain.usecase.GetHabitsWithStatusUseCase
import com.mishr.habittracker.domain.usecase.ToggleHabitCompletionUseCase
import com.mishr.habittracker.domain.usecase.CreateCategoryUseCase
import com.mishr.habittracker.domain.usecase.UpdateHabitNotesUseCase
import com.mishr.habittracker.domain.usecase.ToggleChecklistItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import com.mishr.habittracker.domain.repository.SyncRepository
import com.mishr.habittracker.domain.repository.MigrationRepository
import com.mishr.habittracker.domain.usecase.ObserveAuthStateUseCase
import com.mishr.habittracker.domain.sync.SyncTrigger
import kotlinx.coroutines.flow.first
import com.mishr.habittracker.domain.model.AuthState
import kotlinx.coroutines.flow.filterIsInstance
import com.mishr.habittracker.domain.repository.CategoryRepository
import com.mishr.habittracker.domain.repository.HabitRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.app.AlarmManager
import android.os.Build
import androidx.glance.appwidget.updateAll


import com.mishr.habittracker.domain.repository.UserPreferencesRepository

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val getHabitsWithStatusUseCase: GetHabitsWithStatusUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val createHabitUseCase: CreateHabitUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateHabitNotesUseCase: UpdateHabitNotesUseCase,
    private val toggleChecklistItemUseCase: ToggleChecklistItemUseCase,
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val scheduleReminderUseCase: com.mishr.habittracker.domain.usecase.ScheduleReminderUseCase,
    private val cancelReminderUseCase: com.mishr.habittracker.domain.usecase.CancelReminderUseCase,
    private val syncRepository: SyncRepository,
    private val migrationRepository: MigrationRepository,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val syncTrigger: SyncTrigger,
    private val startCloudSyncUseCase: com.mishr.habittracker.domain.usecase.StartCloudSyncUseCase,
    private val stopCloudSyncUseCase: com.mishr.habittracker.domain.usecase.StopCloudSyncUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HabitsState())
    val state: StateFlow<HabitsState> = _state.asStateFlow()

    private val _effects = Channel<HabitsEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        processIntent(HabitsIntent.LoadHabits)
        
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categoriesList ->
                _state.update { it.copy(categories = categoriesList) }
            }
        }
        
        viewModelScope.launch {
            userPreferencesRepository.getThemeMode().collect { theme ->
                _state.update { it.copy(themeMode = theme) }
            }
        }
    }

    fun processIntent(intent: HabitsIntent) {
        when (intent) {
            is HabitsIntent.LoadHabits -> loadHabits()
            is HabitsIntent.Refresh -> handleRefresh()
            is HabitsIntent.ToggleCompletion -> toggleCompletion(intent.habitId)
            is HabitsIntent.ToggleChecklistItem -> toggleChecklistItem(intent.habitId, intent.checklistItemId, intent.isCompleted)
            is HabitsIntent.OpenCreateSheet -> _state.update { it.copy(showCreateSheet = true) }
            is HabitsIntent.DismissCreateSheet -> _state.update { it.copy(showCreateSheet = false) }
            is HabitsIntent.CreateHabit -> createHabit(intent)
            is HabitsIntent.UpdateHabit -> updateHabit(intent.habit)
            is HabitsIntent.UpdateHabitNotes -> updateHabitNotes(intent.habitId, intent.notes)
            is HabitsIntent.DeleteHabit -> deleteHabit(intent.id)
            is HabitsIntent.ToggleThemeMode -> toggleTheme()
            is HabitsIntent.SetCategoryFilter -> _state.update { it.copy(selectedCategoryId = intent.categoryId) }
            is HabitsIntent.CreateCategory -> createCategory(intent.name, intent.colorTag)
        }
    }
    
    private fun toggleTheme() {
        viewModelScope.launch {
            val currentMode = _state.value.themeMode
            val newMode = if (currentMode == com.mishr.habittracker.domain.model.ThemeMode.DARK) {
                com.mishr.habittracker.domain.model.ThemeMode.LIGHT
            } else {
                com.mishr.habittracker.domain.model.ThemeMode.DARK
            }
            userPreferencesRepository.setThemeMode(newMode)
        }
    }

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context); val componentName = android.content.ComponentName(context, com.mishr.habittracker.feature.habits.widget.HabitsWidgetReceiver::class.java); val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName); appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, com.mishr.habittracker.feature.habits.R.id.widget_list_view)
            } catch (e: Exception) {
            }
        }
    }

    private fun loadHabits() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                getHabitsWithStatusUseCase().collect { habitsList ->
                    _state.update { 
                        it.copy(
                            habits = habitsList,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = AppError.UnknownError(e)
                    ) 
                }
                _effects.send(HabitsEffect.ShowError("Failed to load habits"))
            }
        }
    }

    private fun toggleCompletion(habitId: String) {
        viewModelScope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val result = toggleHabitCompletionUseCase(habitId, today)
            if (result is AppResult.Error) {
                _effects.send(HabitsEffect.ShowError("Failed to update habit status"))
            } else {
                updateWidget()
            }
        }
    }
    
    private fun toggleChecklistItem(habitId: String, checklistItemId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val result = toggleChecklistItemUseCase(habitId, checklistItemId, isCompleted)
            if (result is AppResult.Error) {
                _effects.send(HabitsEffect.ShowError("Failed to update checklist item"))
            } else {
                updateWidget()
            }
        }
    }

        private fun createCategory(name: String, colorTag: String) {
        viewModelScope.launch {
            when (val result = createCategoryUseCase(name, colorTag)) {
                is AppResult.Error -> {
                    val msg = (result.error as? com.mishr.habittracker.domain.result.AppError.UnknownError)?.throwable?.message ?: "Failed to create category"
                    _effects.send(HabitsEffect.ShowError(msg))
                }
                else -> {}
            }
        }
    }

    private fun createHabit(intent: HabitsIntent.CreateHabit) {
        viewModelScope.launch {
            val result = createHabitUseCase(
                name = intent.name,
                description = intent.description,
                scheduleType = intent.scheduleType,
                scheduledDays = intent.scheduledDays,
                colorTag = intent.colorTag,
                iconKey = intent.iconKey,
                reminderEnabled = intent.reminderEnabled,
                reminderHour = intent.reminderHour,
                reminderMinute = intent.reminderMinute,
                categoryId = intent.categoryId,
                checklistEnabled = intent.checklistEnabled,
                checklistItems = intent.checklistItems
            )
            if (intent.reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    _effects.send(HabitsEffect.RequestExactAlarmPermission)
                }
            }
            
            when (result) {
                is AppResult.Success -> {
                    _state.update { it.copy(showCreateSheet = false) }
                    updateWidget()
                }
                is AppResult.Error -> {
                    val msg = when (val err = result.error) {
                        is AppError.UnknownError -> err.throwable?.message
                        is AppError.DatabaseError -> err.message
                        is AppError.NetworkError -> err.message
                        is AppError.AuthError -> err.message
                        is AppError.MigrationError -> err.message
                    } ?: "Failed to create habit"
                    _effects.send(HabitsEffect.ShowError(msg))
                }
            }
        }
    }

    private fun updateHabit(habit: com.mishr.habittracker.domain.model.Habit) {
        viewModelScope.launch {
            when (val result = habitRepository.updateHabit(habit)) {
                is AppResult.Success -> {
                    _state.update { it.copy(showCreateSheet = false) }
                    updateWidget()
                    if (habit.reminderEnabled && habit.reminderHour != null && habit.reminderMinute != null) {
                        habit.reminderHour?.let { h -> habit.reminderMinute?.let { m -> scheduleReminderUseCase(habit.id, h, m) } }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            if (!alarmManager.canScheduleExactAlarms()) {
                                _effects.send(HabitsEffect.RequestExactAlarmPermission)
                            }
                        }
                    } else {
                        cancelReminderUseCase(habit.id)
                    }
                }
                is AppResult.Error -> {
                    _effects.send(HabitsEffect.ShowError("Failed to update habit"))
                }
            }
        }
    }

    private fun updateHabitNotes(habitId: String, notes: String?) {
        viewModelScope.launch {
            when (updateHabitNotesUseCase(habitId, notes)) {
                is AppResult.Error -> _effects.send(HabitsEffect.ShowError("Failed to save notes"))
                else -> {}
            }
        }
    }

    private fun deleteHabit(id: String) {
        viewModelScope.launch {
            when (habitRepository.deleteHabit(id)) {
                is AppResult.Success -> {
                    _state.update { it.copy(showCreateSheet = false) }
                    updateWidget()
                    cancelReminderUseCase(id)
                }
                is AppResult.Error -> {
                    _effects.send(HabitsEffect.ShowError("Failed to delete habit"))
                }
            }
        }
    }

    private fun handleRefresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                val authState = observeAuthStateUseCase().first { it !is AuthState.Loading }
                if (authState is AuthState.Authenticated) {
                    val pendingCount = syncRepository.getPendingSyncCount().first()
                    if (pendingCount > 0) {
                        _effects.send(HabitsEffect.ShowError("Syncing local changes first. Please wait..."))
                        syncTrigger.scheduleSync()
                    } else {
                        // Restart snapshot listeners to pull fresh data without deleting local tables
                        stopCloudSyncUseCase()
                        startCloudSyncUseCase(authState.userId)
                    }
                } else {
                    _effects.send(HabitsEffect.ShowError("Not signed in"))
                }
            } catch (e: Exception) {
                _effects.send(HabitsEffect.ShowError(e.message ?: "Unknown error"))
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

}


