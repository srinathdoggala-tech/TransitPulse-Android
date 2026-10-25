package com.mishr.habittracker.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Interface abstracting coroutine dispatchers for easier testing.
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}
