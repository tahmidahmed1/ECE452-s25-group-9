package com.example.gooddeedfeed.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object LightColors {
    val primary = Color(0xFF6366F1)
    val primaryVariant = Color(0xFF4F46E5)
    val secondary = Color(0xFF06B6D4)
    val tertiary = Color(0xFF8B5CF6)
    val background = Color(0xFFFAFAFA)
    val surface = Color(0xFFFFFFFF)
    val surfaceVariant = Color(0xFFF3F4F6)
    val surfaceContainer = Color(0xFFE5E7EB)
    val onPrimary = Color.White
    val onSecondary = Color.White
    val onTertiary = Color.White
    val onBackground = Color(0xFF1F2937)
    val onSurface = Color(0xFF374151)
    val onSurfaceVariant = Color(0xFF6B7280)
    val outline = Color(0xFFD1D5DB)
    val outlineVariant = Color(0xFFE5E7EB)
    val error = Color(0xFFEF4444)
    val onError = Color.White
    val success = Color(0xFF10B981)
    val warning = Color(0xFFF59E0B)
}

object DarkColors {
    val primary = Color(0xFF818CF8)
    val primaryVariant = Color(0xFF6366F1)
    val secondary = Color(0xFF22D3EE)
    val tertiary = Color(0xFFA78BFA)
    val background = Color(0xFF0F0F0F)
    val surface = Color(0xFF1F1F1F)
    val surfaceVariant = Color(0xFF2D2D2D)
    val surfaceContainer = Color(0xFF3A3A3A)
    val onPrimary = Color(0xFF1E1B4B)
    val onSecondary = Color(0xFF0C4A6E)
    val onTertiary = Color(0xFF4C1D95)
    val onBackground = Color(0xFFF9FAFB)
    val onSurface = Color(0xFFE5E7EB)
    val onSurfaceVariant = Color(0xFF9CA3AF)
    val outline = Color(0xFF4B5563)
    val outlineVariant = Color(0xFF374151)
    val error = Color(0xFFFC8181)
    val onError = Color(0xFF7F1D1D)
    val success = Color(0xFF68D391)
    val warning = Color(0xFFFBD38D)
}

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object BorderRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val full = 9999.dp
}

object Elevation {
    val none = 0.dp
    val sm = 1.dp
    val md = 4.dp
    val lg = 8.dp
    val xl = 16.dp
}

object AnimationDurations {
    const val fast = 150
    const val normal = 300
    const val slow = 500
}

object AnimationSpecs {
    val fastSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh,
    )
    val normalSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val slowSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )
}

@Composable
fun appTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()

    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = DarkColors.primary,
            onPrimary = DarkColors.onPrimary,
            primaryContainer = DarkColors.primaryVariant,
            onPrimaryContainer = DarkColors.onPrimary,
            secondary = DarkColors.secondary,
            onSecondary = DarkColors.onSecondary,
            secondaryContainer = DarkColors.secondary.copy(alpha = 0.3f),
            onSecondaryContainer = DarkColors.secondary,
            tertiary = DarkColors.tertiary,
            onTertiary = DarkColors.onTertiary,
            tertiaryContainer = DarkColors.tertiary.copy(alpha = 0.3f),
            onTertiaryContainer = DarkColors.tertiary,
            background = DarkColors.background,
            onBackground = DarkColors.onBackground,
            surface = DarkColors.surface,
            onSurface = DarkColors.onSurface,
            surfaceVariant = DarkColors.surfaceVariant,
            onSurfaceVariant = DarkColors.onSurfaceVariant,
            surfaceContainer = DarkColors.surfaceContainer,
            surfaceContainerHigh = DarkColors.surfaceContainer.copy(alpha = 0.8f),
            surfaceContainerHighest = DarkColors.surfaceContainer.copy(alpha = 0.6f),
            outline = DarkColors.outline,
            outlineVariant = DarkColors.outlineVariant,
            error = DarkColors.error,
            onError = DarkColors.onError,
            errorContainer = DarkColors.error.copy(alpha = 0.3f),
            onErrorContainer = DarkColors.error,
        )
    } else {
        lightColorScheme(
            primary = LightColors.primary,
            onPrimary = LightColors.onPrimary,
            primaryContainer = LightColors.primary.copy(alpha = 0.1f),
            onPrimaryContainer = LightColors.primaryVariant,
            secondary = LightColors.secondary,
            onSecondary = LightColors.onSecondary,
            secondaryContainer = LightColors.secondary.copy(alpha = 0.1f),
            onSecondaryContainer = LightColors.secondary,
            tertiary = LightColors.tertiary,
            onTertiary = LightColors.onTertiary,
            tertiaryContainer = LightColors.tertiary.copy(alpha = 0.1f),
            onTertiaryContainer = LightColors.tertiary,
            background = LightColors.background,
            onBackground = LightColors.onBackground,
            surface = LightColors.surface,
            onSurface = LightColors.onSurface,
            surfaceVariant = LightColors.surfaceVariant,
            onSurfaceVariant = LightColors.onSurfaceVariant,
            surfaceContainer = LightColors.surfaceContainer,
            surfaceContainerHigh = LightColors.surfaceContainer.copy(alpha = 0.8f),
            surfaceContainerHighest = LightColors.surfaceContainer.copy(alpha = 0.6f),
            outline = LightColors.outline,
            outlineVariant = LightColors.outlineVariant,
            error = LightColors.error,
            onError = LightColors.onError,
            errorContainer = LightColors.error.copy(alpha = 0.1f),
            onErrorContainer = LightColors.error,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
