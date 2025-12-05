package com.dagsbalken.app.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.ui.graphics.Color
import com.dagsbalken.app.data.DayEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CalendarRepository(private val context: Context) {

	suspend fun getEventsForToday(): List<DayEvent> =
		withContext(Dispatchers.IO) {
			// Kontrollera behörighet först
			if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
				return@withContext emptyList<DayEvent>()
			}

			val events = mutableListOf<DayEvent>()

			val projection = arrayOf(
				CalendarContract.Instances.EVENT_ID,
				CalendarContract.Instances.TITLE,
				CalendarContract.Instances.BEGIN,
				CalendarContract.Instances.END,
				CalendarContract.Instances.DISPLAY_COLOR
			)

			// Sätt upp tid för "idag" med java.time
			val zoneId = ZoneId.systemDefault()
			val today = LocalDate.now(zoneId)
			val startOfDayMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
			val endOfDayMillis =
				today.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()

			// Bygg query
			val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
			ContentUris.appendId(builder, startOfDayMillis)
			ContentUris.appendId(builder, endOfDayMillis)

			try {
				context.contentResolver.query(
					builder.build(),
					projection,
					null,
					null,
					"${CalendarContract.Instances.BEGIN} ASC"
				)?.use { cursor ->
					val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
					val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
					val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
					val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
					val colorIdx = cursor.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)

					while (cursor.moveToNext()) {
						val id = cursor.getString(idIdx)
						val title = cursor.getString(titleIdx) ?: "No Title"
						val begin = cursor.getLong(beginIdx)
						val end = cursor.getLong(endIdx)
						val color = cursor.getInt(colorIdx)

						val startTime = Instant.ofEpochMilli(begin).atZone(zoneId).toLocalTime()
						val endTime = Instant.ofEpochMilli(end).atZone(zoneId).toLocalTime()

						events.add(
							DayEvent(
								title = title,
								start = startTime,
								end = endTime,
								color = Color(color),
								icon = null
							)
						)
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
				// Returnera tom lista eller logga fel vid problem
			}
			events
		}
}