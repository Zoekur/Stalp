package com.example.stalp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stalp.data.CalendarRepository
import com.example.stalp.data.DayEvent
import com.example.stalp.data.WeatherData
import com.example.stalp.data.WeatherLocationSettings
import com.example.stalp.data.WeatherRepository
import com.example.stalp.ui.MainViewModel
import com.example.stalp.ui.icons.StalpIcons
import com.example.stalp.ui.settings.SettingsScreen
import com.example.stalp.ui.settings.ThemePreferences
import com.example.stalp.ui.theme.StalpTheme
import com.example.stalp.ui.theme.ThemeOption
import com.example.stalp.workers.WeatherWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val themePrefs = ThemePreferences(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(themePrefs) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WeatherWorker.schedule(applicationContext)

        setContent {
            val themeOption by viewModel.themeOptionFlow.collectAsState(initial = ThemeOption.NordicCalm)
            StalpTheme(themeOption = themeOption) {
                Surface(Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            LinearClockScreen(
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                currentTheme = themeOption,
                                onThemeSelected = { newTheme -> viewModel.onThemeOptionChange(newTheme) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinearClockScreen(onSettingsClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val weatherRepository = remember { WeatherRepository(context) }
    val calendarRepository = remember { CalendarRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val weatherData by weatherRepository.weatherDataFlow.collectAsState(initial = WeatherData())
    val locationSettings by weatherRepository.locationSettingsFlow.collectAsState(initial = WeatherLocationSettings())
    var events by remember { mutableStateOf(emptyList<DayEvent>()) }

    fun loadEvents() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            scope.launch { events = calendarRepository.getEventsForToday() }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.READ_CALENDAR] == true) loadEvents()
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                scope.launch { weatherRepository.saveLocationSettings(true, locationSettings.manualLocationName) }
            }
        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) loadEvents() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CALENDAR)
        } else {
            loadEvents()
        }
        if (locationSettings.useCurrentLocation) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        if (permissionsToRequest.isNotEmpty()) permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    val now by rememberTicker1s()

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Stalp", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            IconButton(onClick = onSettingsClick) {
                Icon(StalpIcons.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(32.dp))
        LinearDayCard(now = now.toLocalTime(), height = 168.dp, events = events)
        Spacer(Modifier.height(16.dp))
        NextEventCard(events = events, now = now.toLocalTime())
        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WeatherInfoCard(modifier = Modifier.weight(1f), weather = weatherData, onRefresh = { WeatherWorker.refreshNow(context) })
            ClothingAdviceCard(modifier = Modifier.weight(1f), weather = weatherData)
        }
    }
}

