package com.example.stalp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import java.io.IOException
import androidx.compose.ui.graphics.Color // Behövs för DayEvent
import java.time.LocalTime // Behövs för DayEvent

// -------- DATAKLASS FÖR TIDSLINJE-EVENT --------
data class DayEvent(
    val id: String,
    val title: String,
    val start: LocalTime,
    val end: LocalTime? = null,
    val icon: String? = null,
    val color: Color = Color(0xFF6AA6FF)
)

// --- DataStore Setup ---
// Skapar en singleton DataStore för att lagra väderdata för huvudappen
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "main_app_weather")

// Nycklar för DataStore
object WeatherPreferencesKeys {
    val TEMP_CELSIUS = intPreferencesKey("temp_c")
    val PRECIP_CHANCE = intPreferencesKey("precip_chance_pct")
    val IS_COLD_ADVICE = stringPreferencesKey("advice_icon") // Sparar ikonen
    val ADVICE_TEXT = stringPreferencesKey("advice_text") // Sparar rådtexten
    val DATA_LOADED = booleanPreferencesKey("data_loaded")
}

// Data class för att representera väderdata i Compose
data class WeatherData(
    val temperatureCelsius: Int = 0,
    val precipitationChance: Int = 0, // i procent (0-100)
    val adviceIcon: String = "☁️",
    val adviceText: String = "Laddar väderdata...",
    val isDataLoaded: Boolean = false
)

// Konstanter för klädrådslogik (enligt din skiss)
object ClothingAdvice {
    // Kallt: Rekommenderar varma kläder vid denna temp eller kallare
    const val COLD_THRESHOLD_C = 5
    // Varmt: Rekommenderar lätta kläder vid denna temp eller varmare
    const val HOT_THRESHOLD_C = 25
    // Regn: Rekommenderar paraply vid denna risk eller högre
    const val PRECIPITATION_THRESHOLD_PCT = 30
}

// --- Repository (Logiken för att läsa/skriva data) ---
class WeatherRepository(private val context: Context) {
    private val dataStore = context.dataStore

    // Flow som tillhandahåller väderdata i realtid till Compose
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

    // Skriver ny väderdata till DataStore
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

    // Kärnlogiken för klädråd
    private fun generateClothingAdvice(temp: Int, precipChance: Int): Pair<String, String> {
        return when {
            // Varma kläder: -5 grader eller lägre (enligt skissen)
            temp <= ClothingAdvice.COLD_THRESHOLD_C -> Pair(
                "Rekommenderar varma kläder: Jacka, mössa, handskar.",
                "🧥🧣🧤"
            )
            // Lätta kläder: +30 grader eller högre (enligt skissen)
            temp > ClothingAdvice.HOT_THRESHOLD_C -> Pair(
                "Välj lätta kläder: Shorts och linne.",
                "🩳👕☀️"
            )
            // Regn
            precipChance >= ClothingAdvice.PRECIPITATION_THRESHOLD_PCT -> Pair(
                "Hög risk för nederbörd (${precipChance}%). Ta med paraply eller regnjacka!",
                "☔️🌧️"
            )
            // Normalt
            else -> Pair(
                "Lätt jacka eller tröja är lagom.",
                "👚"
            )
        }
    }
}