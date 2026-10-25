package com.mishr.habittracker.feature.onboarding

sealed class OnboardingIntent {
    object ContinueAsGuest : OnboardingIntent()
    data class SignInWithGoogle(val idToken: String) : OnboardingIntent()
    data class SignInFailed(val error: String) : OnboardingIntent()
}

data class OnboardingState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class OnboardingEffect {
    object NavigateToHome : OnboardingEffect()
    object NavigateToMigration : OnboardingEffect()
}