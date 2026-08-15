package com.scanrobot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BgWhite,
    secondary = TealAccent,
    onSecondary = BgWhite,
    background = BgLight,
    onBackground = TextPrimary,
    surface = BgWhite,
    onSurface = TextPrimary,
    error = DangerRed,
    onError = BgWhite
)

@Composable
fun ScanRobotTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
