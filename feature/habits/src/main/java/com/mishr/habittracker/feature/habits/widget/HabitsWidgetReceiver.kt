package com.mishr.habittracker.feature.habits.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.mishr.habittracker.feature.habits.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HabitsWidgetReceiver : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_HABIT = "com.mishr.habittracker.widget.ACTION_TOGGLE_HABIT"
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_habits_container)

            // Intent to launch MainActivity
            val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
            val mainPendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_header, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_text, mainPendingIntent)

            // Setup ListView adapter
            val serviceIntent = Intent(context, HabitsWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_text)

            // Setup PendingIntent template for ListView items
            val toggleIntent = Intent(context, HabitsWidgetReceiver::class.java).apply {
                action = ACTION_TOGGLE_HABIT
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context, 1, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, togglePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_TOGGLE_HABIT) {
            val habitId = intent.data?.lastPathSegment ?: intent.getStringExtra(EXTRA_HABIT_ID) ?: return
            Log.d("WIDGET_SYNC", "Tap received for habit: $habitId")
            
            // Trigger instant UI refresh
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HabitsWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            // Re-render the list items instantly
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
            Log.d("WIDGET_SYNC", "notifyAppWidgetViewDataChanged called instantly")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
                    val useCase = entryPoint.getToggleHabitCompletionUseCase()
                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    
                    Log.d("WIDGET_SYNC", "Invoking exact same ToggleHabitCompletionUseCase")
                    useCase(habitId, today)
                    Log.d("WIDGET_SYNC", "UseCase completed successfully")
                } catch (e: Exception) {
                    Log.e("WIDGET_SYNC", "Background write failed: $e", e)
                    // Revert optimistic UI on failure
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
                } finally {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
                    pendingResult.finish()
                }
            }
        }
    }
}


