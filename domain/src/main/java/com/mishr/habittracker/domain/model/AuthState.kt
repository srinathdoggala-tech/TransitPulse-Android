package com.mishr.habittracker.domain.model

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(
        val userId: String,
        val email: String?,
        val displayName: String?
    ) : AuthState()
}