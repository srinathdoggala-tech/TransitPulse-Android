package com.mishr.habittracker.app

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mishr.habittracker.core.designsystem.theme.HabitTrackerTheme
import com.mishr.habittracker.domain.model.AuthState
import com.mishr.habittracker.domain.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
            val authState by viewModel.authState.collectAsStateWithLifecycle()
            val isMigrationPending by viewModel.isGuestMigrationPending.collectAsStateWithLifecycle(initialValue = false)
            
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            HabitTrackerTheme(darkTheme = isDarkTheme) {
                if (hasSeenOnboarding == null || authState is AuthState.Loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val startDest = if (isMigrationPending && authState is AuthState.Authenticated) {
                        Destination.Migration.route
                    } else if (authState is AuthState.Authenticated || hasSeenOnboarding == true) {
                        Destination.Habits.route
                    } else {
                        Destination.Onboarding.route
                    }
                    HabitTrackerNavHost(startDestination = startDest)
                }
            }
        }
    }
}
