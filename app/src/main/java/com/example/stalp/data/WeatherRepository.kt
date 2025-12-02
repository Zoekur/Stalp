package com.example.stalp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "main_app_weather")

object WeatherPreferencesKeys {
    val TEMP_CELSIUS = intPreferencesKey("temp_c")
    val PRECIP_CHANCE = intPreferencesKey("precip_chance_pct")
    val ADVICE_TEXT = stringPreferencesKey("advice_text")
    val CLOTHING_TYPE = stringPreferencesKey("clothing_type") // Vi använder denna för att välja bild!
    val DATA_LOADED = booleanPreferencesKey("data_loaded")
    val LOCATION_NAME_DISPLAY = stringPreferencesKey("location_name_display")
    val PROVIDER = stringPreferencesKey("provider")

    val USE_CURRENT_LOCATION = booleanPreferencesKey("use_current_location")
    val MANUAL_LOCATION_NAME = stringPreferencesKey("manual_location_name")
    val MANUAL_LAT = doublePreferencesKey("manual_lat")
    val MANUAL_LON = doublePreferencesKey("manual_lon")

    // Used to store the last known location for the background worker
    val SAVED_LAT = doublePreferencesKey("saved_lat")
    val SAVED_LON = doublePreferencesKey("saved_lon")
}

data class WeatherData(
    val temperatureCelsius: Int = 0,
    val precipitationChance: Int = 0,
    val adviceText: String = "Laddar...",
    val clothingType: String = "NORMAL", // COLD, HOT, RAIN, NORMAL
    val locationName: String = "Plats",
    val isDataLoaded: Boolean = false
) {
    // Hjälpfunktion för att hämta rätt bild-ID
    fun getClothingResourceId(): Int {
        return when (clothingType) {
            "COLD" -> com.example.stalp.R.drawable.ic_clothing_cold
            "HOT" -> com.example.stalp.R.drawable.ic_clothing_hot
            "RAIN" -> com.example.stalp.R.drawable.ic_clothing_rain
            "NORMAL" -> com.example.stalp.R.drawable.ic_clothing_normal
            else -> com.example.stalp.R.drawable.ic_clothing_normal
        }
    }
}

data class WeatherLocationSettings(
    val useCurrentLocation: Boolean = true,
    val manualLocationName: String = "Stockholm",
    val manualLat: Double = 59.3293,
    val manualLon: Double = 18.0686
)

data class SearchResult(val name: String, val lat: Double, val lon: Double, val country: String)

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
                temperatureCelsius = prefs[WeatherPreferencesKeys.TEMP_CELSIUS] ?: 0,
                precipitationChance = prefs[WeatherPreferencesKeys.PRECIP_CHANCE] ?: 0,
                adviceText = prefs[WeatherPreferencesKeys.ADVICE_TEXT] ?: "Väntar på data...",
                clothingType = prefs[WeatherPreferencesKeys.CLOTHING_TYPE] ?: "NORMAL",
                locationName = prefs[WeatherPreferencesKeys.LOCATION_NAME_DISPLAY] ?: "Ingen plats vald",
                isDataLoaded = prefs[WeatherPreferencesKeys.DATA_LOADED] ?: false
            )
        }

    val locationSettingsFlow: Flow<WeatherLocationSettings> = dataStore.data
        .map { prefs ->
            WeatherLocationSettings(
                useCurrentLocation = prefs[WeatherPreferencesKeys.USE_CURRENT_LOCATION] ?: true,
                manualLocationName = prefs[WeatherPreferencesKeys.MANUAL_LOCATION_NAME] ?: "Stockholm",
                manualLat = prefs[WeatherPreferencesKeys.MANUAL_LAT] ?: 59.3293,
                manualLon = prefs[WeatherPreferencesKeys.MANUAL_LON] ?: 18.0686
            )
        }

    val providerFlow: Flow<WeatherProvider> = dataStore.data
        .map { prefs ->
            WeatherProvider.fromStorageValue(prefs[WeatherPreferencesKeys.PROVIDER])
        }
        .onStart { emit(WeatherProvider.DEFAULT) }

    suspend fun saveWeatherData(temp: Int, precipChance: Int, locationName: String) {
        val (adviceText, clothingType) = generateClothingAdvice(temp, precipChance)
        dataStore.edit { prefs ->
            prefs[WeatherPreferencesKeys.TEMP_CELSIUS] = temp
            prefs[WeatherPreferencesKeys.PRECIP_CHANCE] = precipChance
            prefs[WeatherPreferencesKeys.ADVICE_TEXT] = adviceText
            prefs[WeatherPreferencesKeys.CLOTHING_TYPE] = clothingType
            prefs[WeatherPreferencesKeys.LOCATION_NAME_DISPLAY] = locationName
            prefs[WeatherPreferencesKeys.DATA_LOADED] = true
        }
    }

    suspend fun saveLocationSettings(useCurrent: Boolean, manualName: String, lat: Double = 0.0, lon: Double = 0.0) {
        dataStore.edit { prefs ->
            prefs[WeatherPreferencesKeys.USE_CURRENT_LOCATION] = useCurrent
            prefs[WeatherPreferencesKeys.MANUAL_LOCATION_NAME] = manualName
            if (!useCurrent) {
                prefs[WeatherPreferencesKeys.MANUAL_LAT] = lat
                prefs[WeatherPreferencesKeys.MANUAL_LON] = lon
                // Also update saved coordinates for the worker
                prefs[WeatherPreferencesKeys.SAVED_LAT] = lat
                prefs[WeatherPreferencesKeys.SAVED_LON] = lon
            }
        }
    }

    suspend fun saveCurrentLocationCoordinates(lat: Double, lon: Double) {
        dataStore.edit { prefs ->
            prefs[WeatherPreferencesKeys.SAVED_LAT] = lat
            prefs[WeatherPreferencesKeys.SAVED_LON] = lon
        }
    }

    suspend fun saveProvider(provider: WeatherProvider) {
        dataStore.edit { prefs ->
            prefs[WeatherPreferencesKeys.PROVIDER] = provider.storageValue
        }
    }

    private fun generateClothingAdvice(temp: Int, precipChance: Int): Pair<String, String> {
        return when {
            temp <= ClothingAdvice.COLD_THRESHOLD_C -> Pair("Kallt ute! Ta på dig jacka och mössa.", "COLD")
            temp > ClothingAdvice.HOT_THRESHOLD_C -> Pair("Varmt och skönt. Shorts och t-shirt passar bra.", "HOT")
            precipChance >= ClothingAdvice.PRECIPITATION_THRESHOLD_PCT -> Pair("Risk för regn. Glöm inte paraplyet!", "RAIN")
            else -> Pair("Ganska normalt väder. En tröja fungerar fint.", "NORMAL")
        }
    }
}

object GeocodingService {
    private const val BASE_URL = "https://geocoding-api.open-meteo.com/v1/search"
    suspend fun searchCity(query: String): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.length < 2) return@withContext emptyList()
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val response = URL("$BASE_URL?name=$encodedQuery&count=5&language=sv&format=json").readText()
                val json = JSONObject(response)
                if (!json.has("results")) return@withContext emptyList()
                val resultsArray = json.getJSONArray("results")
                val list = mutableListOf<SearchResult>()
                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.getJSONObject(i)
                    list.add(SearchResult(obj.getString("name"), obj.getDouble("latitude"), obj.getDouble("longitude"), if (obj.has("country")) obj.getString("country") else ""))
                }
                list
            } catch (e: Exception) { emptyList() }
        }
    }
}