package com.example.gooddeedfeed.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Legacy glass theme colors (deprecated - use MaterialTheme.colorScheme instead)
@Deprecated("Use MaterialTheme.colorScheme instead")
val GLASS_BACKGROUND = Color(0x66FFFFFF)
@Deprecated("Use MaterialTheme.colorScheme instead")
val GLASS_OVERLAY = Color(0xCC1A1A1A)
@Deprecated("Use MaterialTheme.colorScheme instead")
val ERROR_COLOR = Color.Red
@Deprecated("Use MaterialTheme.colorScheme instead")
val TEXT_ON_GLASS = Color.White

// Light Theme Colors (Minimalist)
val LightPrimary = Color(0xFF6200EE) // Material purple
val LightOnPrimary = Color.White
val LightBackground = Color.White
val LightSurface = Color(0xFFF7F7F7) // Slightly off-white for surfaces
val LightOnBackground = Color.Black
val LightOnSurface = Color.Black
val LightError = Color(0xFFD32F2F) // Material red

// Modern Dark Theme Colors (Darker grays)
val DarkPrimary = Color(0xFFBB86FC) // Material purple for dark theme
val DarkOnPrimary = Color.Black
val DarkBackground = Color(0xFF121212) // Very dark gray, almost black
val DarkSurface = Color(0xFF1E1E1E) // Dark gray for cards/surfaces
val DarkOnBackground = Color(0xFFE0E0E0) // Light gray text
val DarkOnSurface = Color(0xFFE0E0E0) // Light gray text
val DarkError = Color(0xFFCF6679) // Material red for dark theme

// DP constants
val PADDING_LARGE = 32.dp
val PADDING_MEDIUM = 24.dp
val PADDING_SMALL = 8.dp
val CORNER_RADIUS = 12.dp // Reduced for more modern minimalist feel
val BLUR_RADIUS = 24.dp

@Composable
fun appTheme(content: @Composable () -> Unit) {
    val colorScheme =
        if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = DarkPrimary,
                onPrimary = DarkOnPrimary,
                background = DarkBackground,
                surface = DarkSurface,
                onBackground = DarkOnBackground,
                onSurface = DarkOnSurface,
                error = DarkError,
            )
        } else {
            lightColorScheme(
                primary = LightPrimary,
                onPrimary = LightOnPrimary,
                background = LightBackground,
                surface = LightSurface,
                onBackground = LightOnBackground,
                onSurface = LightOnSurface,
                error = LightError,
            )
        }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
