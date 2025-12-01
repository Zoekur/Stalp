package com.example.stalp.widget

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.Intent

class LinearClockWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // update widget views
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }
}