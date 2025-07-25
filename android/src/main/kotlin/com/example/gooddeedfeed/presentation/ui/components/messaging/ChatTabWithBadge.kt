package com.example.gooddeedfeed.presentation.ui.components.messaging

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gooddeedfeed.presentation.ui.theme.Constants

@Composable
fun ChatTabIcon(
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = icon,
        contentDescription = "Chat",
        tint = if (isSelected) Constants.Colors.primary else Constants.Colors.darkGray,
        modifier = modifier
    )
}