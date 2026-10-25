package com.mishr.habittracker.feature.habits.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import android.widget.RemoteViews

fun checkGlanceApi(widget: GlanceAppWidget, context: Context) {
    val methods = widget.javaClass.methods.map { it.name }
    println(methods.joinToString(", "))
}
