package com.mishr.habittracker.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.mishr.habittracker.core.designsystem.component.HabitIconSet
import com.mishr.habittracker.core.designsystem.theme.HabitPalette
import com.mishr.habittracker.domain.model.DayOfWeek
import com.mishr.habittracker.domain.model.ScheduleType

/**
 * Redesigned Add Habit sheet — minimalist, card-based, matching the reference screenshots.
 *
  * the icon selection is UI-layer only. The remaining params map 1-to-1 with existing
 * domain model fields, so the data layer is untouched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitSheetContent(
    initialHabit: com.mishr.habittracker.domain.model.Habit? = null,
    categories: List<com.mishr.habittracker.domain.model.Category> = emptyList(),
    onCreateCategory: ((String) -> Unit)? = null,
    onSave: (
        name: String,
        description: String,
        scheduleType: ScheduleType,
        scheduledDays: Set<DayOfWeek>,
        colorTag: String,
        iconKey: String,
        reminderEnabled: Boolean,
        reminderHour: Int?,
        reminderMinute: Int?,
        categoryId: String?,
        checklistEnabled: Boolean,
        checklistItems: List<String>
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var name           by remember(initialHabit?.id) { mutableStateOf(initialHabit?.name ?: "") }
    var description    by remember(initialHabit?.id) { mutableStateOf(initialHabit?.description ?: "") }
    var scheduleType   by remember(initialHabit?.id) { mutableStateOf(initialHabit?.scheduleType ?: ScheduleType.DAILY) }
    var selectedDays   by remember(initialHabit?.id) { mutableStateOf(initialHabit?.scheduledDays ?: setOf<DayOfWeek>()) }
    var selectedColor  by remember(initialHabit?.id) { 
        mutableStateOf(initialHabit?.colorTag?.let { tag ->
            try { Color(android.graphics.Color.parseColor(tag)) } catch (e: Exception) { HabitPalette.first() }
        } ?: HabitPalette.first()) 
    }
    var selectedIcon   by remember(initialHabit?.id) { mutableStateOf(initialHabit?.icon ?: HabitIconSet.all.first().key) }
    var reminderOn     by remember(initialHabit?.id) { mutableStateOf(initialHabit?.reminderEnabled ?: false) }
    var reminderHourStr   by remember(initialHabit?.id) { 
        mutableStateOf(initialHabit?.reminderHour?.let { h -> 
            if (h == 0) "12" else if (h > 12) (h - 12).toString() else h.toString()
        } ?: "") 
    }
    var reminderMinuteStr by remember(initialHabit?.id) { mutableStateOf(initialHabit?.reminderMinute?.let { String.format("%02d", it) } ?: "") }
    var reminderAmPm by remember(initialHabit?.id) { mutableStateOf(if ((initialHabit?.reminderHour ?: 0) >= 12) "PM" else "AM") }
    var showColorPicker  by remember { mutableStateOf(false) }
    var showIconPicker   by remember { mutableStateOf(false) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank() && onCreateCategory != null) {
                            onCreateCategory(newCategoryName)
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
    var selectedCategoryId by remember(initialHabit?.id) { mutableStateOf(initialHabit?.categoryId) }
    var checklistEnabled   by remember(initialHabit?.id) { mutableStateOf(initialHabit?.checklistEnabled ?: false) }
    val checklistItems = remember(initialHabit?.id) { 
        androidx.compose.runtime.mutableStateListOf<String>().apply { 
            initialHabit?.checklistItems?.sortedBy { it.order }?.forEach { add(it.text) } 
        } 
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        // Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 16.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline)
        )

        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (initialHabit == null) "Add Habit" else "Edit Habit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = {
                    val h12 = reminderHourStr.toIntOrNull()
                val m = reminderMinuteStr.toIntOrNull()
                val h24 = if (h12 != null) {
                    if (reminderAmPm == "AM") {
                        if (h12 == 12) 0 else h12
                    } else {
                        if (h12 == 12) 12 else h12 + 12
                    }
                } else null
                
                val rawColor = android.graphics.Color.argb(
                    (selectedColor.alpha * 255).toInt(),
                    (selectedColor.red   * 255).toInt(),
                    (selectedColor.green * 255).toInt(),
                    (selectedColor.blue  * 255).toInt()
                )
                val colorHex = "#%08X".format(rawColor)
                    onSave(name, description, scheduleType, selectedDays, colorHex, selectedIcon, reminderOn, h24, m, selectedCategoryId, checklistEnabled, checklistItems.toList())
                },
                enabled = name.isNotBlank() &&
                    (scheduleType == ScheduleType.DAILY || selectedDays.isNotEmpty()) &&
                    (!reminderOn || (reminderHourStr.toIntOrNull() != null && reminderMinuteStr.toIntOrNull() != null))
            ) {
                Text(
                    "Save ✓",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Habit title field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
            placeholder = { Text("Enter Habit Title") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        // Description field
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
            placeholder = { Text("Enter Description (optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        // Colour + Icon picker row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Colour picker trigger
            PickerCard(
                modifier = Modifier.weight(1f),
                onClick = { showColorPicker = !showColorPicker; showIconPicker = false }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                )
                Spacer(Modifier.width(10.dp))
                Text("Color", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }

            // Icon picker trigger
            PickerCard(
                modifier = Modifier.weight(1f),
                onClick = { showIconPicker = !showIconPicker; showColorPicker = false }
            ) {
                Icon(
                    imageVector = HabitIconSet.vectorFor(selectedIcon),
                    contentDescription = "Selected Icon",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("Icon", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }

        // Colour grid
        if (showColorPicker) {
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(HabitPalette) { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (color == selectedColor)
                                    Modifier.border(3.dp, Color(0xFF1A1A1A), CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColor = color; showColorPicker = false }
                    )
                }
            }
        }

        // Icon grid
        if (showIconPicker) {
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = true
            ) {
                items(HabitIconSet.all) { icon ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (icon.key == selectedIcon)
                                    selectedColor.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (icon.key == selectedIcon) 2.dp else 0.dp,
                                color = if (icon.key == selectedIcon) selectedColor else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedIcon = icon.key; showIconPicker = false }
                    ) {
                        Icon(
                            imageVector = icon.vector,
                            contentDescription = icon.key,
                            tint = if (icon.key == selectedIcon) selectedColor else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Frequency section
        SectionCard(title = "Frequency") {
            FrequencyOption("Everyday", scheduleType == ScheduleType.DAILY) {
                scheduleType = ScheduleType.DAILY
                selectedDays = emptySet()
            }
            FrequencyOption("Specific Days of Week", scheduleType == ScheduleType.WEEKLY) {
                scheduleType = ScheduleType.WEEKLY
            }
        }

        // Day-of-week picker (shown when WEEKLY)
        if (scheduleType == ScheduleType.WEEKLY) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DayOfWeek.values().forEach { day ->
                    val isSelected = selectedDays.contains(day)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                            }
                    ) {
                        Text(
                            text = day.name.take(1),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Reminder toggle
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set Reminder", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = reminderOn, onCheckedChange = { reminderOn = it })
            }
        }

        if (reminderOn) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = reminderHourStr,
                    onValueChange = { if (it.length <= 2) reminderHourStr = it },
                    label = { Text("Hour (1-12)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reminderMinuteStr,
                    onValueChange = { if (it.length <= 2) reminderMinuteStr = it },
                    label = { Text("Minute (0-59)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                // AM/PM Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(56.dp).padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (reminderAmPm == "AM") MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (reminderAmPm == "AM") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { reminderAmPm = "AM" },
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                                Text("AM", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (reminderAmPm == "PM") MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (reminderAmPm == "PM") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { reminderAmPm = "PM" },
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                                Text("PM", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Category Picker
        Spacer(Modifier.height(16.dp))
        Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text("None") }
                )
            }
            if (onCreateCategory != null) {
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showAddCategoryDialog = true },
                        label = { Text("+ New Category") }
                    )
                }
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = { selectedCategoryId = cat.id },
                    label = { Text(cat.name) }
                )
            }
        }

        // Checklist Section
        Spacer(Modifier.height(16.dp))
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Checklist", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = checklistEnabled,
                    onCheckedChange = { checklistEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = selectedColor, checkedTrackColor = selectedColor.copy(alpha = 0.5f))
                )
            }
        }

        if (checklistEnabled) {
            checklistItems.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = item,
                        onValueChange = { checklistItems[index] = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Item ${index + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(onClick = { checklistItems.removeAt(index) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Remove")
                    }
                }
            }
            TextButton(
                onClick = { checklistItems.add("") },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add Item", color = selectedColor)
            }
        }

                if (initialHabit != null && onDelete != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Habit", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Small helpers ──────────────────────────────────────────────────────────────

@Composable
private fun PickerCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SectionCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun FrequencyOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

