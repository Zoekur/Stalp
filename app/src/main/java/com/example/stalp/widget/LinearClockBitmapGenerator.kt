package com.example.stalp.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.example.stalp.data.DayEvent
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object LinearClockBitmapGenerator {

    fun generate(
        context: Context,
        width: Int,
        height: Int,
        events: List<DayEvent>,
        currentTime: LocalTime = LocalTime.now()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Colors
        val colorPassed = 0xFF86E3B3.toInt() // Light green
        val colorFuture = 0xFFFFFFFF.toInt() // White
        val colorRedLine = 0xFFEF4444.toInt() // Red
        val colorBorder = 0xFF000000.toInt() // Black
        val colorText = 0xFF000000.toInt()

        // Paint setup
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // 1. Draw Background (Future)
        paint.color = colorFuture
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 2. Draw Progress (Passed time - Left Half)
        // Since the view is centered on 'now', the left half represents time < now.
        paint.color = colorPassed
        canvas.drawRect(0f, 0f, width / 2f, height.toFloat(), paint)

        // Time Window Logic
        val zoomHours = 2
        val windowDurationMinutes = zoomHours * 2 * 60 // 240 minutes
        val minutesPerPixel = windowDurationMinutes.toFloat() / width

        // Window Range (in minutes from start of day)
        val currentMinuteOfDay = currentTime.hour * 60 + currentTime.minute
        val windowStartMinute = currentMinuteOfDay - (windowDurationMinutes / 2)
        val windowEndMinute = windowStartMinute + windowDurationMinutes

        // 3. Draw Events
        events.forEach { event ->
            val eventStartMin = event.start.hour * 60 + event.start.minute
            // Handle day wrapping for end time if needed, but assuming simple same-day events for now based on Repo
            var eventEndMin = if (event.end != null) event.end.hour * 60 + event.end.minute else eventStartMin + 60

            // If end is smaller than start, it might wrap to next day, or be an error.
            // Simple fix: if end < start, assume next day (+24h)
            if (eventEndMin < eventStartMin) eventEndMin += 24 * 60

            // Check visibility
            // We map event times to x-coordinates.
            // x = (eventTime - windowStartMinute) / minutesPerPixel

            val startX = (eventStartMin - windowStartMinute) / minutesPerPixel
            val endX = (eventEndMin - windowStartMinute) / minutesPerPixel

            // Only draw if visible
            if (endX > 0 && startX < width) {
                paint.color = event.color.toArgb()
                canvas.drawRect(startX, 0f, endX, height.toFloat(), paint)
            }
        }

        // 4. Draw Hour Ticks and Text
        paint.color = colorBorder
        paint.strokeWidth = 2f
        paint.textSize = 24f // Adjust based on density ideally, but fixed for now
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER

        // Find first visible hour
        // windowStartMinute might be negative (yesterday)
        val firstHour = (windowStartMinute / 60) - 1
        val lastHour = (windowEndMinute / 60) + 1

        for (h in firstHour..lastHour) {
            val hourMin = h * 60
            val x = (hourMin - windowStartMinute) / minutesPerPixel

            if (x >= 0 && x <= width) {
                // Draw Tick
                canvas.drawLine(x, 0f, x, height * 0.3f, paint)

                // Draw Text
                val hourText = "${(h % 24 + 24) % 24}"
                canvas.drawText(hourText, x, height * 0.6f + 12f, paint)
            }
        }

        // 5. Draw Red Line (Current Time - Center)
        paint.color = colorRedLine
        paint.strokeWidth = 4f
        canvas.drawLine(width / 2f, 0f, width / 2f, height.toFloat(), paint)

        // 6. Border
        paint.color = colorBorder
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.style = Paint.Style.FILL // Reset

        return bitmap
    }
}
