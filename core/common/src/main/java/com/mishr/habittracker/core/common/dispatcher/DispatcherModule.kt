package com.mishr.habittracker.core.common.dispatcher

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Hilt module to provide dispatchers and the [DispatcherProvider].
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(dispatcherProvider: DispatcherProvider): CoroutineDispatcher =
        dispatcherProvider.io

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(dispatcherProvider: DispatcherProvider): CoroutineDispatcher =
        dispatcherProvider.main

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(dispatcherProvider: DispatcherProvider): CoroutineDispatcher =
        dispatcherProvider.default
}
