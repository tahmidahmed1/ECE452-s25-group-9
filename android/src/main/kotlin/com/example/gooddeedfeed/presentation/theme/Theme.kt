package com.example.gooddeedfeed.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Color constants for existing glass theme (can be kept for dark mode or other uses)
val GLASS_BACKGROUND = Color(0x66FFFFFF)
val GLASS_OVERLAY = Color(0xCC1A1A1A)
val ERROR_COLOR = Color.Red
val TEXT_ON_GLASS = Color.White

// New Minimalist Light Theme Colors
val LightPrimary = Color(0xFF6200EE) // A standard Material purple
val LightOnPrimary = Color.White
val LightBackground = Color.White
val LightSurface = Color(0xFFF7F7F7) // Slightly off-white for surfaces like cards
val LightOnBackground = Color.Black
val LightOnSurface = Color.Black
val LightError = Color.Red // Same as ERROR_COLOR

// DP constants
val PADDING_LARGE = 32.dp
val PADDING_MEDIUM = 24.dp
val PADDING_SMALL = 8.dp
val CORNER_RADIUS = 24.dp // Can be reduced for a more minimalist feel if desired, e.g., 8.dp or 12.dp
val BLUR_RADIUS = 24.dp

@Composable
fun appTheme(content: @Composable () -> Unit) {
    val colorScheme =
        if (isSystemInDarkTheme()) {
            darkColorScheme( // Keeps original dark theme
                background = GLASS_OVERLAY,
                surface = GLASS_BACKGROUND,
                onSurface = TEXT_ON_GLASS,
                error = ERROR_COLOR,
                // You might want to define primary, onPrimary etc. for dark theme as well
                primary = LightPrimary, // Example: using same primary
                onPrimary = LightOnPrimary,
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
        // Typography and Shapes can be further customized here for minimalism
        content = content,
    )
}
