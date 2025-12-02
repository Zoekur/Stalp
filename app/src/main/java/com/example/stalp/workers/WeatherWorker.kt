package com.example.stalp.workers

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.stalp.data.WeatherRepository
import com.example.stalp.widget.LinearClockWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WeatherWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = WeatherRepository(context)

            // Fetch settings to determine location source
            val settings = repository.locationSettingsFlow.first()

            // Mock weather fetching logic
            val temp: Int
            val precipChance: Int
            val locationName: String

            if (settings.useCurrentLocation) {
                // Simulating GPS location weather (random but consistent for "local")
                temp = (-5..25).random()
                precipChance = (0..50).random()
                locationName = "Din Plats"
            } else {
                 // Simulating Manual location weather
                 if (settings.manualLocationName.isNotBlank()) {
                     // Generate "deterministic" random based on name length to simulate different weather for different cities
                     val seed = settings.manualLocationName.length
                     temp = (seed % 30)
                     precipChance = (seed * 10 % 100)
                     locationName = settings.manualLocationName
                 } else {
                     temp = 20
                     precipChance = 0
                     locationName = "Väderinformation" // Fallback
                 }
            }
            
            repository.saveWeatherData(temp, precipChance, locationName)

            // Update all widgets
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(LinearClockWidget::class.java)
            
            glanceIds.forEach { glanceId ->
                // Trigger widget update. 
                LinearClockWidget.update(context, glanceId)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "WeatherWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Enforce the minimum periodic interval to prevent crashes
            val repeatInterval = maxOf(30.toLong(), PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MINUTES)

            val workRequest = PeriodicWorkRequestBuilder<WeatherWorker>(repeatInterval, TimeUnit.MINUTES)
                .setConstraints(constraints)
                // Add an initial delay to improve system health
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                // Use KEEP to avoid rescheduling if the work already exists and is unchanged
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
