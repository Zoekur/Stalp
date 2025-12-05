package com.dagsbalken.app.data

import androidx.compose.ui.graphics.Color
import java.time.LocalTime

data class DayEvent(
    val start: LocalTime,
    val end: LocalTime?,
    val title: String,
    val color: Color,
    val icon: String?
)
