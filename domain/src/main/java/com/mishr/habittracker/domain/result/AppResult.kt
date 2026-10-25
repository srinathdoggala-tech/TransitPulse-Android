package com.mishr.habittracker.domain.result

/**
 * A discriminated union that encapsulates a successful outcome with a value of type [T]
 * or a failure with an [AppError].
 */
sealed class AppResult<out T> {
    /**
     * Represents a successful outcome with a value of type [T].
     */
    data class Success<out T>(val data: T) : AppResult<T>()
    
    /**
     * Represents a failed outcome with an [AppError].
     */
    data class Error(val error: AppError) : AppResult<Nothing>()
}

/**
 * Returns true if this instance represents a successful outcome.
 */
fun <T> AppResult<T>.isSuccess(): Boolean = this is AppResult.Success

/**
 * Returns true if this instance represents a failed outcome.
 */
fun <T> AppResult<T>.isError(): Boolean = this is AppResult.Error

/**
 * Executes the given [action] if this instance represents a successful outcome.
 */
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

/**
 * Executes the given [action] if this instance represents a failed outcome.
 */
inline fun <T> AppResult<T>.onError(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(error)
    return this
}
