package com.mishr.habittracker.domain.usecase

import com.mishr.habittracker.domain.repository.ReminderScheduler
import com.mishr.habittracker.domain.result.AppResult
import javax.inject.Inject

class CancelReminderUseCase @Inject constructor(
    private val reminderScheduler: ReminderScheduler
) {
    operator fun invoke(habitId: String): AppResult<Unit> {
        return reminderScheduler.cancel(habitId)
    }
}
