package com.example.stalp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.stalp.data.CalendarRepository
import com.example.stalp.data.DayEvent
import com.example.stalp.data.WeatherRepository
import java.time.LocalTime

object LinearClockWidget : GlanceAppWidget() {

	override val stateDefinition = PreferencesGlanceStateDefinition

	override suspend fun provideGlance(context: Context, id: GlanceId) {
		val weatherRepo = WeatherRepository(context)
		val calendarRepo = CalendarRepository(context)

		// Hämta event (detta sker vid uppdatering)
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
	val colorBorder = ColorProvider(0xFF000000.toInt())

	// Enkel logik för px-beräkning (kan behöva justeras för exakt precision)
	val density = context.resources.displayMetrics.density
	val widthPx = (size.width.value * density).toInt().coerceAtLeast(300)
	val heightPx = (size.height.value * density).toInt().coerceAtLeast(100)

	Column(
		GlanceModifier
			.fillMaxSize()
			.background(ColorProvider(0xFFFFFFFF.toInt()))
			.padding(8.dp)
			.clickable(actionStartActivity<LinearClockConfigActivity>())
	) {

		// 1. TOP SECTION: Klockan
		Box(
			GlanceModifier
				.fillMaxSize()
				.background(colorBorder)
				.padding(2.dp)
		) {
			// OBS: Se till att LinearClockBitmapGenerator finns!
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
	}
}