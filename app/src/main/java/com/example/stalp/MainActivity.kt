package com.example.stalp

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.stalp.ui.icons.StalpIcons
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
import android.content.pm.PackageManager
import androidx.compose.ui.unit.sp
import com.example.stalp.data.DayEvent
import com.example.stalp.data.WeatherData
import com.example.stalp.data.WeatherRepository
import com.example.stalp.ui.settings.SettingsScreen
import com.example.stalp.ui.settings.ThemePreferences
import com.example.stalp.ui.theme.ThemeOption
import com.example.stalp.ui.theme.ThemeSelector
import com.example.stalp.ui.theme.StalpTheme
import com.example.stalp.workers.WeatherWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max


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
    val weatherRepository = remember { WeatherRepository(context) }

    // Samlar in väderdata från DataStore i realtid
    val weatherData by weatherRepository.weatherDataFlow.collectAsState(initial = WeatherData())

    val now by rememberTicker1s()
    val timeLabel = now.format(DateTimeFormatter.ofPattern("HH:mm"))

    // Exempeldata för events
    val events = remember {
        listOf(
            DayEvent(
                "sleep",
                "Sömn",
                LocalTime.of(0, 30),
                LocalTime.of(6, 45),
                color = Color(0xFF334155)
            ),
            DayEvent(
                "commute",
                "Pendling",
                LocalTime.of(8, 0),
                LocalTime.of(8, 30),
                color = Color(0xFF6B7280)
            ),
            DayEvent(
                "standup",
                "Standup (Teams)",
                LocalTime.of(9, 45),
                LocalTime.of(10, 0),
                color = Color(0xFF22C55E)
            ),
            DayEvent(
                "lunch",
                "Lunch",
                LocalTime.of(12, 0),
                LocalTime.of(12, 45),
                icon = "🍽️",
                color = Color(0xFFFDE047)
            ),
            DayEvent(
                "focus",
                "Fokusblock",
                LocalTime.of(13, 15),
                LocalTime.of(15, 0),
                color = Color(0xFF38BDF8)
            ),
            DayEvent(
                "gym",
                "Träning",
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                icon = "🏋️",
                color = Color(0xFFA78BFA)
            )
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) {
            // Theme Selector centered
            Box(Modifier.align(Alignment.Center)) {
                ThemeSelector(
                    selectedOption = themeOption,
                    onOptionSelected = onThemeOptionChange,
                )
            }

            // Settings Button top-right
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(StalpIcons.Settings, contentDescription = "Settings")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Huvudklocka
        Text(
            text = timeLabel,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(16.dp))

        // 1. Tidslinjen (Huvudkomponenten)
        LinearDayCard(
            now = now.toLocalTime(),
            height = 84.dp
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
    height: Dp = 80.dp
) {
    val hourLabelPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = 24f
            isAntiAlias = true
        }
    }
    val hourLabelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    SideEffect { hourLabelPaint.color = hourLabelColor }

    val corner = 28.dp

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
                .background(Color.White, RoundedCornerShape(corner))
        )

        // Canvas för tidslinje
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(height)
        ) {
            val pad = 20.dp.toPx()
            val trackTop = size.height * 0.18f
            val trackHeight = size.height * 0.64f
            val right = size.width - pad
            val trackWidth = right - pad
            val centerX = pad + trackWidth / 2f

            // Inre kapsel (Bakgrund)
            drawRoundRect(
                color = Color(0xFFF8F8F8),
                topLeft = Offset(pad, trackTop),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Zoom-inställningar (visa +/- 2 timmar)
            val hoursToDisplay = 2
            val minutesWindow = hoursToDisplay * 60f
            // Pixlar per minut: Halva bredden representerar 2 timmar (120 min)
            val pxPerMin = (trackWidth / 2f) / minutesWindow

            // Grön fyllnad (Dåtid = vänster halva, fram till 'nu' i mitten)
            drawRoundRect(
                color = Color(0xFFB7EA27),
                topLeft = Offset(pad, trackTop),
                size = Size(trackWidth / 2f, trackHeight),
                cornerRadius = CornerRadius(24f, 24f)
            )
            // Fix: Rita över högra halvan av den gröna rektangeln med en rät kant
            drawRect(
                color = Color(0xFFB7EA27),
                topLeft = Offset(pad + 24f, trackTop),
                size = Size((trackWidth / 2f) - 24f, trackHeight)
            )

            // Inre kapsel (Border - ritas ovanpå)
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.75f),
                topLeft = Offset(pad, trackTop),
                size = Size(trackWidth, trackHeight),
                style = Stroke(width = 2f),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Timetiketter
            val currentHour = now.hour
            val currentMinute = now.minute

            // Iterera offset från -3 till +3 timmar
            for (offset in -3..3) {
                val targetHour = currentHour + offset
                val labelVal = (targetHour % 24 + 24) % 24
                val label = String.format("%02d", labelVal)

                // Skillnad i minuter från 'nu'
                val diffMinutes = (offset * 60) - currentMinute
                val x = centerX + (diffMinutes * pxPerMin)

                if (x >= pad && x <= pad + trackWidth) {
                    // Rita tick
                    drawLine(
                        color = Color.Black.copy(alpha = 0.5f),
                        start = Offset(x, trackTop),
                        end = Offset(x, trackTop + trackHeight * 0.3f),
                        strokeWidth = 2f
                    )

                    // Rita text
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x,
                        trackTop + trackHeight / 2f + 12f,
                        hourLabelPaint
                    )
                }
            }

            // Nu-markör (röd linje i mitten)
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(centerX, trackTop),
                end = Offset(centerX, trackTop + trackHeight),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${data.temperatureCelsius}°C",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        color = Color.Black
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