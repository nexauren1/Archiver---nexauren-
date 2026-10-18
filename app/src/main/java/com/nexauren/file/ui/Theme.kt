package com.nexauren.file.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexaurenScheme = darkColorScheme(
    primary = Color(0xFFFFA726),
    onPrimary = Color(0xFF171717),
    secondary = Color(0xFF9CCC65),
    onSecondary = Color(0xFF171717),
    tertiary = Color(0xFF64B5F6),
    background = Color(0xFF121212),
    surface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFF292929),
    onBackground = Color(0xFFF4F4F4),
    onSurface = Color(0xFFF4F4F4),
    onSurfaceVariant = Color(0xFFBDBDBD)
)

@Composable
fun NexaurenFileTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NexaurenScheme, content = content)
}
