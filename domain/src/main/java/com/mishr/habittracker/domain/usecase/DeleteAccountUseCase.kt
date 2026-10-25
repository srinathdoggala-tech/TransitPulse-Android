package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.AuthState
import com.mishr.habittracker.domain.repository.AuthRepository
import com.mishr.habittracker.domain.repository.MigrationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val migrationRepository: MigrationRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        // Fetch current user ID before we delete the auth record
        val authState = authRepository.authState.first { it !is AuthState.Loading }
        val userId = (authState as? AuthState.Authenticated)?.userId

        if (userId != null) {
            // Delete cloud data first, because we need the auth token to pass Firestore security rules!
            val cloudRes = migrationRepository.deleteCloudData(userId)
            if (cloudRes.isFailure) {
                return cloudRes // Failed to delete cloud data
            }
        }

        // Now delete the Firebase Auth user
        return authRepository.deleteAccount().onSuccess {
            // Finally wipe the local room db
            migrationRepository.wipeLocalData()
        }
    }
}
