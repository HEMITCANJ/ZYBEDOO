package io.github.hemitcanj.zybedoo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ZBackground = Color(0xFF0D0919)
val ZSurface = Color(0xFF171126)
val ZSurfaceHigh = Color(0xFF241B3B)
val ZPrimary = Color(0xFF8B6BFF)
val ZOnPrimary = Color(0xFFFFFFFF)
val ZSecondary = Color(0xFFB8A2FF)
val ZText = Color(0xFFEDE7FF)
val ZTextDim = Color(0xFF9B8FC0)
val ZError = Color(0xFFFF6B8A)
val ZOk = Color(0xFF5CE0A6)
val ZOutline = Color(0xFF3A2E58)

private val ZColorScheme = darkColorScheme(
    primary = ZPrimary,
    onPrimary = ZOnPrimary,
    secondary = ZSecondary,
    onSecondary = ZBackground,
    background = ZBackground,
    onBackground = ZText,
    surface = ZSurface,
    onSurface = ZText,
    surfaceVariant = ZSurfaceHigh,
    onSurfaceVariant = ZTextDim,
    error = ZError,
    outline = ZOutline,
)

@Composable
fun ZBedooTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZColorScheme,
        typography = Typography(),
        content = content,
    )
}
