package com.example.gooddeedfeed.presentation.ui.components.base

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.presentation.theme.DarkColors
import com.example.gooddeedfeed.presentation.theme.LightColors
import com.example.gooddeedfeed.presentation.theme.Spacing

/**
 * Modern screen container with gradient background and improved spacing
 */
@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                DarkColors.background,
                DarkColors.background.copy(alpha = 0.95f),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                LightColors.background,
                LightColors.surface,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/**
 * Consistent screen title component
 */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

/**
 * Consistent subtitle component
 */
@Composable
fun ScreenSubtitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/**
 * Modern spacing components with theme integration
 */
@Composable
fun VerticalSpacer(size: SpacingSize = SpacingSize.Medium) {
    Spacer(modifier = Modifier.height(size.dp))
}

@Composable
fun HorizontalSpacer(size: SpacingSize = SpacingSize.Medium) {
    Spacer(modifier = Modifier.width(size.dp))
}

enum class SpacingSize(val dp: Dp) {
    Small(Spacing.sm),
    Medium(Spacing.md),
    Large(Spacing.lg),
    ExtraLarge(Spacing.xl),
}

/**
 * Section header component
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
} 