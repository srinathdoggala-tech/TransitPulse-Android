package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.repository.AuthRepository
import com.mishr.habittracker.domain.repository.MigrationRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val migrationRepository: MigrationRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut().onSuccess {
            migrationRepository.wipeLocalData()
        }
    }
}
