package com.example.stalp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.copy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.text.backgroundColor
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.stalp.ui.settings.ThemePreferences
import com.example.stalp.ui.theme.StalpTheme
import com.example.stalp.ui.theme.ThemeOption
import com.example.stalp.ui.theme.ThemeSelector
import kotlinx.coroutines.launch

@Composable
private fun ColorSelector(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    Text("Färger") // "Colors"
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Consider defining these colors as constants
        val availableColors = listOf(0xFFFFFFFF.toInt(), 0xFF111827.toInt())

        availableColors.forEach { color ->
            Box(
                Modifier
                    .size(40.dp)
                    .background(androidx.compose.ui.graphics.Color(color))
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

data class WidgetConfig(
    val font: String = LinearClockPrefs.DEF_FONT,
    val scale: Float = LinearClockPrefs.DEF_SCALE,
    val backgroundColor: Int = LinearClockPrefs.DEF_BG,
    val textColor: Int = LinearClockPrefs.DEF_TEXT,
    val accentColor: Int = LinearClockPrefs.DEF_ACCENT,
    val hoursToShow: Int = LinearClockPrefs.DEF_HOURS_TO_SHOW
)
class LinearClockConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // If the ID is invalid, we finish the activity immediately.
            setResult(RESULT_CANCELED) // It's good practice to set result to CANCELED
            finish()
            return // Stop further execution in onCreate
        }

        setContent {
                val themeOption by ThemePreferences.themeOptionFlow(applicationContext)
                    .collectAsState(initial = ThemeOption.NordicCalm)
                val scope = rememberCoroutineScope()
                // In your setContent block
            ColorSelector(
                selectedColor = config.backgroundColor,
                onColorSelected = { newColor -> config = config.copy(backgroundColor = newColor) }
            )
                var config by remember { mutableStateOf(WidgetConfig()) }

    //...

    // Example of updating a value
                Box(
                    Modifier
                        .size(40.dp)
                        .background(androidx.compose.ui.graphics.Color(c))
                        .clickable { config = config.copy(backgroundColor = c) } // Use copy()
                )

    //...

    // When saving, access properties from the config object
                updateAppWidgetState<Preferences>(...)) { prefs ->
                prefs[LinearClockPrefs.FONT_FAMILY] = config.font
                prefs[LinearClockPrefs.FONT_SCALE] = config.scale
                prefs[LinearClockPrefs.COLOR_BG] = config.backgroundColor
                // ... and so on
            }

            StalpTheme(themeOption = themeOption, dynamicColorEnabled = false) {
                var font by remember { mutableStateOf(LinearClockPrefs.DEF_FONT) }
                var scale by remember { mutableStateOf(LinearClockPrefs.DEF_SCALE) }
                var bg by remember { mutableStateOf(LinearClockPrefs.DEF_BG) }
                var text by remember { mutableStateOf(LinearClockPrefs.DEF_TEXT) }
                var accent by remember { mutableStateOf(LinearClockPrefs.DEF_ACCENT) }
                var hoursToShow by remember { mutableStateOf(LinearClockPrefs.DEF_HOURS_TO_SHOW) }

                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(16.dp)) {
                        ThemeSelector(
                            selectedOption = themeOption,
                            onOptionSelected = { option ->
                                scope.launch { ThemePreferences.setThemeOption(applicationContext, option) }
                            },
                        )

                        Spacer(Modifier.height(16.dp))

                        Text("Widget-inställningar", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))

                        Text("Färger")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0xFFFFFFFF.toInt(), 0xFF111827.toInt()).forEach { c ->
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .background(androidx.compose.ui.graphics.Color(c))
                                        .clickable { bg = c }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    val context = this@LinearClockConfigActivity        val manager = GlanceAppWidgetManager(context)

                                    // Find the glanceId corresponding to the appWidgetId we are configuring
                                    val glanceId = manager.getGlanceIdBy(appWidgetId)

                                    if (glanceId != null) {
                                        // Update the state for this specific glanceId
                                        updateAppWidgetState(context,
                                            PreferencesGlanceStateDefinition, glanceId) { prefs ->
                                            // Make sure to use the state holder from suggestion #1
                                            prefs.toMutablePreferences().apply {
                                                this[LinearClockPrefs.FONT_FAMILY] = config.font
                                                this[LinearClockPrefs.FONT_SCALE] = config.scale
                                                this[LinearClockPrefs.COLOR_BG] = config.backgroundColor
                                                this[LinearClockPrefs.COLOR_TEXT] = config.textColor
                                                this[LinearClockPrefs.COLOR_ACCENT] = config.accentColor
                                                this[LinearClockPrefs.HOURS_TO_SHOW] = config.hoursToShow
                                            }
                                        }
                                        // Trigger an update for the widget
                                        LinearClockWidget.update(context, glanceId)
                                    }

                                    // Set the result and finish
                                    val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                    setResult(RESULT_OK, result)
                                    finish()
                                }
                            },

                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save") }
                    }
                }
            }
        }
    }
}
