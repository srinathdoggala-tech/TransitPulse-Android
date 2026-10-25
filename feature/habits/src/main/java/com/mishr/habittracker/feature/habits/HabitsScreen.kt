package com.mishr.habittracker.feature.habits

import android.os.Build
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mishr.habittracker.core.designsystem.component.DateSelectorRow
import com.mishr.habittracker.core.designsystem.component.HabitCard
import com.mishr.habittracker.core.designsystem.component.toStableSet
import com.mishr.habittracker.core.designsystem.theme.BackgroundLight
import com.mishr.habittracker.core.designsystem.theme.HabitYellow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    // ── Permission request ────────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Error effects ─────────────────────────────────────────────────────────
    LaunchedEffect(viewModel.effects) {
                viewModel.effects.collect { effect ->
            when (effect) {
                is HabitsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is HabitsEffect.RequestExactAlarmPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                }
            }
        }
    }

    // ── Date strip ────────────────────────────────────────────────────────────
    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val dateFormatter = DateTimeFormatter.ofPattern("d")
    // Show Mon–today (up to 7 days ending at today)
    val stripDays = (-6..0).map { offset ->
        val d = today.plusDays(offset.toLong())
        Pair(d.format(dayFormatter), d.format(dateFormatter))
    }
    var selectedDayIndex by remember { mutableIntStateOf(6) } // today = last

    // ── Filter state ──────────────────────────────────────────────────────────
    var showOnlyDue by remember { mutableStateOf(false) }

    val filteredHabits = remember(state.habits, showOnlyDue, state.selectedCategoryId) {
        val baseList = if (state.selectedCategoryId != null) {
            state.habits.filter { it.habit.categoryId == state.selectedCategoryId }
        } else {
            state.habits
        }
        if (showOnlyDue) {
            val todayDomainDayOfWeek = com.mishr.habittracker.domain.model.DayOfWeek.valueOf(today.dayOfWeek.name)
            baseList.filter { 
                !it.isCompletedToday && (
                    it.habit.scheduleType == com.mishr.habittracker.domain.model.ScheduleType.DAILY || 
                    it.habit.scheduledDays.contains(todayDomainDayOfWeek)
                )
            }
        }
        else baseList
    }



    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.processIntent(HabitsIntent.CreateCategory(newCategoryName, "#FF6B6B"))
                        }
                        showAddCategoryDialog = false
                        newCategoryName = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.processIntent(HabitsIntent.OpenCreateSheet) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { paddingValues ->
        @OptIn(ExperimentalMaterial3Api::class)
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.processIntent(HabitsIntent.Refresh) },
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val dateLabel = today.format(DateTimeFormatter.ofPattern("d MMM"))
                    Text(
                        text = "Today, $dateLabel",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.processIntent(HabitsIntent.ToggleThemeMode) }) {
                        Icon(
                            imageVector = if (state.themeMode == com.mishr.habittracker.domain.model.ThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                                        IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Date selector strip ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                DateSelectorRow(
                    days = stripDays,
                    selectedIndex = selectedDayIndex,
                    accentColor = HabitYellow,
                    onDaySelected = { selectedDayIndex = it }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Filter chips + habit score ────────────────────────────────────
                        androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChipButton(
                        label = "All Categories",
                        selected = state.selectedCategoryId == null,
                        onClick = { viewModel.processIntent(HabitsIntent.SetCategoryFilter(null)) }
                    )
                }
                item {
                    FilterChipButton(
                        label = "+ Add Category",
                        selected = false,
                        onClick = { showAddCategoryDialog = true }
                    )
                }
                items(state.categories) { cat ->
                    FilterChipButton(
                        label = cat.name,
                        selected = state.selectedCategoryId == cat.id,
                        onClick = { viewModel.processIntent(HabitsIntent.SetCategoryFilter(cat.id)) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChipButton(
                    label = "All (${state.habits.size})",
                    selected = !showOnlyDue,
                    onClick = { showOnlyDue = false }
                )
                Spacer(Modifier.width(8.dp))
                FilterChipButton(
                    label = "Due (${state.habits.count { !it.isCompletedToday && (it.habit.scheduleType == com.mishr.habittracker.domain.model.ScheduleType.DAILY || it.habit.scheduledDays.contains(com.mishr.habittracker.domain.model.DayOfWeek.valueOf(today.dayOfWeek.name))) }})",
                    selected = showOnlyDue,
                    onClick = { showOnlyDue = true }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Habit list ────────────────────────────────────────────────────
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HabitYellow)
                }
            } else if (state.habits.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No habits yet.\nTap + to create one!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredHabits, key = { it.habit.id }) { habitWithStatus ->
                        val iconKey = habitWithStatus.habit.icon
                        HabitCard(
                            name = habitWithStatus.habit.name,
                            streak = habitWithStatus.currentStreak,
                            isCompletedToday = habitWithStatus.isCompletedToday,
                            completedDates = habitWithStatus.completedDates.toStableSet(),
                            colorTag = habitWithStatus.habit.colorTag,
                            onToggleCompletion = {
                                viewModel.processIntent(
                                    HabitsIntent.ToggleCompletion(habitWithStatus.habit.id)
                                )
                            },
                            iconKey = iconKey,
                            onClick = { onNavigateToDetail(habitWithStatus.habit.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }
        }

        // ── Create Habit Sheet ────────────────────────────────────────────────
        if (state.showCreateSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.processIntent(HabitsIntent.DismissCreateSheet) },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CreateHabitSheetContent(
                    initialHabit = null,
                    categories = state.categories,
                    onCreateCategory = { viewModel.processIntent(HabitsIntent.CreateCategory(it, "#FF6B6B")) },
                    onSave = { name, description, scheduleType, days, color, iconKey, reminderEnabled, rHour, rMinute, catId, checkEnabled, checkItems ->
                        viewModel.processIntent(
                            HabitsIntent.CreateHabit(
                                name = name,
                                description = description,
                                scheduleType = scheduleType,
                                scheduledDays = days,
                                colorTag = color,
                                iconKey = iconKey,
                                reminderEnabled = reminderEnabled,
                                reminderHour = rHour,
                                reminderMinute = rMinute,
                                categoryId = catId,
                                checklistEnabled = checkEnabled,
                                checklistItems = checkItems
                            )
                        )
                    },
                    onDismiss = {
                        viewModel.processIntent(HabitsIntent.DismissCreateSheet)
                    }
                )
            }
        }
    }
}

// ── Filter chip ────────────────────────────────────────────────────────────────
@Composable
private fun FilterChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) HabitYellow else MaterialTheme.colorScheme.surface,
        shadowElevation = if (selected) 0.dp else 1.dp,
        onClick = onClick
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}


