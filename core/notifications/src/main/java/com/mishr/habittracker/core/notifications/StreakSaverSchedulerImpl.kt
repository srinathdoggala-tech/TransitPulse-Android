package com.mishr.habittracker.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mishr.habittracker.domain.repository.StreakSaverScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject

class StreakSaverSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StreakSaverScheduler {

    override fun schedule() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StreakSaverReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            STREAK_SAVER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Target exactly 10:00 PM today
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If it's already past 10 PM, schedule for tomorrow 10 PM
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("StreakSaverScheduler", "Scheduled Streak Saver for: ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("StreakSaverScheduler", "Exact alarm permission missing", e)
        }
    }

    companion object {
        const val STREAK_SAVER_REQUEST_CODE = 8888
    }
}
