package com.example.gooddeedfeed.presentation.ui.components.base

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.presentation.theme.BorderRadius
import com.example.gooddeedfeed.presentation.theme.Elevation
import com.example.gooddeedfeed.presentation.theme.ModernAnimatedVisibility
import com.example.gooddeedfeed.presentation.theme.Spacing
import com.example.gooddeedfeed.presentation.theme.bounceClick
import com.example.gooddeedfeed.presentation.theme.shake

/**
 * Modern form input field with enhanced styling and animations
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = spring(),
        label = "borderColor",
    )

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label, style = MaterialTheme.typography.bodyMedium) },
            enabled = enabled,
            isError = isError,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .shake(enabled = isError),
            shape = RoundedCornerShape(BorderRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = borderColor,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        if (isError && errorMessage != null) {
            ModernAnimatedVisibility(visible = true) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
                )
            }
        }
    }
}

/**
 * Enhanced primary button with modern styling and animations
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val buttonColor by animateColorAsState(
        targetValue = if (enabled && !isLoading) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        },
        label = "buttonColor",
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(enabled = enabled && !isLoading, onClick = onClick)
            .shadow(
                elevation = Elevation.md,
                shape = RoundedCornerShape(BorderRadius.md),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(BorderRadius.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = buttonColor,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevation.sm,
            pressedElevation = Elevation.none,
            disabledElevation = Elevation.none,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * Enhanced secondary button with modern styling
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.bounceClick(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(BorderRadius.md),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
} 