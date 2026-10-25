package com.mishr.habittracker.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mishr.habittracker.core.designsystem.theme.HeatmapEmpty
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Full-width habit card matching the new minimalist design.
 *
 * Shows:
 *  - Coloured circular icon with emoji
 *  - Habit name + streak subtitle
 *  - Completion toggle button
 *  - 10-week heatmap calendar (70 squares, 7 columns)
 *
 * @param completedDates  Set of "yyyy-MM-dd" strings that are completed.
 *                        The grid shows the most recent 70 days.
 * @param iconKey         Key from [HabitIconSet] — stored per-session in UI state.
 */
@Composable
fun HabitCard(
    name: String,
    streak: Int,
    isCompletedToday: Boolean,
    colorTag: String,
    onToggleCompletion: () -> Unit,
    modifier: Modifier = Modifier,
    completedDates: StableSet<kotlinx.datetime.LocalDate> = emptySet<kotlinx.datetime.LocalDate>().toStableSet(),
    iconKey: String = "star",
    onClick: (() -> Unit)? = null
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(colorTag))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val iconVector = HabitIconSet.vectorFor(iconKey)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) 
        accentColor.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.surface) 
    else 
        accentColor.copy(alpha = 0.1f)
        
    val iconBg = if (isDark) accentColor.copy(alpha = 0.15f) else Color.Transparent

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = cardBg,
        shadowElevation = 0.dp,
        border = null,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Row 1: Icon + Name/Streak + Check button ──────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBg)
                        .border(1.dp, if (isDark) Color.Transparent else accentColor.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Name + streak
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Streak · $streak day${if (streak != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = if (streak > 0) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Completion toggle
                CompletionButton(
                    isCompleted = isCompletedToday,
                    accentColor = accentColor,
                    onClick = onToggleCompletion
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Row 2: Heatmap ─────────────────────────────────────────────────
            HeatmapGrid(
                completedDates = completedDates,
                accentColor = accentColor
            )
        }
    }
}

// ── Completion Button ──────────────────────────────────────────────────
@Composable
private fun CompletionButton(
    isCompleted: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // In light theme, if the accent color is too light, the button can blend into the card.
    // Adding a subtle border for contrast when completed in light theme.
    val completedBorderColor = if (!isDark && accentColor.luminance() > 0.4f) {
        accentColor.copy(alpha = 0.8f).compositeOver(Color.Black.copy(alpha = 0.2f))
    } else {
        accentColor
    }
    
    // Fix: Uncompleted empty circle in light mode needs higher contrast
    val emptyBorderColor = if (!isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isCompleted) accentColor else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (isCompleted) completedBorderColor else emptyBorderColor,
                shape = CircleShape
            )
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        if (isCompleted) {
            // Determine text colour based on luminance for contrast
            val iconTint = if (accentColor.luminance() > 0.4f) Color(0xFF1A1A1A) else Color.White
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Heatmap Grid ───────────────────────────────────────────────────────────────
/**
 * Renders a 10 × 7 grid (most-recent 70 days, newest bottom-right).
 * Each cell is a small rounded square, coloured with the habit accent if completed.
 */
@Composable
fun HeatmapGrid(
    completedDates: StableSet<kotlinx.datetime.LocalDate>,
    accentColor: Color
) {
    val today = kotlinx.datetime.Clock.System.now().let {
        kotlinx.datetime.TimeZone.currentSystemDefault().let { tz ->
            it.toLocalDateTime(tz).date
        }
    }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val totalCells = 154 // 22 weeks × 7 days

    // Build list: index 0 = oldest day (153 days ago), index 153 = today
    val days = (totalCells - 1 downTo 0).map { daysAgo ->
        today.minus(daysAgo, kotlinx.datetime.DateTimeUnit.DAY)
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp)
    ) {
        val cellPx = 11.dp.toPx()
        val spacingPx = 3.dp.toPx()
        val radius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())

        val uncompletedColor = if (isDark) {
            accentColor.copy(alpha = 0.12f).compositeOver(surfaceColor)
        } else {
            accentColor.copy(alpha = 0.15f)
        }

        days.forEachIndexed { index, date ->
            val col = index / 7
            val row = index % 7

            val isCompleted = completedDates.contains(date)
            val cellColor = if (isCompleted) accentColor else uncompletedColor

            val x = col * (cellPx + spacingPx)
            val y = row * (cellPx + spacingPx)

            drawRoundRect(
                color = cellColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(cellPx, cellPx),
                cornerRadius = radius
            )
        }
    }
}
