package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.exception.RequiresRecentLoginException
import com.mishr.habittracker.domain.model.AuthState
import com.mishr.habittracker.domain.repository.AuthRepository
import com.mishr.habittracker.domain.repository.MigrationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteAccountUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var migrationRepository: MigrationRepository
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase

    @Before
    fun setup() {
        authRepository = mockk(relaxed = true)
        migrationRepository = mockk(relaxed = true)
        deleteAccountUseCase = DeleteAccountUseCase(authRepository, migrationRepository)
        
        every { authRepository.authState } returns flowOf(AuthState.Authenticated("user123", "test@test.com", "Test User"))
    }

    @Test
    fun `invoke - full success path - deletes cloud, then auth, then room`() = runTest {
        coEvery { migrationRepository.deleteCloudData("user123") } returns Result.success(Unit)
        coEvery { authRepository.deleteAccount() } returns Result.success(Unit)

        val result = deleteAccountUseCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { migrationRepository.deleteCloudData("user123") }
        coVerify(exactly = 1) { authRepository.deleteAccount() }
        coVerify(exactly = 1) { migrationRepository.wipeLocalData() }
    }

    @Test
    fun `invoke - cloud deletion failure - Auth and Room untouched`() = runTest {
        val error = Exception("Cloud delete failed")
        coEvery { migrationRepository.deleteCloudData("user123") } returns Result.failure(error)

        val result = deleteAccountUseCase()

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        
        coVerify(exactly = 1) { migrationRepository.deleteCloudData("user123") }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
        coVerify(exactly = 0) { migrationRepository.wipeLocalData() }
    }

    @Test
    fun `invoke - requires-recent-login - returns specific error and leaves room untouched`() = runTest {
        coEvery { migrationRepository.deleteCloudData("user123") } returns Result.success(Unit)
        
        val recentLoginError = RequiresRecentLoginException("Requires recent login")
        coEvery { authRepository.deleteAccount() } returns Result.failure(recentLoginError)

        val result = deleteAccountUseCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RequiresRecentLoginException)
        
        coVerify(exactly = 1) { migrationRepository.deleteCloudData("user123") }
        coVerify(exactly = 1) { authRepository.deleteAccount() }
        coVerify(exactly = 0) { migrationRepository.wipeLocalData() }
    }
}
