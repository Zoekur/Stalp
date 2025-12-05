package com.dagsbalken.app.data

enum class WeatherProvider(val storageValue: String, val displayName: String) {
    OPEN_METEO(storageValue = "open_meteo", displayName = "Open-Meteo"),
    SMHI(storageValue = "smhi", displayName = "SMHI"),
    SIMULATED(storageValue = "simulated", displayName = "Simulerad");

    companion object {
        val DEFAULT: WeatherProvider = OPEN_METEO

        fun fromStorageValue(value: String?): WeatherProvider {
            return entries.firstOrNull { it.storageValue == value } ?: DEFAULT
        }
    }
}
