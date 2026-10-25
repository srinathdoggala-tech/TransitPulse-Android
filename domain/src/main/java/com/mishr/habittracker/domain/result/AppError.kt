package com.mishr.habittracker.domain.result

/**
 * Base sealed class for all application errors.
 */
sealed class AppError {
    /**
     * Represents network-related errors.
     */
    data class NetworkError(val message: String) : AppError()
    
    /**
     * Represents database-related errors.
     */
    data class DatabaseError(val message: String) : AppError()
    
    /**
     * Represents authentication-related errors.
     */
    data class AuthError(val message: String) : AppError()
    
    /**
     * Represents data migration-related errors.
     */
    data class MigrationError(val message: String) : AppError()
    
    /**
     * Represents an unknown or unexpected error.
     */
    data class UnknownError(val throwable: Throwable? = null) : AppError()
}
