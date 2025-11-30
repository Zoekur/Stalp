package com.example.stalp.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.stalp.data.WeatherRepository
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class WeatherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Simulera hämtning av väder
            val temp = Random.nextInt(-10, 35)
            val precip = Random.nextInt(0, 100)

            val repository = WeatherRepository(applicationContext)
            repository.saveWeatherData(temp, precip)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "weather_worker"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WeatherWorker>(
                1, TimeUnit.HOURS // Uppdatera varje timme
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
