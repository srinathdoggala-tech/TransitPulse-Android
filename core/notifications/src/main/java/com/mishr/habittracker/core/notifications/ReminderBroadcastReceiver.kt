package com.mishr.habittracker.core.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.mishr.habittracker.domain.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitRepository: HabitRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
        if (habitId == null) {
            Log.e("ReminderReceiver", "Received intent without habit ID")
            return
        }

        val pendingResult = goAsync()
        
        scope.launch {
            try {
                val habit = habitRepository.getHabitById(habitId).firstOrNull()
                if (habit != null && habit.isActive && habit.reminderEnabled) {
                    showNotification(context, habitId, habit.name)
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Failed to process reminder for $habitId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, habitId: String, habitName: String) {
        // Create an Intent for the activity you want to start
        // Using deep linking to the specific habit would be ideal, but for now we'll just open the app.
        // Assuming MainActivity is the entry point
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        var pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            if (launchIntent != null) {
                addNextIntentWithParentStack(launchIntent)
            }
            getPendingIntent(
                habitId.hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        if (pendingIntent == null) {
            Log.w("ReminderReceiver", "TaskStackBuilder returned null PendingIntent. Falling back to simple PendingIntent.")
            if (launchIntent != null) {
                pendingIntent = PendingIntent.getActivity(
                    context,
                    habitId.hashCode(),
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }

        val builder = NotificationCompat.Builder(context, NotificationChannels.HABIT_REMINDERS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Habit Reminder")
            .setContentText("It's time for: $habitName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Permission check is required by IDE, but we assume it's granted or OS won't show it
            notificationManager.notify(habitId.hashCode(), builder.build())
        } catch (e: SecurityException) {
            Log.e("ReminderReceiver", "Missing POST_NOTIFICATIONS permission", e)
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Failed to show notification", e)
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }
}
