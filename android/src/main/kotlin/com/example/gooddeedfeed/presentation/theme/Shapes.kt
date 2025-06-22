package com.example.gooddeedfeed.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

// Modern Shape System
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(BorderRadius.xs),     // 4dp - for small chips, badges
    small = RoundedCornerShape(BorderRadius.sm),          // 8dp - for buttons, small cards
    medium = RoundedCornerShape(BorderRadius.md),         // 12dp - for cards, dialogs
    large = RoundedCornerShape(BorderRadius.lg),          // 16dp - for large cards, sheets
    extraLarge = RoundedCornerShape(BorderRadius.xl)      // 20dp - for bottom sheets, large surfaces
) 