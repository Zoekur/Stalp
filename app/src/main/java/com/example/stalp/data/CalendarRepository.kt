package com.example.stalp.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

class CalendarRepository(private val context: Context) {

    fun getEventsForToday(): List<DayEvent> {
        val events = mutableListOf<DayEvent>()
        
        // Check permission first (simplified, assuming handled by caller or granted)
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.DISPLAY_COLOR
        )

        // Set up the time range for today
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Selection clause
        val selection = "${CalendarContract.Instances.BEGIN} >= ? AND ${CalendarContract.Instances.BEGIN} <= ?"
        val selectionArgs = arrayOf(startOfDay.timeInMillis.toString(), endOfDay.timeInMillis.toString())

        // Construct the URI for the range
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startOfDay.timeInMillis)
        ContentUris.appendId(builder, endOfDay.timeInMillis)

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

                val startTime = Instant.ofEpochMilli(begin).atZone(ZoneId.systemDefault()).toLocalTime()
                val endTime = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalTime()

                events.add(
                    DayEvent(
                        id = id,
                        title = title,
                        start = startTime,
                        end = endTime,
                        color = androidx.compose.ui.graphics.Color(color)
                    )
                )
            }
        }
        return events
    }
}
