package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gooddeedfeed.presentation.ui.theme.CornerRadius
import com.example.gooddeedfeed.presentation.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    authState: com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState = com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState.Idle,
    modifier: Modifier = Modifier,
) {
    // Animation for the loading bar
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 2000,
            easing = FastOutSlowInEasing,
        ),
        label = "progress",
    )

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gradient",
    )

    // Text fade in animation
    val textAlpha by animateFloatAsState(
        targetValue = if (progress > 0.1f) 1f else 0f,
        animationSpec = tween(800),
        label = "textAlpha",
    )

    LaunchedEffect(Unit) {
        // Start loading animation
        progress = 1f
        // Wait minimum time for animation
        delay(1500)
    }

    // Watch for auth state changes after minimum delay
    LaunchedEffect(authState) {
        if (progress >= 1f) { // Only proceed if animation has started
            when (authState) {
                is com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState.Success,
                is com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState.SignedOut,
                -> {
                    // Auth state is determined, finish splash
                    delay(300) // Small delay to let user see completed animation
                    onSplashFinished()
                }
                else -> {
                    // Still loading auth state, wait
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background,
                    ),
                    startY = gradientOffset * 1000f,
                    endY = (gradientOffset + 0.5f) * 1000f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(Spacing.extraLarge),
        ) {
            // App Title
            Text(
                text = "GoodDeedFeed",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 42.sp,
                    letterSpacing = (-0.5).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha),
                modifier = Modifier.padding(bottom = Spacing.small),
            )

            // Subtitle
            Text(
                text = "Making the world better, one deed at a time",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha * 0.8f),
                modifier = Modifier.padding(bottom = Spacing.xxLarge),
            )

            Spacer(modifier = Modifier.height(Spacing.xxLarge))

            // Loading Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(240.dp),
            ) {
                // Loading Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(CornerRadius.small))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(CornerRadius.small))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.tertiary,
                                    ),
                                ),
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Loading Text
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
} 
