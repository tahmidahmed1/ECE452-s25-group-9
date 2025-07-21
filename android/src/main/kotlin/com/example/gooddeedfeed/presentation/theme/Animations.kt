package com.example.gooddeedfeed.presentation.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.debugInspectorInfo
import kotlin.math.sin

object AppAnimations {
    val fadeIn = fadeIn(
        animationSpec = tween(
            durationMillis = AnimationDurations.normal,
            easing = FastOutSlowInEasing,
        ),
    )

    val fadeOut = fadeOut(
        animationSpec = tween(
            durationMillis = AnimationDurations.fast,
            easing = FastOutLinearInEasing,
        ),
    )

    val scaleIn = scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        initialScale = 0.8f,
    )

    val scaleOut = scaleOut(
        animationSpec = tween(
            durationMillis = AnimationDurations.fast,
            easing = FastOutLinearInEasing,
        ),
        targetScale = 0.8f,
    )

    val slideInFromBottom = slideInVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        initialOffsetY = { it / 2 },
    )

    val slideOutToBottom = slideOutVertically(
        animationSpec = tween(
            durationMillis = AnimationDurations.normal,
            easing = FastOutLinearInEasing,
        ),
        targetOffsetY = { it / 2 },
    )

    val enterAnimation = fadeIn + scaleIn + slideInFromBottom
    val exitAnimation = fadeOut + scaleOut + slideOutToBottom
}

@Composable
fun ModernAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "AnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = AppAnimations.enterAnimation,
        exit = AppAnimations.exitAnimation,
        label = label,
        content = content,
    )
}

fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "bounceClick"
        properties["enabled"] = enabled
        properties["onClick"] = onClick
    },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "bounceScale",
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

fun Modifier.hoverScale(
    scale: Float = 1.05f,
    enabled: Boolean = true,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "hoverScale"
        properties["scale"] = scale
        properties["enabled"] = enabled
    },
) {
    var isHovered by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered && enabled) scale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "hoverScale",
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
}

@Composable
fun Modifier.shake(
    enabled: Boolean = false,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "shake"
        properties["enabled"] = enabled
    },
) {
    val shakeOffset by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "shakeOffset",
    )

    this.graphicsLayer {
        translationX = shakeOffset * 10f * sin(shakeOffset * 20f)
    }
}
