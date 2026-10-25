package com.mishr.habittracker.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*

@Composable
fun HabitCalendarSheetContent(
    completedDates: Set<LocalDate>,
    accentColor: Color
) {
    var today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var currentMonthStart by remember { mutableStateOf(LocalDate(today.year, today.monthNumber, 1)) }
    
    val daysInMonth = getDaysInMonth(currentMonthStart.year, currentMonthStart.monthNumber)
    val firstDayOfWeek = currentMonthStart.dayOfWeek.isoDayNumber // Monday is 1, Sunday is 7
    // Calculate empty cells before the 1st of the month
    val emptyCellsBefore = firstDayOfWeek - 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header (Month Navigation)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentMonthStart = if (currentMonthStart.monthNumber == 1) {
                    LocalDate(currentMonthStart.year - 1, 12, 1)
                } else {
                    LocalDate(currentMonthStart.year, currentMonthStart.monthNumber - 1, 1)
                }
            }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month", tint = accentColor)
            }
            
            Text(
                text = currentMonthStart.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + currentMonthStart.year,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = {
                currentMonthStart = if (currentMonthStart.monthNumber == 12) {
                    LocalDate(currentMonthStart.year + 1, 1, 1)
                } else {
                    LocalDate(currentMonthStart.year, currentMonthStart.monthNumber + 1, 1)
                }
            }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month", tint = accentColor)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Days of week header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(emptyCellsBefore) {
                Box(modifier = Modifier.size(40.dp))
            }
            items(daysInMonth) { dayIndex ->
                val dayOfMonth = dayIndex + 1
                val date = LocalDate(currentMonthStart.year, currentMonthStart.monthNumber, dayOfMonth)
                val isCompleted = completedDates.contains(date)
                val isToday = date == today
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayOfMonth.toString(),
                            color = if (isCompleted) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isCompleted || isToday) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        if (isToday && !isCompleted) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}
