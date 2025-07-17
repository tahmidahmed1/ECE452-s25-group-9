package com.example.gooddeedfeed.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

data class ToastData(
    val message: String,
    val type: ToastType,
    val duration: Long = 3000L, // Changed default to 3 seconds
)

@Composable
fun CustomToastHost(
    toastData: ToastData?,
    onDismiss: () -> Unit,
) {
    var visible by remember(toastData) { mutableStateOf(false) }

    LaunchedEffect(toastData) {
        if (toastData != null) {
            visible = true
            delay(toastData.duration)
            visible = false
            delay(300) // Wait for exit animation
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible && toastData != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300),
        ) + fadeOut(),
    ) {
        if (toastData != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp), // Increased spacing from edges
                contentAlignment = Alignment.BottomCenter,
            ) {
                CustomToast(
                    message = toastData.message,
                    type = toastData.type,
                    onDismiss = {
                        visible = false
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun CustomToast(
    message: String,
    type: ToastType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxWidth = (screenWidth * 0.9f).coerceAtMost(400.dp)

    var offsetX by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = with(density) { 100.dp.toPx() }

    val (backgroundColor, contentColor, icon) = when (type) {
        ToastType.SUCCESS -> Triple(
            Color(0xFF4CAF50),
            Color.White,
            Icons.Default.CheckCircle,
        )
        ToastType.ERROR -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            Icons.Default.Close,
        )
        ToastType.WARNING -> Triple(
            Color(0xFFFF9800),
            Color.White,
            Icons.Default.Warning,
        )
        ToastType.INFO -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            Icons.Default.CheckCircle,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(999f),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 200.dp, max = maxWidth)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (abs(offsetX) > dismissThreshold) {
                                onDismiss()
                            } else {
                                offsetX = 0f
                            }
                        },
                    ) { _, dragAmount ->
                        offsetX += dragAmount.x
                    }
                }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor,
            ),
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )

                Text(
                    text = message,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                    lineHeight = 18.sp,
                )
            }
        }
    }
} 
