package com.example.stalp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.stalp.ui.settings.ThemePreferences
import com.example.stalp.ui.theme.StalpTheme
import com.example.stalp.ui.theme.ThemeOption
import com.example.stalp.ui.theme.ThemeSelector
import kotlinx.coroutines.launch

class LinearClockConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish(); return
        }

        setContent {
            val themeOption by ThemePreferences.themeOptionFlow(applicationContext)
                .collectAsState(initial = ThemeOption.NordicCalm)
            val scope = rememberCoroutineScope()

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
                                    val ctx = this@LinearClockConfigActivity
                                    val manager = GlanceAppWidgetManager(ctx)
                                    // Försöker hitta ett befintligt GlanceId. Detta är säkrare nu
                                    val glanceId = manager.getGlanceIds(LinearClockWidget::class.java).firstOrNull()

                                    if (glanceId != null) {
                                        // Vi specificerar typen <Preferences> explicit för att hjälpa kompilatorn
                                        updateAppWidgetState<Preferences>(ctx, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                                            prefs[LinearClockPrefs.FONT_FAMILY] = font
                                            prefs[LinearClockPrefs.FONT_SCALE] = scale
                                            prefs[LinearClockPrefs.COLOR_BG] = bg
                                            prefs[LinearClockPrefs.COLOR_TEXT] = text
                                            prefs[LinearClockPrefs.COLOR_ACCENT] = accent
                                            prefs[LinearClockPrefs.HOURS_TO_SHOW] = hoursToShow
                                        }
                                        // Rätt anrop: LinearClockWidget är ett objekt, inga parenteser
                                        LinearClockWidget.update(ctx, glanceId)
                                    }

                                    val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                    setResult(RESULT_OK, result)
                                    finish()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Spara & Slutför") }
                    }
                }
            }
        }
    }
}
