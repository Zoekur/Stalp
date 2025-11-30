package com.example.stalp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.currentState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.TextUnit
import androidx.glance.unit.TextUnitType
import com.example.stalp.data.CalendarRepository
import com.example.stalp.data.WeatherRepository
import com.example.stalp.data.DayEvent
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

object LinearClockWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Fetch repositories
        val weatherRepo = WeatherRepository(context)
        val calendarRepo = CalendarRepository(context)
        
        provideContent {
            val weatherData by weatherRepo.weatherDataFlow.collectAsState(initial = null)
            // Ideally we should observe calendar data too, but for now we fetch it once or assume it's stable during composition
            // In a real app we might want to reload it periodically or on change
            val events = calendarRepo.getEventsForToday()
            
            LinearClockWidgetContent(weatherData, events)
        }
    }
}

@Composable
private fun LinearClockWidgetContent(
    weatherData: com.example.stalp.data.WeatherData?,
    events: List<DayEvent>
) {
    val prefs = currentState<Preferences>()
    // Defaults matching the user's description and existing preferences
    val scale = (prefs[LinearClockPrefs.FONT_SCALE] ?: LinearClockPrefs.DEF_SCALE).coerceIn(0.7f, 1.6f)

    val colorPassed = ColorProvider(0xFF86E3B3.toInt()) // Light green
    val colorFuture = ColorProvider(0xFFFFFFFF.toInt()) // White
    val colorRedLine = ColorProvider(0xFFEF4444.toInt()) // Red
    val colorBorder = ColorProvider(0xFF000000.toInt()) // Black border

    val cal = Calendar.getInstance()
    val nowHour = cal.get(Calendar.HOUR_OF_DAY)
    val nowMin = cal.get(Calendar.MINUTE)

    // "Zoom in on current time +- a couple of hours"
    val zoomHours = 2 // +/- 2 hours
    val windowDurationHours = zoomHours * 2 // Total 4 hours visible
    val windowDurationMinutes = windowDurationHours * 60

    val currentMinuteOfDay = nowHour * 60 + nowMin

    // Calculate start and end of the window (in minutes from start of day)
    // We want 'now' to be in the center, so subtract half the window duration.
    val windowStartMinute = currentMinuteOfDay - (windowDurationMinutes / 2)
    val windowEndMinute = windowStartMinute + windowDurationMinutes
    
    // Helper function to map a time (in minutes from midnight) to a fraction [0, 1] relative to the window
    fun mapTimeToWindowFraction(timeInMinutes: Int): Float {
        // Adjust for day wrapping if necessary for display calculation?
        // Simpler approach: normalize everything to the linear timeline relative to windowStart.

        // If the window crosses midnight (e.g. 23:00 to 03:00), we need to handle wrapping.
        // But visualizing a wrapping timeline linearly is tricky if we stick to strict "minutes from midnight".
        // Instead, let's treat time as continuous relative to windowStart.

        var t = timeInMinutes
        // If the window starts in previous day (negative), and we are comparing a time from today?
        // Or if window wraps to next day?

        // Let's normalize 't' to be comparable to windowStartMinute.
        // If windowStartMinute is negative (e.g. -60 means 23:00 yesterday), and t is 23*60 (today),
        // we should treat t as t - 24*60 if it's meant to be yesterday? No.

        // Better approach: Since we only care about "today's" events mostly, let's handle wrapping:
        // If windowStartMinute < 0: we are showing end of yesterday and start of today.
        // If windowEndMinute > 24*60: we are showing end of today and start of tomorrow.

        // To simplify projection: project t into the coordinate system [windowStartMinute, windowEndMinute].

        // Handling the wrap for t:
        // If windowStartMinute < 0 and t is large (e.g. 23:00), it might be yesterday.
        // If windowEndMinute > 24*60 and t is small (e.g. 01:00), it might be tomorrow.

        // However, 'events' are only for "today" (00:00 to 23:59).
        // So we just need to check if 't' falls within [windowStartMinute, windowEndMinute]
        // taking into account that the window might be outside [0, 24*60].

        // Case 1: Window is within today (e.g. 10:00 to 14:00). No wrapping.
        // Case 2: Window starts yesterday (e.g. -01:00 to 03:00).
        //    Today's events 00:00..03:00 correspond to relative time [60..240] in window (starts at -60).
        //    Today's events 23:00..23:59 are not visible (they are far future).
        // Case 3: Window ends tomorrow (e.g. 22:00 to 26:00 (02:00)).
        //    Today's events 22:00..23:59 correspond to relative time [0..120].
        //    Tomorrow's events are not in 'events' list, so we ignore.

        // So, for a given 't' (0..1440), we project it.
        // If windowStartMinute < 0:
        //    Also check if t (as yesterday) fits? No, events are today.
        //    So we map t directly. relative = t - windowStartMinute.
        //    If t=0, windowStart=-60, relative=60. Correct.

        // If windowEndMinute > 1440:
        //    Also map t directly. relative = t - windowStartMinute.
        //    If t=1439, windowStart=1300, relative=139. Correct.

        // What if we are at 00:30? window = -01:30 to 02:30.
        // windowStart = -90.
        // Current time t = 30.
        // Relative pos = 30 - (-90) = 120.
        // Center of window (duration 240) is 120. Correct.

        // What if we are at 23:30? window = 21:30 to 25:30.
        // windowStart = 21*60 + 30 = 1290.
        // Current time t = 1410.
        // Relative pos = 1410 - 1290 = 120. Center. Correct.

        val relative = t - windowStartMinute
        return relative.toFloat() / windowDurationMinutes.toFloat()
    }
    
    // Progress fraction is always 0.5 (center) unless we are clamped at edges of day?
    // "Zoom in on current time". If we simply scroll the view, the red line is always center.
    val progressFraction = 0.5f

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(0xFFFFFFFF.toInt()))
            .padding(8.dp)
            .clickable(actionStartActivity<LinearClockConfigActivity>())
    ) {
        
        // 1. TOP SECTION: Linear Clock (Zoomed)
        Box(
            GlanceModifier
                .fillMaxWidth()
                .height(80.dp)
                .background(colorBorder)
                .padding(2.dp)
        ) {
             Box(
                GlanceModifier
                    .fillMaxSize()
                    .background(colorFuture)
            ) {
                 // 1.1 Green Progress Bar (Passed time)
                 // Since we are centered on 'now', the left half is always passed time.
                 Row(GlanceModifier.fillMaxSize()) {
                     Box(
                         GlanceModifier
                             .fillMaxHeight()
                             .defaultWeight(1f) // Left half
                             .background(colorPassed)
                     ) {}
                     Spacer(GlanceModifier.defaultWeight(1f)) // Right half
                 }
                 
                 // 1.2 Calendar Events
                 events.forEach { event ->
                     val startMin = event.start.hour * 60 + event.start.minute
                     val endMin = if (event.end != null) event.end.hour * 60 + event.end.minute else startMin + 60
                     // We assume events don't wrap around midnight for simplicity in this calculation or are split.
                     
                     val startFrac = mapTimeToWindowFraction(startMin)
                     val endFrac = mapTimeToWindowFraction(endMin)
                     
                     // Only draw if visible in window [0, 1]
                     if (endFrac > 0f && startFrac < 1f) {
                         val visibleStart = startFrac.coerceAtLeast(0f)
                         val visibleEnd = endFrac.coerceAtMost(1f)
                         val duration = visibleEnd - visibleStart

                         if (duration > 0) {
                             Row(GlanceModifier.fillMaxSize()) {
                                 if (visibleStart > 0) Spacer(GlanceModifier.defaultWeight(visibleStart))
                                 Box(
                                     GlanceModifier
                                         .fillMaxHeight()
                                         .defaultWeight(duration)
                                         .background(ColorProvider(event.color))
                                 ) { }
                                 if (visibleEnd < 1f) Spacer(GlanceModifier.defaultWeight(1f - visibleEnd))
                             }
                         }
                     }
                 }

                 // 1.3 Hour Numbers
                 // We want to show hours that fall within the window.
                 // Window range: windowStartMinute to windowEndMinute
                 // E.g. -90 to 150.
                 // We iterate through all hours that *could* be visible.
                 // The first hour boundary after windowStartMinute.
                 val firstHour = (windowStartMinute / 60) - 1
                 val lastHour = (windowEndMinute / 60) + 1

                 for (h in firstHour..lastHour) {
                     val timeInMin = h * 60
                     val frac = mapTimeToWindowFraction(timeInMin)

                     if (frac in 0f..1f) {
                         // We position the text at 'frac'.
                         // Using Row with weights is tricky for exact positioning of multiple items without nested structure.
                         // But we can try to place a 0-width Box at the position and let it overflow? No.
                         // We can use a Row where we place items relative to each other? Hard.
                         // Best bet in Glance for arbitrary position:
                         // Use a Row with spacers. But we can't do that for multiple items easily in a loop unless we sort them.
                         // Since we loop in order:
                         // Wait, we need to render ALL hours in one Row to ensure spacing?
                         // Or one Row per hour? One Row per hour works if they are overlays.

                         // Let's render one Row per hour tick for simplicity of positioning.
                         Row(GlanceModifier.fillMaxSize()) {
                             if (frac > 0) Spacer(GlanceModifier.defaultWeight(frac))
                             Box(
                                 GlanceModifier.width(1.dp).fillMaxHeight(0.3f).background(ColorProvider(0xFF000000.toInt())) // Tick mark
                             ) {}
                             // Text
                             /*
                             Text(
                                 text = "${(h + 24) % 24}", // Handle negative hours or > 23
                                 style = TextStyle(fontSize = TextUnit(10.dp.value, TextUnitType.Sp))
                             )
                             */
                             if (frac < 1f) Spacer(GlanceModifier.defaultWeight(1f - frac))
                         }

                         // Separate Row for Text to avoid layout issues with Tick
                         Row(GlanceModifier.fillMaxSize()) {
                             if (frac > 0) Spacer(GlanceModifier.defaultWeight(frac))
                             Text(
                                 text = "${(h % 24 + 24) % 24}",
                                 style = TextStyle(fontSize = TextUnit(10.dp.value, TextUnitType.Sp)),
                                 modifier = GlanceModifier.padding(top = 10.dp) // Push text below tick
                             )
                             if (frac < 1f) Spacer(GlanceModifier.defaultWeight(1f - frac))
                         }
                     }
                 }
                 
                 // 1.4 Red Line (Current Time) - Always center
                 Row(GlanceModifier.fillMaxSize()) {
                     Spacer(GlanceModifier.defaultWeight(0.5f))
                     Box(
                         GlanceModifier
                             .width(2.dp)
                             .fillMaxHeight()
                             .background(colorRedLine)
                     ) {}
                     Spacer(GlanceModifier.defaultWeight(0.5f))
                 }
            }
        }
        
        Spacer(GlanceModifier.height(8.dp))
        
        // 2. BOTTOM SECTION: Two Boxes (Weather+Clothing combined, Calendar)
        Row(GlanceModifier.fillMaxWidth().height(120.dp)) {
            
            // 2.1 Weather & Clothing Box (Left)
            Box(
                GlanceModifier
                    .defaultWeight(1f)
                    .fillMaxHeight()
                    .background(ColorProvider(0xFFFFFFFF.toInt()))
                    .cornerRadius(16.dp)
                    .padding(2.dp)
                    .background(colorBorder)
            ) {
                 Column(
                    GlanceModifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .background(ColorProvider(0xFFFFFFFF.toInt()))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (weatherData != null && weatherData.isDataLoaded) {
                         // Icon
                         Text(
                            text = weatherData.adviceIcon,
                            style = TextStyle(fontSize = TextUnit(32.dp.value, TextUnitType.Sp))
                        )
                        Spacer(GlanceModifier.height(4.dp))
                         // Weather Text
                         Text(
                             text = "${weatherData.temperatureCelsius}°C",
                             style = TextStyle(fontSize = TextUnit(16.dp.value, TextUnitType.Sp), fontWeight = FontWeight.Bold)
                         )
                         Text(
                             text = weatherData.adviceText,
                             style = TextStyle(fontSize = TextUnit(10.dp.value, TextUnitType.Sp)),
                             maxLines = 3
                         )
                    } else {
                        Text("Laddar väder...")
                    }
                }
            }
            
            Spacer(GlanceModifier.width(8.dp))
            
            // 2.2 Calendar Box (Right) - NEW
            Box(
                GlanceModifier
                    .defaultWeight(1f)
                    .fillMaxHeight()
                    .background(ColorProvider(0xFFFFFFFF.toInt()))
                    .cornerRadius(16.dp)
                    .padding(2.dp)
                    .background(colorBorder)
            ) {
                 Column(
                    GlanceModifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .background(ColorProvider(0xFFFFFFFF.toInt()))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Kalender",
                        style = TextStyle(fontSize = TextUnit(12.dp.value, TextUnitType.Sp), fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(4.dp))

                    if (events.isEmpty()) {
                        Text(
                            text = "Inga fler händelser idag.",
                            style = TextStyle(fontSize = TextUnit(10.dp.value, TextUnitType.Sp))
                        )
                    } else {
                        // Display next 2-3 events
                        val upcomingEvents = events.filter {
                            val eventStartMin = it.start.hour * 60 + it.start.minute
                            eventStartMin >= currentMinuteOfDay
                        }.take(3)

                        if (upcomingEvents.isEmpty()) {
                             Text(
                                text = "Inga fler händelser.",
                                style = TextStyle(fontSize = TextUnit(10.dp.value, TextUnitType.Sp))
                            )
                        } else {
                            upcomingEvents.forEach { event ->
                                Text(
                                    text = "• ${event.start} ${event.title}",
                                    style = TextStyle(fontSize = TextUnit(10.dp.value, TextUnitType.Sp)),
                                    maxLines = 1
                                )
                                Spacer(GlanceModifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
