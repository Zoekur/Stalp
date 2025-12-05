package com.dagsbalken.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun DagsbalkenTheme(
    themeOption: ThemeOption,
    dynamicColorEnabled: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = Typography,
        content = content
    )
}
