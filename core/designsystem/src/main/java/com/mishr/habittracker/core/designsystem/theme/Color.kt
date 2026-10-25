package com.mishr.habittracker.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ── Base Palette ──────────────────────────────────────────────────────────────
val BackgroundLight    = Color(0xFFF5F5F0) // warm off-white
val SurfaceLight       = Color(0xFFFFFFFF)
val OnSurfaceLight     = Color(0xFF1A1A1A)
val OnSurfaceVariant   = Color(0xFF7A7A7A)
val DividerColor       = Color(0xFFEAEAEA)

// ── Habit Accent Colours (user-choosable palette) ──────────────────────────────
val HabitYellow        = Color(0xFFFFC107)  // Gym / default
val HabitGreen         = Color(0xFF4CAF50)  // DSA / nature
val HabitRed           = Color(0xFFF44336)  // Health / urgent
val HabitBlue          = Color(0xFF2196F3)  // Focus / work
val HabitPurple        = Color(0xFF9C27B0)  // Mindfulness
val HabitOrange        = Color(0xFFFF5722)  // Fitness
val HabitTeal          = Color(0xFF009688)  // Wellness
val HabitPink          = Color(0xFFE91E63)  // Creative
val HabitBrown         = Color(0xFF795548)  // Food / diet
val HabitIndigo        = Color(0xFF3F51B5)  // Learning

val HabitPalette = listOf(
    HabitYellow, HabitGreen, HabitRed, HabitBlue, HabitPurple,
    HabitOrange, HabitTeal, HabitPink, HabitBrown, HabitIndigo
)

// ── Heatmap Colours ────────────────────────────────────────────────────────────
val HeatmapEmpty       = Color(0xFFEDEDE8)  // subtle warm grey
val HeatmapFull        = Color(0xFF4CAF50)  // override per-habit with accent

// ── Semantic / State ──────────────────────────────────────────────────────────
val HabitCompleted     = Color(0xFF4CAF50)
val HabitIncomplete    = Color(0xFFE0E0E0)
val StreakFire         = Color(0xFFFF9800)

// ── Legacy (kept for backward compat) ─────────────────────────────────────────
val Purple80           = Color(0xFFD0BCFF)
val PurpleGrey80       = Color(0xFFCCC2DC)
val Pink80             = Color(0xFFEFB8C8)
val Purple40           = Color(0xFF6650a4)
val PurpleGrey40       = Color(0xFF625b71)
val Pink40             = Color(0xFF7D5260)
val CardGlassSurface   = Color(0x80FFFFFF)
val CardGlassSurfaceDark = Color(0x20000000)
val HabitIncompleteDark = Color(0xFF424242)
