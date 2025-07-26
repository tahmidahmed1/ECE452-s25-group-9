package com.example.gooddeedfeed.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A composable that displays a shutter-style loading animation.
 * This mimics the effect of animated vertical bars (shutters) sliding or fading in sequence.
 */
@Composable
fun ShutterLoadingView(
    modifier: Modifier = Modifier,
    barColor: Color = Color.LightGray,
    barCount: Int = 5,
    barWidth: Dp = 16.dp,
    barHeight: Dp = 64.dp,
    barSpacing: Dp = 8.dp,
    animationDuration: Int = 800,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shutter-loading")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
    ) {
        for (i in 0 until barCount) {
            val delay = i * (animationDuration / barCount)
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(animationDuration, delayMillis = delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar-alpha-$i",
            )
            Box(
                modifier = Modifier
                    .size(width = barWidth, height = barHeight)
                    .alpha(alpha)
                    .background(barColor),
            )
        }
    }
}
