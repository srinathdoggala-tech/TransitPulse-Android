package com.mishr.habittracker.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps an icon key string to an ImageVector for display.
 * The key is stored in the data layer (Habit.icon).
 */
object HabitIconSet {

    /** All available icons the user can pick from. */
    val all: List<HabitIcon> = listOf(
        // Existing
        HabitIcon("star",          Icons.Filled.Star),
        HabitIcon("favorite",      Icons.Filled.Favorite),
        HabitIcon("face",          Icons.Filled.Face),
        HabitIcon("home",          Icons.Filled.Home),
        HabitIcon("build",         Icons.Filled.Build),
        HabitIcon("edit",          Icons.Filled.Edit),
        HabitIcon("date_range",    Icons.Filled.DateRange),
        HabitIcon("list",          Icons.AutoMirrored.Filled.List),
        HabitIcon("location",      Icons.Filled.LocationOn),
        HabitIcon("lock",          Icons.Filled.Lock),
        HabitIcon("mail",          Icons.Filled.MailOutline),
        HabitIcon("notifications", Icons.Filled.Notifications),
        HabitIcon("person",        Icons.Filled.Person),
        HabitIcon("phone",         Icons.Filled.Phone),
        HabitIcon("play",          Icons.Filled.PlayArrow),
        HabitIcon("shopping",      Icons.Filled.ShoppingCart),
        HabitIcon("thumb_up",      Icons.Filled.ThumbUp),
        HabitIcon("check_circle",  Icons.Filled.CheckCircle),
        HabitIcon("create",        Icons.Filled.Create),
        HabitIcon("settings",      Icons.Filled.Settings),
        
        // New Additions (Minimalist Material Icons)
        HabitIcon("smoke_free",    Icons.Filled.SmokeFree),
        HabitIcon("fitness_center",Icons.Filled.FitnessCenter),
        HabitIcon("directions_run",Icons.AutoMirrored.Filled.DirectionsRun),
        HabitIcon("directions_walk",Icons.AutoMirrored.Filled.DirectionsWalk),
        HabitIcon("directions_bike",Icons.AutoMirrored.Filled.DirectionsBike),
        HabitIcon("self_improvement",Icons.Filled.SelfImprovement),
        HabitIcon("water_drop",    Icons.Filled.WaterDrop),
        HabitIcon("restaurant",    Icons.Filled.Restaurant),
        HabitIcon("local_dining",  Icons.Filled.LocalDining),
        HabitIcon("bedtime",       Icons.Filled.Bedtime),
        HabitIcon("menu_book",     Icons.AutoMirrored.Filled.MenuBook),
        HabitIcon("school",        Icons.Filled.School),
        HabitIcon("code",          Icons.Filled.Code),
        HabitIcon("work",          Icons.Filled.Work),
        HabitIcon("book",          Icons.Filled.Book),
        HabitIcon("music_note",    Icons.Filled.MusicNote),
        HabitIcon("brush",         Icons.Filled.Brush),
        HabitIcon("cleaning_services",Icons.Filled.CleaningServices),
        HabitIcon("spa",           Icons.Filled.Spa),
        HabitIcon("wb_sunny",      Icons.Filled.WbSunny),
        HabitIcon("nights_stay",   Icons.Filled.NightsStay),
        HabitIcon("no_cell",       Icons.Filled.NoCell),
        HabitIcon("savings",       Icons.Filled.Savings),
        HabitIcon("track_changes", Icons.Filled.TrackChanges),
        HabitIcon("health_and_safety",Icons.Filled.HealthAndSafety),
        HabitIcon("emoji_events",  Icons.Filled.EmojiEvents),
        HabitIcon("eco",           Icons.Filled.Eco),
        HabitIcon("local_cafe",    Icons.Filled.LocalCafe),
        HabitIcon("monitor_heart", Icons.Filled.MonitorHeart),
        HabitIcon("laptop_mac",    Icons.Filled.LaptopMac),
        HabitIcon("directions_car",Icons.Filled.DirectionsCar),
        HabitIcon("flight",        Icons.Filled.Flight),
        HabitIcon("pets",          Icons.Filled.Pets),
        HabitIcon("psychology",    Icons.Filled.Psychology),
    )

    fun vectorFor(key: String): ImageVector =
        all.firstOrNull { it.key == key }?.vector ?: Icons.Filled.Star
}

data class HabitIcon(val key: String, val vector: ImageVector)
