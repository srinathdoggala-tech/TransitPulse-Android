package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.model.MigrationStatus
import com.mishr.habittracker.domain.repository.MigrationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MigrateGuestDataUseCase @Inject constructor(
    private val migrationRepository: MigrationRepository
) {
    operator fun invoke(userId: String): Flow<MigrationStatus> {
        return migrationRepository.migrateGuestDataToCloud(userId)
    }
}
