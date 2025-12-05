package com.dagsbalken.app.data

import android.content.Context
import com.dagsbalken.app.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class WeatherProvider(val displayName: String) {
    OPEN_METEO("Open-Meteo"),
    SMHI("SMHI"),
    SIMULATED("Simulerad")
}

data class WeatherData(
    val temperatureCelsius: Int = 0,
    val precipitationChance: Int = 0,
    val locationName: String = "",
    val isDataLoaded: Boolean = false,
    val adviceText: String = "Laddar...",
) {
    fun getClothingResourceId(): Int {
        val temp = temperatureCelsius
        val precip = precipitationChance
        if (precip > 50) {
            return if (temp < 10) R.drawable.ic_clothing_rain_cold else R.drawable.ic_clothing_rain_warm
        } else if (temp < 0) {
            return R.drawable.ic_clothing_winter
        } else if (temp < 10) {
            return R.drawable.ic_clothing_cold
        } else if (temp < 20) {
            return R.drawable.ic_clothing_warm
        } else {
            return R.drawable.ic_clothing_warm // Defaulting to warm
        }
    }
}

class WeatherRepository(private val context: Context) {
    val weatherDataFlow: Flow<WeatherData> = context.dataStore.data.map { p ->
        WeatherData(
            temperatureCelsius = p[WeatherPreferencesKeys.TEMPERATURE] ?: 0,
            precipitationChance = p[WeatherPreferencesKeys.PRECIPITATION] ?: 0,
            locationName = p[WeatherPreferencesKeys.LOCATION_NAME] ?: "Okänd plats",
            isDataLoaded = p[WeatherPreferencesKeys.IS_LOADED] ?: false
        )
    }

    // ... (rest of the class)
}