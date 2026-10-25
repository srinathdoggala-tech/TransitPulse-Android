package com.mishr.habittracker.core.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mishr.habittracker.domain.repository.HabitRepository
import com.mishr.habittracker.domain.repository.ReminderScheduler
import com.mishr.habittracker.domain.repository.StreakSaverScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class RescheduleRemindersWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler,
    private val streakSaverScheduler: StreakSaverScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            streakSaverScheduler.schedule()
            val habits = habitRepository.getAllHabits().firstOrNull() ?: emptyList()
            val result = reminderScheduler.rescheduleAll(habits)
            
            if (result is com.mishr.habittracker.domain.result.AppResult.Success) {
                Log.d("RescheduleWorker", "Successfully rescheduled alarms")
                Result.success()
            } else {
                Log.e("RescheduleWorker", "Failed to reschedule alarms")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("RescheduleWorker", "Exception during reschedule", e)
            Result.retry()
        }
    }
}
