package com.mishr.habittracker.domain.model

sealed class MigrationStatus {
    object NotStarted : MigrationStatus()
    data class InProgress(val progress: Float) : MigrationStatus()
    object Completed : MigrationStatus()
    data class Failed(val error: String, val canRetry: Boolean) : MigrationStatus()
}
