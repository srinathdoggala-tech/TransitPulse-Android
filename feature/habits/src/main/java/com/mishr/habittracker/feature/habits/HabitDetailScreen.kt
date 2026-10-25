package com.mishr.habittracker.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mishr.habittracker.core.designsystem.component.HabitIconSet
import com.mishr.habittracker.domain.model.HabitWithStatus
import com.mishr.habittracker.core.designsystem.component.toStableSet

/**
 * Habit detail screen — coloured header + streak stats + mark done button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitWithStatus: HabitWithStatus,
    categories: List<com.mishr.habittracker.domain.model.Category> = emptyList(),
    onNavigateBack: () -> Unit,
    onToggleCompletion: () -> Unit,
    onUpdateHabit: (com.mishr.habittracker.domain.model.Habit) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onUpdateNotes: (String) -> Unit = {},
    onToggleChecklistItem: (String, Boolean) -> Unit = { _, _ -> },
    onCreateCategory: (String) -> Unit = {}
) {
    val habit = habitWithStatus.habit

    val accentColor = try {
        Color(android.graphics.Color.parseColor(habit.colorTag))
    } catch (e: Exception) {
        Color(0xFF4CAF50)
    }

    val iconVector = HabitIconSet.vectorFor(habit.icon)
    val iconBg    = accentColor.copy(alpha = 0.15f)

    var showEditSheet by remember { mutableStateOf(false) }
    var showTrophySheet by remember { mutableStateOf(false) }
    var showChartSheet by remember { mutableStateOf(false) }
    
    var isEditingNotes by remember { mutableStateOf(false) }
    var notesText by remember(habit.id) { mutableStateOf(habit.notes ?: "") }
    
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val onAccent: Color = if (accentColor.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
        // ── Coloured header ────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    // Back button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Icon circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(6.dp))

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onToggleCompletion()
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (habitWithStatus.isCompletedToday)
                            accentColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (habitWithStatus.isCompletedToday) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = if (habitWithStatus.isCompletedToday) "Completed" else "Not Completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (habitWithStatus.isCompletedToday) accentColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Action buttons row (Trophy, Calendar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SquareActionButton(icon = Icons.Outlined.EmojiEvents, onClick = { showTrophySheet = true })
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Stats row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(label = "Current Streak", value = "${habitWithStatus.currentStreak}", modifier = Modifier.weight(1f))
                        StatCard(label = "Best Streak",    value = "${habitWithStatus.bestStreak}", modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── About this habit ──────────────────────────────────────────────
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "About this habit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = habit.description.takeIf { !it.isNullOrBlank() } ?: "No description provided.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    
                    // Notes Section
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!isEditingNotes) {
                                    TextButton(onClick = { isEditingNotes = true }) {
                                        Text(if (habit.notes.isNullOrBlank()) "Add Note" else "Edit Note")
                                    }
                                }
                            }
                            if (isEditingNotes) {
                                OutlinedTextField(
                                    value = notesText,
                                    onValueChange = { notesText = it },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                                    placeholder = { Text("Write your notes here...") }
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        onUpdateNotes(notesText)
                                        isEditingNotes = false
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Save Note")
                                }
                            } else {
                                Text(
                                    text = habit.notes.takeIf { !it.isNullOrBlank() } ?: "No notes.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Checklist Section
                    if (habit.checklistEnabled && habit.checklistItems.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                    text = "Checklist",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                habit.checklistItems.forEach { item ->
                                    val isCompleted = habitWithStatus.todayChecklistCompletions.contains(item.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleChecklistItem(item.id, !isCompleted) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isCompleted,
                                            onCheckedChange = { onToggleChecklistItem(item.id, it) },
                                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Latest achievement unlocked ───────────────────────────────────
                    val milestones = listOf(1, 10, 30, 50, 100)
                    val latestAchievement = milestones.filter { habitWithStatus.bestStreak >= it }.maxOrNull()
                    if (latestAchievement != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Latest Achievement",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.EmojiEvents,
                                        contentDescription = "Achievements",
                                        tint = accentColor
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(accentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFF1A1A1A)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$latestAchievement Days Streak",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Unlocked!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = accentColor
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Embedded Activity Calendar ────────────────────────────────────
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Activity Calendar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // Reuse the core calendar logic here.
                            HabitCalendarSheetContent(
                                completedDates = habitWithStatus.completedDates,
                                accentColor = accentColor
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
    }

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CreateHabitSheetContent(
                initialHabit = habit,
                categories = categories,
                            onCreateCategory = onCreateCategory,
                onSave = { name, description, scheduleType, days, color, iconKey, reminderEnabled, rHour, rMinute, catId, checkEnabled, checkItems ->
                    val updatedHabit = habit.copy(
                        name = name,
                        description = description,
                        scheduleType = scheduleType,
                        scheduledDays = days,
                        colorTag = color,
                        icon = iconKey,
                        reminderEnabled = reminderEnabled,
                        reminderHour = rHour,
                        reminderMinute = rMinute,
                        categoryId = catId,
                        checklistEnabled = checkEnabled,
                        checklistItems = checkItems.mapIndexed { index, text -> com.mishr.habittracker.domain.model.ChecklistItem(id = java.util.UUID.randomUUID().toString(), text = text, order = index) }
                    )
                    onUpdateHabit(updatedHabit)
                    showEditSheet = false
                },
                onDelete = {
                    onDeleteHabit(habit.id)
                    showEditSheet = false
                },
                onDismiss = { showEditSheet = false }
            )
        }
    }


    if (showTrophySheet) {
        ModalBottomSheet(
            onDismissRequest = { showTrophySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TrophyAchievementsSheetContent(
                bestStreak = habitWithStatus.bestStreak,
                accentColor = accentColor
            )
        }
    }
    
    if (showChartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChartSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Habit Heatmap",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))
                com.mishr.habittracker.core.designsystem.component.HeatmapGrid(completedDates = habitWithStatus.completedDates.toStableSet(), accentColor = accentColor)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ── Square Action Button ────────────────────────────────────────────────────────
@Composable
private fun SquareActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── Stat card ──────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ── Action button ──────────────────────────────────────────────────────────────
@Composable
private fun ActionButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor   = contentColor
        ),
        shape  = RoundedCornerShape(12.dp),
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

