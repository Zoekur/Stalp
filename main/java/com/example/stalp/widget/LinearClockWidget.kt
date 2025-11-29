package com.example.stalp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
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
// Dessa två är kritiska för TextUnit-felet:
import androidx.glance.unit.TextUnit
import androidx.glance.unit.TextUnitType

object LinearClockWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            LinearClockWidgetContent()
        }
    }
}

@Composable
private fun LinearClockWidgetContent() {
    val prefs = currentState<Preferences>()
    val fontKey = prefs[LinearClockPrefs.FONT_FAMILY] ?: LinearClockPrefs.DEF_FONT
    val scale   = (prefs[LinearClockPrefs.FONT_SCALE] ?: LinearClockPrefs.DEF_SCALE).coerceIn(0.7f, 1.6f)
    val colorBg = prefs[LinearClockPrefs.COLOR_BG] ?: LinearClockPrefs.DEF_BG
    val colorText = prefs[LinearClockPrefs.COLOR_TEXT] ?: LinearClockPrefs.DEF_TEXT
    val colorAccent = prefs[LinearClockPrefs.COLOR_ACCENT] ?: LinearClockPrefs.DEF_ACCENT
    val totalHoursToShow = prefs[LinearClockPrefs.HOURS_TO_SHOW] ?: LinearClockPrefs.DEF_HOURS_TO_SHOW

    val hoursBeforeNow = totalHoursToShow / 2 - 1
    val hoursAfterNow = totalHoursToShow - hoursBeforeNow - 1

    val cal = java.util.Calendar.getInstance()
    val nowHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val nowMin = cal.get(java.util.Calendar.MINUTE)
    val nowFrac = nowMin / 60f

    val hours = ((-hoursBeforeNow)..hoursAfterNow).map { wrap24(nowHour + it) }
    val redLineFraction = (hoursBeforeNow.toFloat() + nowFrac) / totalHoursToShow.toFloat()
    val currentHourIndex = hoursBeforeNow

    val fam = when (fontKey) {
        "sans" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(colorBg))
            .padding(12.dp)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<LinearClockConfigActivity>())
    ) {
        // Tidslinje Container
        Box(GlanceModifier.fillMaxWidth().defaultWeight()) {

            Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                hours.forEachIndexed { idx, h ->
                    Column(GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {

                        Text(
                            text = h.toString().padStart(2, '0'),
                            style = TextStyle(
                                color = ColorProvider(colorText),
                                fontFamily = fam,
                                fontWeight = if (idx == currentHourIndex) FontWeight.Bold else FontWeight.Normal,
                                fontSize = TextUnit(14f * scale, TextUnitType.Sp),
                            ),
                        )
                        Spacer(GlanceModifier.height((4 * scale).dp))

                        HourBlock(
                            progress = if (idx == currentHourIndex) nowFrac else null,
                            bg = 0xFFE5E7EB.toInt(),
                            accent = colorAccent
                        )
                    }
                }
            }

            // Röda bandet
            Row(GlanceModifier.fillMaxWidth().fillMaxHeight()) {
                if (redLineFraction > 0f) {
                    Spacer(GlanceModifier.defaultWeight(redLineFraction))
                }
                Box(
                    GlanceModifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(ColorProvider(0xFFEF4444.toInt()))
                ) {}
                if (redLineFraction < 1f) {
                    Spacer(GlanceModifier.defaultWeight(1f - redLineFraction))
                }
            }
        }

        Spacer(GlanceModifier.height((8 * scale).dp))

        // Nedre raden
        Row(GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End, // Förenklad alignment
            verticalAlignment = Alignment.CenterVertically) {

            Text(
                text = "%02d:%02d".format(nowHour, nowMin),
                style = TextStyle(
                    color = ColorProvider(colorText),
                    fontFamily = fam,
                    fontSize = TextUnit(18f * scale, TextUnitType.Sp)
                ),
                modifier = GlanceModifier.padding(end = 16.dp)
            )
            Text(
                text = "⚙️",
                style = TextStyle(
                    color = ColorProvider(colorText),
                    fontFamily = fam,
                    fontSize = TextUnit(18f * scale, TextUnitType.Sp)
                ),
                modifier = GlanceModifier.clickable(
                    actionStartActivity<LinearClockConfigActivity>()
                )
            )
        }
    }
}

@Composable
private fun HourBlock(progress: Float?, bg: Int, accent: Int) {
    Box(
        GlanceModifier
            .fillMaxWidth()
            .height(18.dp)
            .background(ColorProvider(bg))
            .cornerRadius(4.dp)
    ) {
        if (progress != null) {
            val pct = progress.coerceIn(0f, 1f)
            Row(GlanceModifier.fillMaxSize()) {
                if (pct > 0f) {
                    Box(
                        GlanceModifier
                            .fillMaxHeight()
                            .defaultWeight(pct)
                            .background(ColorProvider(accent))
                            .cornerRadius(4.dp)
                    ) {}
                }
                if (pct < 1f) {
                    Spacer(GlanceModifier.defaultWeight(1f - pct))
                }
            }
        }
    }
}

private fun wrap24(hour: Int): Int {
    return (hour % 24 + 24) % 24
}