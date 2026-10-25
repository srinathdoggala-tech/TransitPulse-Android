package com.mishr.habittracker.core.firebase.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mishr.habittracker.core.firebase.AuthRepositoryImpl
import com.mishr.habittracker.core.firebase.MigrationRepositoryImpl
import com.mishr.habittracker.domain.repository.AuthRepository
import com.mishr.habittracker.domain.repository.MigrationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindMigrationRepository(
        migrationRepositoryImpl: MigrationRepositoryImpl
    ): MigrationRepository

    @Binds
    @Singleton
    abstract fun bindSyncTrigger(
        syncScheduler: com.mishr.habittracker.core.firebase.sync.SyncScheduler
    ): com.mishr.habittracker.domain.sync.SyncTrigger


    @Binds
    @Singleton
    abstract fun bindStartCloudSyncUseCase(
        impl: com.mishr.habittracker.core.firebase.usecase.StartCloudSyncUseCaseImpl
    ): com.mishr.habittracker.domain.usecase.StartCloudSyncUseCase

    @Binds
    @Singleton
    abstract fun bindStopCloudSyncUseCase(
        impl: com.mishr.habittracker.core.firebase.usecase.StopCloudSyncUseCaseImpl
    ): com.mishr.habittracker.domain.usecase.StopCloudSyncUseCase

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth {
            return FirebaseAuth.getInstance()
        }

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            return FirebaseFirestore.getInstance()
        }
    }
}
