package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.AuthState
import com.mishr.habittracker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<AuthState> {
        return authRepository.authState
    }
}