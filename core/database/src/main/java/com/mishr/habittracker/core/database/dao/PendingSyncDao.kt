package com.mishr.habittracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {
    
    @Insert
    suspend fun insertSyncEvent(event: com.mishr.habittracker.core.database.entity.PendingSyncEntity)

    @Query("SELECT * FROM pending_sync_events ORDER BY timestamp ASC")
    suspend fun getAllPendingSyncEvents(): List<com.mishr.habittracker.core.database.entity.PendingSyncEntity>

    @Query("DELETE FROM pending_sync_events WHERE id IN (:ids)")
    suspend fun deleteSyncEvents(ids: List<Long>)

    @Query("SELECT * FROM pending_sync_events WHERE entityId = :entityId")
    suspend fun getSyncEventsForEntity(entityId: String): List<com.mishr.habittracker.core.database.entity.PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync_events")
    fun getPendingSyncCount(): Flow<Int>
}
