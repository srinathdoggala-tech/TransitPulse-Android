package com.mishr.habittracker.core.database.repository

import com.mishr.habittracker.core.database.dao.PendingSyncDao
import com.mishr.habittracker.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val pendingSyncDao: PendingSyncDao
) : SyncRepository {
    override fun getPendingSyncCount(): Flow<Int> {
        return pendingSyncDao.getPendingSyncCount()
    }
}
