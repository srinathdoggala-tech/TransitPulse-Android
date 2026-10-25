package com.mishr.habittracker.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.mishr.habittracker.domain.model.Habit
import com.mishr.habittracker.domain.repository.ReminderScheduler
import com.mishr.habittracker.domain.result.AppError
import com.mishr.habittracker.domain.result.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject

class AlarmManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(habitId: String, hour: Int, minute: Int): AppResult<Unit> {
        return try {
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                putExtra(ReminderBroadcastReceiver.EXTRA_HABIT_ID, habitId)
                data = Uri.parse("habit://$habitId")
            }

            // The requestCode is no longer the sole uniqueness factor since intent.data is set, 
            // completely mitigating hash collision risks per Android's Intent.filterEquals() rules.
            val requestCode = habitId.hashCode()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val triggerTime = calendar.timeInMillis

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if permission denied
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("AlarmManagerScheduler", "Failed to schedule alarm", e)
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    override fun cancel(habitId: String): AppResult<Unit> {
        return try {
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                data = Uri.parse("habit://$habitId")
            }
            val requestCode = habitId.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("AlarmManagerScheduler", "Failed to cancel alarm", e)
            AppResult.Error(AppError.UnknownError(e))
        }
    }

    override fun rescheduleAll(habits: List<Habit>): AppResult<Unit> {
        return try {
            for (habit in habits) {
                val hour = habit.reminderHour
                val minute = habit.reminderMinute
                if (habit.isActive && habit.reminderEnabled && hour != null && minute != null) {
                    schedule(habit.id, hour, minute)
                } else {
                    cancel(habit.id)
                }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("AlarmManagerScheduler", "Failed to reschedule alarms", e)
            AppResult.Error(AppError.UnknownError(e))
        }
    }
}
