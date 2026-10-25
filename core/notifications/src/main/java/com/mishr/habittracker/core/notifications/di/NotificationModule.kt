package com.mishr.habittracker.core.notifications.di

import com.mishr.habittracker.core.notifications.AlarmManagerReminderScheduler
import com.mishr.habittracker.domain.repository.ReminderScheduler
import dagger.Binds
import com.mishr.habittracker.core.notifications.StreakSaverSchedulerImpl
import com.mishr.habittracker.domain.repository.StreakSaverScheduler
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(
        impl: AlarmManagerReminderScheduler
    ): ReminderScheduler

    @Binds
    @Singleton
    abstract fun bindStreakSaverScheduler(
        impl: StreakSaverSchedulerImpl
    ): StreakSaverScheduler
}
