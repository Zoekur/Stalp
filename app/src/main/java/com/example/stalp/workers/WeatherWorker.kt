package com.example.stalp.workers

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder // Denna saknades troligen eller användes fel
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.stalp.data.WeatherRepository
import com.example.stalp.widget.LinearClockWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class WeatherWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Hämta/Simulera data
            val temp = (-10..35).random()
            val precipChance = (0..100).random()

            // Spara till repository
            val repository = WeatherRepository(context)
            repository.saveWeatherData(temp, precipChance)

            // Uppdatera widgeten
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(LinearClockWidget::class.java)

            glanceIds.forEach { glanceId ->
                LinearClockWidget.update(context, glanceId)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "weather_fetch_worker_main"

        fun schedule(context: Context) {
            // Korrekt användning av PeriodicWorkRequestBuilder
            val req = PeriodicWorkRequestBuilder<WeatherWorker>(1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    req
                )
        }
    }
}