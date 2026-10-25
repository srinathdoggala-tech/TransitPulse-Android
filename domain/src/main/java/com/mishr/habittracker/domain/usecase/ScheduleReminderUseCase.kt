package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.repository.ReminderScheduler
import com.mishr.habittracker.domain.result.AppResult
import javax.inject.Inject

class ScheduleReminderUseCase @Inject constructor(
    private val reminderScheduler: ReminderScheduler
) {
    operator fun invoke(habitId: String, hour: Int, minute: Int): AppResult<Unit> {
        return reminderScheduler.schedule(habitId, hour, minute)
    }
}
