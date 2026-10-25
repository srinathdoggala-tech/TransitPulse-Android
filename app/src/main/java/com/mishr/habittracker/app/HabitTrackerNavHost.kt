package com.mishr.habittracker.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mishr.habittracker.feature.habits.HabitDetailScreen
import com.mishr.habittracker.feature.habits.HabitsIntent
import com.mishr.habittracker.feature.habits.HabitsScreen
import com.mishr.habittracker.feature.habits.HabitsViewModel
import com.mishr.habittracker.feature.onboarding.OnboardingScreen
import com.mishr.habittracker.feature.settings.SettingsScreen
import com.mishr.habittracker.feature.settings.AccountScreen
import com.mishr.habittracker.feature.settings.AppearanceScreen
import com.mishr.habittracker.feature.settings.CategoriesManagementScreen

/**
 * Navigation host for the Habit Tracker app.
 */
@Composable
fun HabitTrackerNavHost(startDestination: String = Destination.Habits.route) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Destination.Migration.route) {
            com.mishr.habittracker.feature.onboarding.MigrationScreen(
                onMigrationComplete = { 
                    navController.navigate(Destination.Habits.route) { 
                        popUpTo(0) { inclusive = true } 
                    }
                },
                onContinueAsGuest = { 
                    navController.navigate(Destination.Habits.route) { 
                        popUpTo(0) { inclusive = true } 
                    }
                }
            )
        }

        composable(Destination.Onboarding.route) {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate(Destination.Habits.route) {
                        popUpTo(Destination.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToMigration = {
                    navController.navigate(Destination.Migration.route) {
                        popUpTo(Destination.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

                composable(Destination.Habits.route) {
            HabitsScreen(
                onNavigateToDetail = { habitId ->
                    navController.navigate(Destination.HabitDetail.createRoute(habitId))
                },
                onNavigateToSettings = {
                    navController.navigate(Destination.Settings.route)
                }
            )
        }

        composable(Destination.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCategories = {
                    navController.navigate(Destination.CategoriesManagement.route)
                },
                onNavigateToAppearance = {
                    navController.navigate(Destination.Appearance.route)
                },
                onNavigateToAccount = {
                    navController.navigate(Destination.Account.route)
                }
            )
        }

        composable(Destination.Account.route) {
            AccountScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToOnboarding = {
                    navController.navigate(Destination.Onboarding.route) {
                        popUpTo(Destination.Settings.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Destination.Appearance.route) {
            AppearanceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destination.CategoriesManagement.route) {
            CategoriesManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.HabitDetail.route,
            arguments = listOf(navArgument("habitId") { type = NavType.StringType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId") ?: return@composable

            // Obtain the ViewModel scoped to the Habits back-stack entry so we
            // reuse the already-loaded state without a second database call.
            val habitsEntry = remember(backStackEntry) { 
                navController.getBackStackEntry(Destination.Habits.route) 
            }
            val viewModel: HabitsViewModel = hiltViewModel(habitsEntry)
            val state by viewModel.state.collectAsState()

            val habitWithStatus = state.habits.firstOrNull { it.habit.id == habitId }
                ?: return@composable

                        HabitDetailScreen(
                habitWithStatus    = habitWithStatus,
                categories = state.categories,
                onNavigateBack     = { navController.popBackStack() },
                onToggleCompletion = {
                    viewModel.processIntent(HabitsIntent.ToggleCompletion(habitId))
                },
                onUpdateHabit = { updatedHabit ->
                    viewModel.processIntent(HabitsIntent.UpdateHabit(updatedHabit))
                },
                onDeleteHabit = { id ->
                    viewModel.processIntent(HabitsIntent.DeleteHabit(id))
                    navController.popBackStack()
                },
                onUpdateNotes = { notes ->
                    viewModel.processIntent(HabitsIntent.UpdateHabitNotes(habitId, notes))
                },
                onToggleChecklistItem = { itemId, isCompleted ->
                    viewModel.processIntent(HabitsIntent.ToggleChecklistItem(habitId, itemId, isCompleted))
                },
                onCreateCategory = {
                    viewModel.processIntent(HabitsIntent.CreateCategory(it, "#FF6B6B"))
                }
            )
        }
    }
}
