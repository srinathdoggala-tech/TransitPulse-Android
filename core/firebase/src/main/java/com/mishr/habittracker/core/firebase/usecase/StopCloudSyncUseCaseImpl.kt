package com.mishr.habittracker.core.firebase.usecase
import com.mishr.habittracker.domain.usecase.StopCloudSyncUseCase
import com.mishr.habittracker.core.firebase.sync.CloudSyncManager
import javax.inject.Inject

class StopCloudSyncUseCaseImpl @Inject constructor(
    private val cloudSyncManager: CloudSyncManager
) : StopCloudSyncUseCase {
    override fun invoke() {
        cloudSyncManager.stopListening()
    }
}
