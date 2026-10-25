package com.mishr.habittracker.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class StreakSaverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("StreakSaverReceiver", "Received 10 PM trigger. Starting WorkManager...")
        val workRequest = OneTimeWorkRequestBuilder<StreakSaverWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
