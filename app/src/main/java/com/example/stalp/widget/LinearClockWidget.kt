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
import androidx.glance.text.FontFamily
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
import java.time.temporal.ChronoUnit

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
    // The visualization shows a green progress bar (passed time) and white future time.
    // We can use defaults or preferences for colors, but let's try to match the image.
    val colorPassed = ColorProvider(0xFF86E3B3.toInt()) // Light green
    val colorFuture = ColorProvider(0xFFFFFFFF.toInt()) // White
    val colorRedLine = ColorProvider(0xFFEF4444.toInt()) // Red
    val colorBorder = ColorProvider(0xFF000000.toInt()) // Black border

    val cal = java.util.Calendar.getInstance()
    val nowHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val nowMin = cal.get(java.util.Calendar.MINUTE)
    
    // Total hours to show: 24h as per visualization (1 2 ... 23 00)
    // The visualization shows a full day linear clock.
    val startHour = 1 // or 0? Visualization starts at 1.
    val hoursList = (1..23).toList() + 0
    
    // We need to calculate the fraction of the day that has passed.
    val minutesPassedToday = nowHour * 60 + nowMin
    val totalMinutesInDay = 24 * 60
    val progressFraction = minutesPassedToday.toFloat() / totalMinutesInDay.toFloat()

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(0xFFFFFFFF.toInt())) // Background for the whole widget
            .padding(8.dp)
            .clickable(actionStartActivity<LinearClockConfigActivity>())
    ) {
        
        // 1. TOP SECTION: Linear Clock (Green progress bar + Calendar Events)
        // Visualization shows a bordered box
        Box(
            GlanceModifier
                .fillMaxWidth()
                .height(80.dp) // Adjust height as needed
                .background(colorBorder)
                .padding(2.dp) // Border thickness
        ) {
            // Inner container with actual content
             Box(
                GlanceModifier
                    .fillMaxSize()
                    .background(colorFuture)
            ) {
                 // 1.1 Green Progress Bar (Passed time)
                 Row(GlanceModifier.fillMaxSize()) {
                     if (progressFraction > 0f) {
                         Box(
                             GlanceModifier
                                 .fillMaxHeight()
                                 .defaultWeight(progressFraction)
                                 .background(colorPassed)
                         ) {}
                     }
                     if (progressFraction < 1f) {
                         Spacer(GlanceModifier.defaultWeight(1f - progressFraction))
                     }
                 }
                 
                 // 1.2 Calendar Events
                 // We overlay them. Since we are in a Box, we can stack.
                 // But we need to position them horizontally based on time.
                 // Events are absolute time (00:00 - 23:59).
                 // We need to map start/end time to horizontal fraction.
                 // Since Glance doesn't support absolute positioning well (like offset), 
                 // we might need to use a Row with Spacers/Weights for each event, but that's hard if they overlap.
                 // Simplified approach: Render non-overlapping events or just one layer of events.
                 // Or use a custom drawing (Canvas) which Glance doesn't support directly for widgets (it uses RemoteViews).
                 // We can simulate absolute positioning with a Row and weights if we handle one event at a time or non-overlapping.
                 // For now, let's just try to render them on top if possible, or maybe just render markers.
                 // The visualization shows a purple block.
                 
                 // Let's try to render events using a Row that spans the whole day.
                 // But since we can't easily overlay multiple varying-width boxes at exact positions without a complex weight calculation,
                 // we might check if we can render them inside the same Row structure or a separate Box overlay.
                 // Box overlay works.
                 
                 events.forEach { event ->
                     val startMin = event.start.hour * 60 + event.start.minute
                     // Handle end time wrapping or clamping
                     val endMin = if (event.end != null) event.end.hour * 60 + event.end.minute else startMin + 60
                     val safeEndMin = if (endMin < startMin) 24*60 else endMin // Handle overflow?
                     
                     val startFrac = startMin.toFloat() / totalMinutesInDay
                     val durationFrac = (safeEndMin - startMin).toFloat() / totalMinutesInDay
                     
                     if (durationFrac > 0) {
                         Row(GlanceModifier.fillMaxSize()) {
                             if (startFrac > 0) Spacer(GlanceModifier.defaultWeight(startFrac))
                             Box(
                                 GlanceModifier
                                     .fillMaxHeight()
                                     .defaultWeight(durationFrac)
                                     .background(ColorProvider(event.color)) // Purple in visual, but we use event color
                                     .run {
                                         // Make it semi-transparent or hatched? Glance doesn't support alpha easily on background color unless color has alpha.
                                         // Let's assume event.color has alpha or is solid.
                                         this
                                     }
                             ) { }
                             if (startFrac + durationFrac < 1f) Spacer(GlanceModifier.defaultWeight(1f - (startFrac + durationFrac)))
                         }
                     }
                 }

                 // 1.3 Hour Numbers
                 // These should be overlayed on top of bars.
                 Row(
                     GlanceModifier.fillMaxSize(),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     hoursList.forEach { h ->
                         Box(
                             GlanceModifier.defaultWeight(1f),
                             contentAlignment = Alignment.Center
                         ) {
                             Text(
                                 text = h.toString(),
                                 style = TextStyle(
                                     fontSize = TextUnit(12.dp.value, TextUnitType.Sp), // Approximate
                                     fontWeight = FontWeight.Bold
                                 )
                             )
                         }
                     }
                 }
                 
                 // 1.4 Red Line (Current Time)
                 Row(GlanceModifier.fillMaxSize()) {
                     if (progressFraction > 0f) Spacer(GlanceModifier.defaultWeight(progressFraction))
                     Box(
                         GlanceModifier
                             .width(2.dp)
                             .fillMaxHeight()
                             .background(colorRedLine)
                     ) {}
                     if (progressFraction < 1f) Spacer(GlanceModifier.defaultWeight(1f - progressFraction))
                 }
            }
        }
        
        Spacer(GlanceModifier.height(8.dp))
        
        // 2. BOTTOM SECTION: Two Boxes (Weather & Clothing)
        Row(GlanceModifier.fillMaxWidth().height(120.dp)) { // Fixed height for bottom section
            
            // 2.1 Weather Info Box
            Box(
                GlanceModifier
                    .defaultWeight(1f)
                    .fillMaxHeight()
                    .background(ColorProvider(0xFFFFFFFF.toInt()))
                    .cornerRadius(16.dp)
                    // Border simulation (Box inside Box with padding)
                    .padding(2.dp)
                    .background(colorBorder) // Border color
            ) {
                 Box(
                    GlanceModifier
                        .fillMaxSize()
                        .padding(2.dp) // Border width
                        .background(ColorProvider(0xFFFFFFFF.toInt())) // Inner background
                        .padding(8.dp)
                ) {
                    if (weatherData != null && weatherData.isDataLoaded) {
                         Text(
                             text = "Temp: ${weatherData.temperatureCelsius}°C\n" +
                                    "Nederbörd: ${weatherData.precipitationChance}%\n\n" +
                                    weatherData.adviceText,
                             style = TextStyle(fontSize = TextUnit(12.dp.value, TextUnitType.Sp))
                         )
                    } else {
                        Text("Laddar väder...")
                    }
                }
            }
            
            Spacer(GlanceModifier.width(8.dp))
            
            // 2.2 Clothing Advice Box
            Box(
                GlanceModifier
                    .defaultWeight(1f)
                    .fillMaxHeight()
                    .background(ColorProvider(0xFFFFFFFF.toInt()))
                    .cornerRadius(16.dp)
                    .padding(2.dp)
                    .background(colorBorder)
            ) {
                 Box(
                    GlanceModifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .background(ColorProvider(0xFFFFFFFF.toInt()))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (weatherData != null && weatherData.isDataLoaded) {
                        // Display the icon/emoji large
                        Text(
                            text = weatherData.adviceIcon,
                            style = TextStyle(fontSize = TextUnit(40.dp.value, TextUnitType.Sp))
                        )
                    } else {
                         Text("?")
                    }
                }
            }
        }
    }
}
