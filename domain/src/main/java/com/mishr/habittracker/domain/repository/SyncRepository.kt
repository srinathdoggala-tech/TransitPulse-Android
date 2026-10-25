package com.mishr.habittracker.domain.repository

import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun getPendingSyncCount(): Flow<Int>
}
