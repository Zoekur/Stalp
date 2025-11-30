package com.example.stalp.workers

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.stalp.data.WeatherRepository
import com.example.stalp.widget.LinearClockWidget
import com.example.stalp.widget.LinearClockPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WeatherWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Mock weather fetching logic
            // In a real app, you would make a network call here
            // e.g. using Retrofit to fetch from an API
            
            // Randomly generate some weather data for demo purposes
            // Or hardcode it to match the visualization requirements partially
            val temp = (-10..35).random()
            val precipChance = (0..100).random()
            
            val repository = WeatherRepository(context)
            repository.saveWeatherData(temp, precipChance)

            // Update all widgets
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(LinearClockWidget::class.java)
            
            glanceIds.forEach { glanceId ->
                // Trigger widget update. 
                // Since LinearClockWidget reads from WeatherRepository in its Content (or we will make it so),
                // we just need to trigger a refresh.
                LinearClockWidget.update(context, glanceId)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "weather_worker_periodic"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
