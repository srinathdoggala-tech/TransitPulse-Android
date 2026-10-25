package com.mishr.habittracker.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary          = HabitYellow,
    onPrimary        = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFFFFF3CD),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary        = HabitGreen,
    onSecondary      = Color.White,
    background       = Color(0xFFF9F9F9),
    onBackground     = Color(0xFF1A1A1A),
    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF1A1A1A),
    surfaceVariant   = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF7A7A7A),
    outline          = Color(0xFFEAEAEA),
    error            = HabitRed,
    onError          = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary          = HabitYellow,
    onPrimary        = Color(0xFF1A1A1A),
    background       = Color(0xFF0B1017),
    onBackground     = Color(0xFFF5F7FA),
    surface          = Color(0xFF151B23),
    onSurface        = Color(0xFFF5F7FA),
    surfaceVariant   = Color(0xFF1B222C),
    onSurfaceVariant = Color(0xFFA7B0BE),
    outline          = Color(0xFF28313C)
)

@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