@Composable
fun LinearDayCard(now: LocalTime, height: Dp = 160.dp, events: List<DayEvent> = emptyList()) {
    // VIKTIGT: Svart färg på texten för att garantera att den syns mot ljus bakgrund
    val hourLabelPaint = remember { Paint().apply { textAlign = Paint.Align.CENTER; textSize = 54f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD; color = android.graphics.Color.BLACK } }

    val dayGradient = Brush.horizontalGradient(listOf(Color(0xFFFFE082), Color(0xFF81C784), Color(0xFF4FC3F7), Color(0xFF5E35B1)))
    // Bakgrunden är vit/ljusgrå
    val surfaceColor = Color(0xFFF0F0F0)
    val trackBgColor = Color(0xFFE0E0E0)
    // Svarta linjer för kontrast
    val borderColor = Color.Black.copy(alpha = 0.5f)
    val tickColor = Color.Black.copy(alpha = 0.7f)
    val nowColor = Color(0xFFEF4444)

    Box(Modifier.fillMaxWidth().height(height + 24.dp)) {
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(height).background(surfaceColor, RoundedCornerShape(28.dp)))

        Canvas(modifier = Modifier.align(Alignment.TopCenter).fillMaxSize()) { // FillMaxSize för att täcka boxen
            val pad = 20.dp.toPx()
            val trackHeight = size.height * 0.80f
            val trackWidth = size.width - (pad * 2)
            val pxPerMin = trackWidth / (24 * 60)
            val trackTop = size.height * 0.10f

            // Rita bakgrundsspåret
            drawRoundRect(trackBgColor, Offset(pad, trackTop), Size(trackWidth, trackHeight), CornerRadius(24f, 24f))

            // Rita Events
            events.forEach { event ->
                val startPx = pad + ((event.start.hour * 60 + event.start.minute) * pxPerMin)
                val widthPx = ((if (event.end != null && event.end.isAfter(event.start)) (event.end.hour * 60 + event.end.minute) else (event.start.hour * 60 + event.start.minute + 60)) - (event.start.hour * 60 + event.start.minute)) * pxPerMin
                drawRect(event.color.copy(alpha = 0.3f), Offset(startPx, trackTop), Size(widthPx, trackHeight))
            }

            // Rita passerad tid (Gradient)
            val currentX = pad + ((now.hour * 60 + now.minute) * pxPerMin)
            if (currentX > pad) {
                drawRoundRect(dayGradient, Offset(pad, trackTop), Size(currentX - pad, trackHeight), CornerRadius(24f, 24f))
            }

            // Ram
            drawRoundRect(borderColor, Offset(pad, trackTop), Size(trackWidth, trackHeight), CornerRadius(24f, 24f), Stroke(2f))

            // Linjer och text
            for (h in 0..24) {
                val x = pad + (h * 60 * pxPerMin)
                drawLine(tickColor, Offset(x, trackTop), Offset(x, trackTop + trackHeight * 0.4f), 2f)
                if (h % 6 == 0 && h != 0 && h != 24) {
                    drawContext.canvas.nativeCanvas.drawText(h.toString(), x, trackTop + trackHeight / 2f + 18f, hourLabelPaint)
                }
            }
            // Nu-linjen
            drawLine(nowColor, Offset(currentX, trackTop), Offset(currentX, trackTop + trackHeight), 4f, StrokeCap.Square)
        }
    }
}

@Composable
fun NextEventCard(events: List<DayEvent>, now: LocalTime) {
    val next = events.sortedBy { it.start }.firstOrNull { !it.start.isBefore(now.minusMinutes(1)) } ?: return
    Row(Modifier.fillMaxWidth().background(Color(0xFF808080), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(next.icon ?: "•", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp), color = Color.Black)
        Column {
            Text(next.title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            Text("${next.start.format(DateTimeFormatter.ofPattern("HH:mm"))}${next.end?.let { " – ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}" } ?: ""}", color = Color(0xFF6B7280), fontSize = 14.sp)
        }
    }
}

@Composable
fun WeatherInfoCard(modifier: Modifier = Modifier, weather: WeatherData, onRefresh: () -> Unit) {
    Card(modifier = modifier.height(200.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = onRefresh, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Uppdatera", tint = Color.Black)
            }
            Column(modifier = Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!weather.isDataLoaded) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Laddar...", fontSize = 14.sp, color = Color.Gray)
                } else {
                    Text(weather.locationName, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Color.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("${weather.temperatureCelsius}°C", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("${weather.precipitationChance}% risk", fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ClothingAdviceCard(modifier: Modifier = Modifier, weather: WeatherData) {
    Card(modifier = modifier.height(200.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            if (!weather.isDataLoaded) {
                Text("...", fontSize = 24.sp, color = Color.Black)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Klädråd", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    Spacer(Modifier.height(16.dp))
                    // HÄR ÄR BILDEN TILLBAKA!
                    Image(
                        painter = painterResource(id = weather.getClothingResourceId()),
                        contentDescription = "Kläder",
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(weather.adviceText, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun rememberTicker1s(): State<LocalDateTime> {
    val state = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { state.value = LocalDateTime.now(); delay(60000) } }
    return state
}