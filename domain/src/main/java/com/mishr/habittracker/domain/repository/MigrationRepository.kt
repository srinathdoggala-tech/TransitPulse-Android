package com.mishr.habittracker.domain.repository

import com.mishr.habittracker.domain.model.MigrationStatus
import kotlinx.coroutines.flow.Flow

interface MigrationRepository {
    /**
     * Operation 1: PUSH — Migrate local Guest data up to Firestore for this userId.
     * Only called when isGuestMigrationPending == true (i.e., user had local data before signing in).
     */
    fun migrateGuestDataToCloud(userId: String): Flow<MigrationStatus>

    /**
     * Operation 2: PULL — Pull existing Firestore data down into empty local Room.
     * Called on EVERY successful sign-in when local Room is empty, regardless of guest flag.
     * Returns true if any data was pulled, false if Firestore was also empty.
     */
    suspend fun pullCloudDataToLocal(userId: String): Result<Boolean>

    /**
     * Wipes all local Room tables. Called on sign-out.
     */
    suspend fun wipeLocalData()

    /**
     * Deletes all cloud data for the given user.
     */
    suspend fun deleteCloudData(userId: String): Result<Unit>
}
