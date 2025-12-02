package com.example.stalp.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.stalp.data.GeocodingService
import com.example.stalp.data.SearchResult
import com.example.stalp.data.WeatherLocationSettings
import com.example.stalp.data.WeatherProvider
import com.example.stalp.data.WeatherRepository
import com.example.stalp.ui.icons.StalpIcons
import com.example.stalp.ui.theme.ThemeOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeOption,
    onThemeSelected: (ThemeOption) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val weatherRepository = remember { WeatherRepository(context) }
    val locationSettings by weatherRepository.locationSettingsFlow.collectAsState(initial = WeatherLocationSettings())
    val selectedProvider by weatherRepository.providerFlow.collectAsState(initial = WeatherProvider.DEFAULT)

    // UI States
    var themeExpanded by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }

    // Sök-states
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSearchResults by remember { mutableStateOf(false) }

    val providers = WeatherProvider.entries

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            // Om nekad, behåll GPS avstängd men krascha inte
            val useGps = granted

            scope.launch {
                // Skicka med lat/lon även här (nollställda eller gamla värden) för att matcha funktionen
                weatherRepository.saveLocationSettings(
                    useGps,
                    locationSettings.manualLocationName,
                    locationSettings.manualLat,
                    locationSettings.manualLon
                )
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inställningar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(StalpIcons.ArrowBack, contentDescription = "Tillbaka")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // --- UTSEENDE ---
            Text("Utseende", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = !themeExpanded }
            ) {
                OutlinedTextField(
                    value = currentTheme.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tema") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = themeExpanded,
                    onDismissRequest = { themeExpanded = false }
                ) {
                    ThemeOption.values().forEach { option ->
                        DropdownMenuItem(text = { Text(option.displayName) }, onClick = { onThemeSelected(option); themeExpanded = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- VÄDER & PLATS ---
            Text("Väder & Plats", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // Väderleverantör
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = !providerExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProvider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Väderleverantör") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    providers.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.displayName) },
                            onClick = {
                                providerExpanded = false
                                scope.launch { weatherRepository.saveProvider(provider) }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plats Toggle (GPS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newValue = !locationSettings.useCurrentLocation
                        if (newValue) {
                            // Vill aktivera GPS
                            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                scope.launch {
                                    weatherRepository.saveLocationSettings(true, locationSettings.manualLocationName, locationSettings.manualLat, locationSettings.manualLon)
                                }
                            } else {
                                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        } else {
                            // Avaktivera GPS
                            scope.launch {
                                weatherRepository.saveLocationSettings(false, locationSettings.manualLocationName, locationSettings.manualLat, locationSettings.manualLon)
                            }
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Använd nuvarande position", style = MaterialTheme.typography.bodyLarge)
                    Text("Hämtar väder för din exakta position", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = locationSettings.useCurrentLocation, onCheckedChange = null)
            }

            // --- SÖK PLATS (Om GPS är av) ---
            if (!locationSettings.useCurrentLocation) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Vald plats: ${locationSettings.manualLocationName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        if (query.length > 2) {
                            scope.launch {
                                isSearching = true
                                searchResults = GeocodingService.searchCity(query)
                                isSearching = false
                                showSearchResults = true
                            }
                        } else {
                            showSearchResults = false
                        }
                    },
                    label = { Text("Sök stad (t.ex. Umeå)") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (showSearchResults && searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .heightIn(max = 200.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            searchResults.forEach { result ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(result.name, style = MaterialTheme.typography.bodyLarge)
                                            Text(result.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        scope.launch {
                                            weatherRepository.saveLocationSettings(
                                                useCurrent = false,
                                                manualName = result.name,
                                                lat = result.lat,
                                                lon = result.lon
                                            )
                                            searchQuery = ""
                                            showSearchResults = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else if (showSearchResults && searchQuery.length > 2 && !isSearching) {
                    Text("Inga platser hittades.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}