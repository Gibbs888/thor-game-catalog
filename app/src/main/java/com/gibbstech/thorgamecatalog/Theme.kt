package com.gibbstech.thorgamecatalog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ThorDarkColors = darkColorScheme(
    primary = Color(0xFF9A83FF),
    onPrimary = Color(0xFF16005E),
    primaryContainer = Color(0xFF2B1B66),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF2EE6A6),
    onSecondary = Color(0xFF003825),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFF1F3F7),
    surface = Color(0xFF111620),
    onSurface = Color(0xFFF1F3F7),
    surfaceVariant = Color(0xFF1B2230),
    onSurfaceVariant = Color(0xFFBEC6D5),
    outline = Color(0xFF3B4558),
)

@Composable
fun ThorCatalogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ThorDarkColors,
        content = content,
    )
}
