package com.mishr.habittracker.feature.habits.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.mishr.habittracker.feature.habits.R
import com.mishr.habittracker.domain.model.HabitWithStatus
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone

class HabitsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitsWidgetFactory(this.applicationContext)
    }
}

class HabitsWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var habits: List<HabitWithStatus> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        Log.d("WIDGET_SYNC", "RemoteViewsFactory.onDataSetChanged() STARTED")
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val useCase = entryPoint.getHabitsWithStatusUseCase()
        
        runBlocking {
            try {
                val allHabits = useCase().first()
                val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                val todayDomainDayOfWeek = com.mishr.habittracker.domain.model.DayOfWeek.valueOf(today.dayOfWeek.name)
                
                habits = allHabits.filter { habitWithStatus ->
                    val habit = habitWithStatus.habit
                    habit.scheduleType == com.mishr.habittracker.domain.model.ScheduleType.DAILY || 
                    habit.scheduledDays.contains(todayDomainDayOfWeek)
                }
            } catch (e: Exception) {
                Log.e("WIDGET_SYNC", "Error fetching habits in factory", e)
                habits = emptyList()
            }
        }
        val size = habits.size
        Log.d("WIDGET_SYNC", "RemoteViewsFactory.onDataSetChanged() COMPLETED. Fetched " + size + " habits.")
    }

    override fun onDestroy() {
        habits = emptyList()
    }

    override fun getCount(): Int = habits.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= habits.size) return RemoteViews(context.packageName, R.layout.widget_habit_item)
        val habitWithStatus = habits[position]
        val habit = habitWithStatus.habit

        val views = RemoteViews(context.packageName, R.layout.widget_habit_item)
        
        views.setTextViewText(R.id.widget_item_name, habit.name)
        if (habitWithStatus.isCompletedToday) {
            views.setTextColor(R.id.widget_item_name, Color.GRAY)
        } else {
            views.setTextColor(R.id.widget_item_name, Color.WHITE)
        }

        if (habitWithStatus.currentStreak > 0) {
            views.setTextViewText(R.id.widget_item_streak, habitWithStatus.currentStreak.toString())
        } else {
            views.setTextViewText(R.id.widget_item_streak, "")
        }

        val accentColor = try {
            Color.parseColor(habit.colorTag)
        } catch (e: Exception) {
            Color.parseColor("#4CAF50")
        }
        views.setInt(R.id.widget_item_sidebar, "setBackgroundColor", accentColor)

        val iconRes = if (habitWithStatus.isCompletedToday) {
            com.mishr.habittracker.feature.habits.R.drawable.ic_widget_check
        } else {
            com.mishr.habittracker.feature.habits.R.drawable.ic_widget_circle
        }
        views.setImageViewResource(R.id.widget_item_checkbox, iconRes)
        views.setInt(R.id.widget_item_checkbox, "setColorFilter", if (habitWithStatus.isCompletedToday) Color.GRAY else accentColor)

        val fillInIntent = Intent().apply {
            data = android.net.Uri.parse("habittracker://widget/toggle/" + habit.id)
            // putExtra removed, relying on URI
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_checkbox, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = habits.getOrNull(position)?.habit?.id?.hashCode()?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}




