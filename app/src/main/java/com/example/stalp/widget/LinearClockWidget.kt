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
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.TextUnit
import androidx.glance.unit.TextUnitType
import com.example.stalp.data.CalendarRepository
import com.example.stalp.data.WeatherRepository
import com.example.stalp.data.DayEvent
import java.time.LocalTime
import java.util.Calendar

object LinearClockWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weatherRepo = WeatherRepository(context)
        val calendarRepo = CalendarRepository(context)
        
        // Data loading inside provideContent creates a new subscription on every update
        // but for Glance, update is infrequent.
        // However, we need to call suspend function getEventsForToday.
        val events = calendarRepo.getEventsForToday()

        provideContent {
            val weatherData by weatherRepo.weatherDataFlow.collectAsState(initial = null)
            
            LinearClockWidgetContent(weatherData, events)
        }
    }
}

@Composable
private fun LinearClockWidgetContent(
    weatherData: com.example.stalp.data.WeatherData?,
    events: List<DayEvent>
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val colorBorder = ColorProvider(0xFF000000.toInt()) // Black border

    // Calculate dimensions for bitmap
    // Glance uses Dp, bitmap needs pixels.
    // We can use a fixed density assumption or try to get display metrics,
    // but typically widgets have standard sizes.
    // Let's assume a reasonable width like 400-600px for clarity, or map from Dp.
    val density = context.resources.displayMetrics.density
    val widthPx = (size.width.value * density).toInt().coerceAtLeast(300)
    val heightPx = (80 * density).toInt().coerceAtLeast(100)

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(0xFFFFFFFF.toInt()))
            .padding(8.dp)
            .clickable(actionStartActivity<LinearClockConfigActivity>())
    ) {
        
        // 1. TOP SECTION: Linear Clock (Optimized Bitmap)
        Box(
            GlanceModifier
                .fillMaxWidth()
                .height(80.dp)
                .background(colorBorder)
                .padding(2.dp)
        ) {
            val bitmap = LinearClockBitmapGenerator.generate(
                context = context,
                width = widthPx,
                height = heightPx,
                events = events,
                currentTime = LocalTime.now()
            )

            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "Linear Clock",
                modifier = GlanceModifier.fillMaxSize()
            )
        }
        
        Spacer(GlanceModifier.height(8.dp))
        
        // 2. BOTTOM SECTION: Two Boxes (Weather Text Left, Clothing Visual Right)
        Row(GlanceModifier.fillMaxWidth().height(120.dp)) {
            
            // 2.1 Weather Text Box (Left)
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
                    horizontalAlignment = Alignment.Start, // Align text to start
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (weatherData != null && weatherData.isDataLoaded) {
                         // Temperature
                         Text(
                             text = "${weatherData.temperatureCelsius}°C",
                             style = TextStyle(fontSize = TextUnit(24.dp.value, TextUnitType.Sp), fontWeight = FontWeight.Bold)
                         )
                         Spacer(GlanceModifier.height(4.dp))
                         // Advice Text
                         Text(
                             text = weatherData.adviceText,
                             style = TextStyle(fontSize = TextUnit(12.dp.value, TextUnitType.Sp)),
                             maxLines = 4
                         )
                    } else {
                        Text("Laddar väder...")
                    }
                }
            }
            
            Spacer(GlanceModifier.width(8.dp))
            
            // 2.2 Clothing Visual Box (Right)
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
                        Image(
                            provider = ImageProvider(weatherData.getClothingResourceId()),
                            contentDescription = "Clothing advice",
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    } else {
                         Text("Laddar...")
                    }
                }
            }
        }
    }
}
