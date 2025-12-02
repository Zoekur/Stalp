package com.example.stalp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

// --- DataStore Setup ---
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "main_app_weather")

// Nycklar för DataStore
object WeatherPreferencesKeys {
    val TEMP_CELSIUS = intPreferencesKey("temp_c")
    val PRECIP_CHANCE = intPreferencesKey("precip_chance_pct")
    val IS_COLD_ADVICE = stringPreferencesKey("advice_icon")
    val ADVICE_TEXT = stringPreferencesKey("advice_text")
    val DATA_LOADED = booleanPreferencesKey("data_loaded")
}

// Data class för UI
data class WeatherData(
    val temperatureCelsius: Int = 0,
    val precipitationChance: Int = 0,
    val adviceIcon: String = "☁️",
    val adviceText: String = "Laddar väderdata...",
    val isDataLoaded: Boolean = false
)

object ClothingAdvice {
    const val COLD_THRESHOLD_C = 5
    const val HOT_THRESHOLD_C = 25
    const val PRECIPITATION_THRESHOLD_PCT = 30
}

class WeatherRepository(private val context: Context) {
    private val dataStore = context.dataStore

    val weatherDataFlow = dataStore.data
        .map { prefs ->
            WeatherData(
                temperatureCelsius = prefs[WeatherPreferencesKeys.TEMP_CELSIUS] ?: 15,
                precipitationChance = prefs[WeatherPreferencesKeys.PRECIP_CHANCE] ?: 0,
                adviceIcon = prefs[WeatherPreferencesKeys.IS_COLD_ADVICE] ?: "☁️",
                adviceText = prefs[WeatherPreferencesKeys.ADVICE_TEXT] ?: "Väntar på data...",
                isDataLoaded = prefs[WeatherPreferencesKeys.DATA_LOADED] ?: false
            )
        }

    suspend fun saveWeatherData(temp: Int, precipChance: Int) {
        val (adviceText, adviceIcon) = generateClothingAdvice(temp, precipChance)

        dataStore.edit { prefs ->
            prefs[WeatherPreferencesKeys.TEMP_CELSIUS] = temp
            prefs[WeatherPreferencesKeys.PRECIP_CHANCE] = precipChance
            prefs[WeatherPreferencesKeys.ADVICE_TEXT] = adviceText
            prefs[WeatherPreferencesKeys.IS_COLD_ADVICE] = adviceIcon
            prefs[WeatherPreferencesKeys.DATA_LOADED] = true
        }
    }

    private fun generateClothingAdvice(temp: Int, precipChance: Int): Pair<String, String> {
        return when {
            temp <= ClothingAdvice.COLD_THRESHOLD_C -> Pair(
                "Rekommenderar varma kläder: Jacka, mössa, handskar.",
                "🧥🧣🧤"
            )
            temp > ClothingAdvice.HOT_THRESHOLD_C -> Pair(
                "Välj lätta kläder: Shorts och linne.",
                "🩳👕☀️"
            )
            precipChance >= ClothingAdvice.PRECIPITATION_THRESHOLD_PCT -> Pair(
                "Hög risk för nederbörd (${precipChance}%). Ta med paraply!",
                "☔️🌧️"
            )
            else -> Pair(
                "Lätt jacka eller tröja är lagom.",
                "👚"
            )
        }
    }
}