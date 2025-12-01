package com.example.stalp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.stalp.data.CalendarRepository
import com.example.stalp.data.DayEvent
import com.example.stalp.data.WeatherData
import com.example.stalp.data.WeatherRepository
import com.example.stalp.ui.icons.StalpIcons
import com.example.stalp.ui.settings.SettingsScreen
import com.example.stalp.ui.settings.ThemePreferences
import com.example.stalp.ui.theme.StalpTheme
import com.example.stalp.ui.theme.ThemeOption
import com.example.stalp.ui.theme.ThemeSelector
import com.example.stalp.workers.WeatherWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// -------- HUVUDAKTIVITET --------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Schemalägger väder-workern att starta så snart appen öppnas
        WeatherWorker.schedule(applicationContext)

        val appContext = applicationContext

        setContent {
            val themeOption by ThemePreferences.themeOptionFlow(appContext)
                .collectAsState(initial = ThemeOption.NordicCalm)
            val scope = rememberCoroutineScope()
            var showSettings by remember { mutableStateOf(false) }

            StalpTheme(themeOption = themeOption) { // Återställt tema
                Surface(Modifier.fillMaxSize()) {
                    if (showSettings) {
                        SettingsScreen(onBack = { showSettings = false })
                    } else {
                        LinearClockScreen(
                            themeOption = themeOption,
                            onThemeOptionChange = { option ->
                                scope.launch { ThemePreferences.setThemeOption(appContext, option) }
                            },
                            onSettingsClick = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}

// -------- SKÄRM (Kombinerar tidslinje & kort) --------
@Composable
fun LinearClockScreen(
    themeOption: ThemeOption,
    onThemeOptionChange: (ThemeOption) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val weatherRepository = remember { WeatherRepository(context) }
    val calendarRepository = remember { CalendarRepository(context) }

    // Samlar in väderdata från DataStore i realtid
    val weatherData by weatherRepository.weatherDataFlow.collectAsState(initial = WeatherData())

    // Events state
    var events by remember { mutableStateOf(emptyList<DayEvent>()) }

    // Permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                scope.launch {
                    events = calendarRepository.getEventsForToday()
                }
            }
        }
    )

    // Check permission and fetch events
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            events = calendarRepository.getEventsForToday()
        } else {
            launcher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    val now by rememberTicker1s()
    // Removed digital clock label as requested

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding() // Fixar "black bar" problemet genom att undvika system bars
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP HEADER: Title (Left) + Settings (Right)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stalp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = StalpIcons.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Theme Selector (Optional placement)
        ThemeSelector(
            selectedOption = themeOption,
            onOptionSelected = onThemeOptionChange,
        )

        Spacer(Modifier.height(24.dp)) // Increased spacing since clock is gone

        // 1. Tidslinjen (Huvudkomponenten) - Nu med dubbel höjd och hela dygnet
        LinearDayCard(
            now = now.toLocalTime(),
            height = 168.dp, // Dubblat från 84.dp
            events = events // Passar events till tidslinjen
        )

        Spacer(Modifier.height(16.dp))

        // 2. Nästa Händelse (Tilläggsinformation)
        NextEventCard(events = events, now = now.toLocalTime())

        Spacer(Modifier.height(24.dp)) // Mer utrymme innan korten

        // 3. Väder- och Klädrådsrutor (Från din skiss)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Väderinformationsruta (Vänster)
            WeatherInfoCard(modifier = Modifier.weight(1f), data = weatherData)

            // Klädrådsruta (Höger)
            ClothingAdviceCard(modifier = Modifier.weight(1f), data = weatherData)
        }

        Spacer(Modifier.height(16.dp))

        // Version info
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = 0.5f)
        )
    }
}

// ----------------------------------------------------------
// 1. TIDSLINJE KOMPONENTER
// ----------------------------------------------------------

