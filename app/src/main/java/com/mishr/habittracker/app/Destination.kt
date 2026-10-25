package com.mishr.habittracker.app

sealed class Destination(val route: String) {
    data object Onboarding : Destination("onboarding")
    data object Habits : Destination("habits")
    data object Settings : Destination("settings")
    data object Account : Destination("account")
    data object Appearance : Destination("appearance")
    data object CategoriesManagement : Destination("categories_management")
    data object Migration : Destination("migration")
    data object HabitDetail : Destination("habit_detail/{habitId}") {
        fun createRoute(habitId: String) = "habit_detail/$habitId"
    }
}
