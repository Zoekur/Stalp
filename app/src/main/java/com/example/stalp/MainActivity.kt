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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stalp.data.DayEvent
import com.example.stalp.data.WeatherData
import com.example.stalp.data.WeatherRepository
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

            StalpTheme(themeOption = themeOption) { // Återställt tema
                Surface(Modifier.fillMaxSize()) {
                    LinearClockScreen(
                        themeOption = themeOption,
                        onThemeOptionChange = { option ->
                            scope.launch { ThemePreferences.setThemeOption(appContext, option) }
                        }
                    )
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
) {
    val context = LocalContext.current
    val weatherRepository = remember { WeatherRepository(context) }

    // Samlar in väderdata från DataStore i realtid
    val weatherData by weatherRepository.weatherDataFlow.collectAsState(initial = WeatherData())

    val now by rememberTicker1s()
    val timeLabel = now.format(DateTimeFormatter.ofPattern("HH:mm"))
    val mins = now.hour * 60 + now.minute
    val pct = mins / (24f * 60f)

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
        ThemeSelector(
            selectedOption = themeOption,
            onOptionSelected = onThemeOptionChange,
        )

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
            progress = pct,
            now = now.toLocalTime(),
            events = events,
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
    }
}

// ----------------------------------------------------------
// 1. TIDSLINJE KOMPONENTER (från föregående kodblock)
// ----------------------------------------------------------

@Composable
fun LinearDayCard(
    progress: Float,
    now: LocalTime,
    events: List<DayEvent>,
    height: Dp = 80.dp
) {
    val corner = 28.dp
    Box(
        Modifier
            .fillMaxWidth()
            .height(height + 24.dp)
            .background(Color.Transparent)
    ) {
        // Yttre kapsel
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(height)
                .background(Color.White, RoundedCornerShape(corner))
        )

        // Grön fyllnad (dagens progress)
        Box(
            Modifier
                .align(Alignment.TopStart)
                .height(height)
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(
                    Color(0xFFB7EA27),
                    RoundedCornerShape(topStart = corner, bottomStart = corner)
                )
        )

        // Canvas för timetiketter, events, markör
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

            // Inre kapsel (Bakgrund)
            drawRoundRect(
                color = Color(0xFFF8F8F8),
                topLeft = Offset(pad, trackTop),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(24f, 24f)
            )
            // Inre kapsel (Border)
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.75f),
                topLeft = Offset(pad, trackTop),
                size = Size(trackWidth, trackHeight),
                style = Stroke(width = 2f),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Grön fyllnad inne i kapseln (Dagens progress)
            drawRoundRect(
                color = Color(0xFFB7EA27),
                topLeft = Offset(pad, trackTop),
                size = Size(trackWidth * progress.coerceIn(0f, 1f), trackHeight),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Timetiketter 1–24
            for (h in 1..24) {
                val x = pad + trackWidth * (h / 24f)
                val label =
                    if (h == 24) "24" else h.toString() // Ändrat till "24" istället för "24/00"
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textAlign = Paint.Align.CENTER
                        textSize = 24f
                        isAntiAlias = true
                    }
                    drawText(label, x, trackTop + trackHeight / 2f + 8f, paint)
                }
            }

            // Event-segment (Lila händelser)
            events.forEach { e ->
                // Fixar wrap-around för midnatts-händelser
                var startFrac = fractionOfDay(e.start)
                var endFrac = e.end?.let { fractionOfDay(it) } ?: startFrac

                // Hanterar händelser som går över midnatt
                if (endFrac < startFrac) {
                    endFrac += 1.0f // T.ex. 23:00 (0.95) till 01:00 (0.04) -> 0.04 + 1.0 = 1.04
                }

                val startX = pad + trackWidth * startFrac
                val endX = pad + trackWidth * endFrac
                val w = max(endX - startX, trackWidth * 0.008f)
                val segTop = trackTop + trackHeight * 0.08f
                val segHeight = trackHeight * 0.84f

                drawRoundRect(
                    color = e.color,
                    topLeft = Offset(startX, segTop),
                    size = Size(w, segHeight),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }

            // Nu-markör (röd linje)
            val nowX = pad + trackWidth * fractionOfDay(now)
            drawLine(
                color = Color.Red,
                start = Offset(nowX, trackTop),
                end = Offset(nowX, trackTop + trackHeight),
                strokeWidth = 4f,
                cap = StrokeCap.Square
            )
        }

        // Ikonbubblor + titlar
        EventLabelsOverlay(events = events, height = height)
    }
}

// -------- IKONER --------
@Composable
private fun EventLabelsOverlay(events: List<DayEvent>, height: Dp) {
    Box(Modifier.fillMaxSize()) {
        events.filter { it.icon != null }.forEach { e ->
            val frac = fractionOfDay(e.start).coerceIn(0.0001f, 0.9999f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height + 24.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.weight(frac))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                e.icon!!,
                                modifier = Modifier.align(Alignment.Center),
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            e.title,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f - frac))
                }
            }
        }
    }
}

// -------- NÄSTA HÄNDELSE --------
@Composable
fun NextEventCard(events: List<DayEvent>, now: LocalTime) {
    val next = remember(events, now) {
        // Hitta nästa händelse som inte har passerat (minus 1 minut för att hantera tickern)
        events.sortedBy { it.start }.firstOrNull { !it.start.isBefore(now.minusMinutes(1)) }
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
private fun fractionOfDay(t: LocalTime): Float {
    val mins = t.hour * 60 + t.minute
    return mins / (24f * 60f)
}

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