package com.nexauren.file.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexaurenScheme = lightColorScheme(
    primary = Color(0xFF4B5DFF),
    secondary = Color(0xFF00A8A8),
    background = Color(0xFFF7F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEFF1F8),
    onBackground = Color(0xFF171925),
    onSurface = Color(0xFF171925)
)

@Composable
fun NexaurenFileTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NexaurenScheme, content = content)
}
