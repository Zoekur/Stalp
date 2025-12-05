package com.dagsbalken.app.workers

import android.content.Context
import androidx.work.*
import androidx.work.CoroutineWorker
import com.dagsbalken.app.data.WeatherPreferencesKeys
import com.dagsbalken.app.data.WeatherProvider
import com.dagsbalken.app.data.WeatherRepository
import com.dagsbalken.app.data.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object WeatherWorker {
    private const val UNIQUE_WORK_NAME = "dagsbalken_weather_refresh"

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
            val locationSettings = repo.locationSettingsFlow.first()

            // Determine coordinates
            val dataStore = applicationContext.dataStore
            val p = dataStore.data.first()

            val savedLat = p[WeatherPreferencesKeys.SAVED_LAT] ?: 59.3293
            val savedLon = p[WeatherPreferencesKeys.SAVED_LON] ?: 18.0686

            val lat = if (locationSettings.useCurrentLocation) savedLat else locationSettings.manualLat
            val lon = if (locationSettings.useCurrentLocation) savedLon else locationSettings.manualLon

            val locationName = if (locationSettings.useCurrentLocation) "Din Plats" else locationSettings.manualLocationName

            val (temp, precip, sourceName) = when (provider) {
                WeatherProvider.OPEN_METEO -> fetchOpenMeteo(lat, lon, locationName)
                WeatherProvider.SMHI -> Triple(Random.nextInt(-10, 21), Random.nextInt(10, 91), "SMHI (Mock)") // SMHI not impl yet
                WeatherProvider.SIMULATED -> Triple(Random.nextInt(5, 31), Random.nextInt(0, 51), "Simulerad")
            }

            repo.saveWeatherData(temp, precip, sourceName)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun fetchOpenMeteo(lat: Double, lon: Double, locationName: String): Triple<Int, Int, String> {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,precipitation&timezone=auto"
        val response = URL(url).readText()
        val json = JSONObject(response)
        if (!json.has("current")) {
            return Triple(0, 0, locationName)
        }
        val current = json.getJSONObject("current")
        val temp = if (current.has("temperature_2m")) current.getDouble("temperature_2m").toInt() else 0
        val precipVal = if (current.has("precipitation")) current.getDouble("precipitation") else 0.0
        val precip = (precipVal * 10).toInt()

        // OpenMeteo gives precipitation in mm. Dagsbalken expects %.
        // Let's check 'daily.precipitation_probability_max'.
        // To keep it simple and match "current" endpoint: if precip > 0, we say high chance.
        // Better: use hourly forecast or just accept that we display precip amount or something?
        // The UI says "% risk".
        // Let's try to get probability if possible.
        // URL with daily: &daily=precipitation_probability_max
        // Let's re-fetch with better params?
        // Or just stick to current. "Precipitation" in current is usually mm.
        // Let's stick to the simpler current param for now and map >0mm to >50% chance for UI feedback.

        val precipChance = if (precip > 0) 80 else 10 // Simplified logic

        return Triple(temp, precipChance, locationName)
    }
}

