package com.mishr.habittracker.core.firebase.usecase
import com.mishr.habittracker.domain.usecase.StartCloudSyncUseCase
import com.mishr.habittracker.core.firebase.sync.CloudSyncManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.DelicateCoroutinesApi
import javax.inject.Inject

class StartCloudSyncUseCaseImpl @Inject constructor(
    private val cloudSyncManager: CloudSyncManager
) : StartCloudSyncUseCase {
    @OptIn(DelicateCoroutinesApi::class)
    override fun invoke(userId: String) {
        cloudSyncManager.startListening(userId, GlobalScope)
    }
}
