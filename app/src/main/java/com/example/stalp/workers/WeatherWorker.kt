package com.example.stalp.workers

import android.content.Context
import androidx.work.*
import androidx.work.CoroutineWorker
import com.example.stalp.data.WeatherProvider
import com.example.stalp.data.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object WeatherWorker {
    private const val UNIQUE_WORK_NAME = "stalp_weather_refresh"

    fun schedule(context: Context) {
        val work = PeriodicWorkRequestBuilder<RefreshWeatherWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(0, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, work)
    }

    fun refreshNow(context: Context) {
        val one = OneTimeWorkRequestBuilder<RefreshWeatherWorker>().build()
        WorkManager.getInstance(context).enqueue(one)
    }
}

class RefreshWeatherWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repo = WeatherRepository(applicationContext)
            val provider = repo.providerFlow.first()

            // Simple mock/fallback data. In a real app you'd call a network API here.
            val (temp, precip, location) = when (provider) {
                WeatherProvider.OPEN_METEO -> Triple(Random.nextInt(-5, 26), Random.nextInt(0, 81), "Open-Meteo")
                WeatherProvider.SMHI -> Triple(Random.nextInt(-10, 21), Random.nextInt(10, 91), "SMHI")
                WeatherProvider.SIMULATED -> Triple(Random.nextInt(5, 31), Random.nextInt(0, 51), "Simulerad")
            }

            repo.saveWeatherData(temp, precip, location)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

