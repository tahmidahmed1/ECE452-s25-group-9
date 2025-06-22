package com.example.gooddeedfeed.presentation.ui.components.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Dot indicators to show onboarding progress
 */
@Composable
fun OnboardingDotIndicators(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < currentStep
            val isSelected = index == currentStep - 1

            Box(
                modifier = Modifier
                    .size(
                        if (isSelected) 12.dp else 8.dp,
                    )
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                    ),
            )
        }
    }
}

/**
 * Section header for volunteer profile fields
 */
@Composable
fun ProfileSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Chip selection component for skills
 */
@Composable
fun SkillChip(
    text: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = { Text(text) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
} 
