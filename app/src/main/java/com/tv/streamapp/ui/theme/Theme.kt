package com.tv.streamapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val TvBackground = Color(0xFF0B0F19)
val TvSurface = Color(0xFF131B2E)
val TvSurfaceFocused = Color(0xFF1E293B)
val TvBorder = Color(0xFF334155)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

@Composable
fun AndroidTvStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00C8FF),
            background = TvBackground,
            surface = TvSurface,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}
