package com.mishr.habittracker.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mishr.habittracker.domain.repository.StreakSaverScheduler
import com.mishr.habittracker.domain.usecase.GetHabitsWithStatusUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltWorker
class StreakSaverWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getHabitsWithStatusUseCase: GetHabitsWithStatusUseCase,
    private val streakSaverScheduler: StreakSaverScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Reschedule for tomorrow right away
            streakSaverScheduler.schedule()

            // 2. Fetch habits
            val habitsWithStatus = getHabitsWithStatusUseCase().firstOrNull() ?: emptyList()
            if (habitsWithStatus.isEmpty()) return Result.success()

            // 3. Filter for at-risk habits
            // - Scheduled for today (isScheduledToday - implicit by streak grace logic or we can calculate it)
            // Wait, GetHabitsWithStatusUseCase calculates streak with today's date.
            // If it's NOT scheduled today, then missing it wouldn't break the streak.
            // Let's explicitly define logic:
            
            val atRiskHabits = habitsWithStatus.filter { 
                !it.isCompletedToday && it.currentStreak > 0 
            }
            
            // Further filter: is it scheduled today?
            // A habit that is NOT scheduled today shouldn't nag the user.
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val actuallyAtRisk = atRiskHabits.filter { hws -> 
                val h = hws.habit
                if (h.scheduleType == com.mishr.habittracker.domain.model.ScheduleType.DAILY) true
                else {
                    val ktxDay = today.dayOfWeek
                    val domainDay = when (ktxDay) {
                        kotlinx.datetime.DayOfWeek.MONDAY -> com.mishr.habittracker.domain.model.DayOfWeek.MONDAY
                        kotlinx.datetime.DayOfWeek.TUESDAY -> com.mishr.habittracker.domain.model.DayOfWeek.TUESDAY
                        kotlinx.datetime.DayOfWeek.WEDNESDAY -> com.mishr.habittracker.domain.model.DayOfWeek.WEDNESDAY
                        kotlinx.datetime.DayOfWeek.THURSDAY -> com.mishr.habittracker.domain.model.DayOfWeek.THURSDAY
                        kotlinx.datetime.DayOfWeek.FRIDAY -> com.mishr.habittracker.domain.model.DayOfWeek.FRIDAY
                        kotlinx.datetime.DayOfWeek.SATURDAY -> com.mishr.habittracker.domain.model.DayOfWeek.SATURDAY
                        kotlinx.datetime.DayOfWeek.SUNDAY -> com.mishr.habittracker.domain.model.DayOfWeek.SUNDAY
                        else -> com.mishr.habittracker.domain.model.DayOfWeek.MONDAY
                    }
                    h.scheduledDays.contains(domainDay)
                }
            }

            if (actuallyAtRisk.isNotEmpty()) {
                sendSmartNotification(actuallyAtRisk)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("StreakSaverWorker", "Failed to process streak saver", e)
            Result.retry()
        }
    }

    private fun sendSmartNotification(atRiskHabits: List<com.mishr.habittracker.domain.model.HabitWithStatus>) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Construct the intent to open the app
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            9999,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Don't lose your streak! \uD83D\uDD25"
        val text = if (atRiskHabits.size == 1) {
            "You have 2 hours left to complete '${atRiskHabits[0].habit.name}'."
        } else {
            "You have ${atRiskHabits.size} habits left to complete before midnight."
        }

        val notification = NotificationCompat.Builder(appContext, NotificationChannels.HABIT_REMINDERS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // temporary fallback icon
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(8888, notification)
    }
}