@Composable
fun LinearDayCard(
    now: LocalTime,
    height: Dp = 160.dp,
    events: List<DayEvent> = emptyList() // Tog emot events
) {
    val hourLabelPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = 36f // Större text
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    val hourLabelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    SideEffect { hourLabelPaint.color = hourLabelColor }

    val corner = 28.dp

    // Theme colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val trackBgColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val passedColor = Color(0xFFB7EA27)
    val nowColor = Color(0xFFEF4444)
    val eventColor = Color(0xFFE0E0E0) // Ljusgrå för events på tidslinjen

    Box(
        Modifier
            .fillMaxWidth()
            .height(height + 24.dp)
            .background(Color.Transparent)
    ) {
        // Yttre kapsel (Bakgrund)
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(height)
                .background(surfaceColor, RoundedCornerShape(corner))
        )

        // Canvas för tidslinje (00 - 24)
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(height)
        ) {
            val pad = 20.dp.toPx()
            val trackTop = size.height * 0.10f // Lite marginal i toppen
            val trackHeight = size.height * 0.80f // Använd det mesta av höjden
            val right = size.width - pad
            val trackWidth = right - pad
            val left = pad

            // Inre kapsel (Bakgrund)
            drawRoundRect(
                color = trackBgColor,
                topLeft = Offset(left, trackTop),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Beräkna pixlar per minut för HELA dygnet (24h = 1440 min)
            val totalMinutes = 24 * 60
            val pxPerMin = trackWidth / totalMinutes

            // Rita events (Som små markörer eller block i bakgrunden)
            events.forEach { event ->
                val startMin = event.start.hour * 60 + event.start.minute
                val endMin = (event.end?.hour ?: 0) * 60 + (event.end?.minute ?: 0)

                // Enkel hantering av events som går över midnatt eller saknar slut -> visa 1h
                val actualEndMin = if (event.end != null && endMin > startMin) endMin else startMin + 60

                val eventStartPx = left + (startMin * pxPerMin)
                val eventWidthPx = (actualEndMin - startMin) * pxPerMin

                // Rita event som ett färgat block (svagt)
                drawRect(
                    color = event.color.copy(alpha = 0.3f), // Använd eventets färg, transparent
                    topLeft = Offset(eventStartPx, trackTop),
                    size = Size(eventWidthPx, trackHeight)
                )
            }

            // Nuvarande tid i minuter
            val currentMinutes = now.hour * 60 + now.minute
            val currentX = left + (currentMinutes * pxPerMin)

            // Grön fyllnad (Från 00:00 till nu)
            val passedWidth = currentX - left
            if (passedWidth > 0) {
                 drawRoundRect(
                    color = passedColor,
                    topLeft = Offset(left, trackTop),
                    size = Size(passedWidth, trackHeight),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                 if (passedWidth > 24f) {
                     drawRect(
                         color = passedColor,
                         topLeft = Offset(currentX - 10f, trackTop),
                         size = Size(10f, trackHeight)
                     )
                 }
            }

            // Inre kapsel (Border - ritas ovanpå för snyggare kant)
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(left, trackTop),
                size = Size(trackWidth, trackHeight),
                style = Stroke(width = 2f),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Loopa igenom 24 timmar
            for (h in 0..24) {
                val min = h * 60
                val x = left + (min * pxPerMin)

                // Rita Tim-markering (långt streck)
                drawLine(
                    color = tickColor,
                    start = Offset(x, trackTop),
                    end = Offset(x, trackTop + trackHeight * 0.4f), // 40% av höjden
                    strokeWidth = 2f
                )

                // Text: Endast för 6, 12, 18, 24
                if (h % 6 == 0 && h != 0) {
                    val label = h.toString()
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x,
                        trackTop + trackHeight / 2f + 18f, // Centrerat vertikalt ungefär
                        hourLabelPaint
                    )
                }

                // Halvtimmar (korta streck) - men inte efter 24
                if (h < 24) {
                    val halfMin = min + 30
                    val halfX = left + (halfMin * pxPerMin)
                    drawLine(
                        color = tickColor.copy(alpha = 0.3f), // Svagare
                        start = Offset(halfX, trackTop),
                        end = Offset(halfX, trackTop + trackHeight * 0.2f), // Kortare (20%)
                        strokeWidth = 2f
                    )
                }
            }

            // Nu-markör (röd linje)
            drawLine(
                color = nowColor,
                start = Offset(currentX, trackTop),
                end = Offset(currentX, trackTop + trackHeight),
                strokeWidth = 4f,
                cap = StrokeCap.Square
            )
        }
    }
}

// -------- NÄSTA HÄNDELSE --------
@Composable
fun NextEventCard(events: List<DayEvent>, now: LocalTime) {
    val sortedEvents = remember(events) { events.sortedBy { it.start } }
    val next = remember(sortedEvents, now) {
        // Hitta nästa händelse som inte har passerat (minus 1 minut för att hantera tickern)
        sortedEvents.firstOrNull { !it.start.isBefore(now.minusMinutes(1)) }
    } ?: return

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(next.icon ?: "•", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
        Column {
            Text(next.title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "${next.start.format(DateTimeFormatter.ofPattern("HH:mm"))}${
                    next.end?.let {
                        " – ${
                            it.format(
                                DateTimeFormatter.ofPattern("HH:mm")
                            )
                        }"
                    } ?: ""
                }",
                color = Color(0xFF6B7280),
                fontSize = 14.sp
            )
        }
    }
}

// ----------------------------------------------------------
// 2. VÄDER-KOMPONENT (Uppdaterad för att ta emot data)
// ----------------------------------------------------------

@Composable
fun WeatherInfoCard(modifier: Modifier = Modifier, data: WeatherData) {
    Card(
        modifier = modifier.height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!data.isDataLoaded) {
                // Laddningsindikator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Laddar väderdata...", fontSize = 14.sp, color = Color.Gray)
                }
            } else {
                // Visar riktig data
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Väderinformation",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${data.temperatureCelsius}°C",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${data.precipitationChance}% risk för nederbörd",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------
// 3. KLÄDRÅDS-KOMPONENT (Uppdaterad för att ta emot data)
// ----------------------------------------------------------

@Composable
fun ClothingAdviceCard(modifier: Modifier = Modifier, data: WeatherData) {
    Card(
        modifier = modifier.height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!data.isDataLoaded) {
                // Laddningsindikator (synkroniserad med väderkortet)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(data.adviceIcon, fontSize = 48.sp)
                    Text(
                        data.adviceText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                // Visar klädråd baserat på logik i WeatherRepository
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Klädråd",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(data.adviceIcon, fontSize = 48.sp)
                    Text(
                        data.adviceText,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// -------- HJÄLPARE --------

// -------- ENKEL TICKER --------
@Composable
fun rememberTicker1s(): State<LocalDateTime> {
    val state = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            // Använder 60 sekunder delay för att minska batterianvändningen
            delay(60000)
            state.value = LocalDateTime.now()
        }
    }
    return state
}
