package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.presentation.ui.components.ToastManager
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton

@Composable
fun PrivacySettingsScreen(
    onClose: () -> Unit,
    initialNotificationsEnabled: Boolean = true,
    initialLocationEnabled: Boolean = true,
    initialShareProfilePicture: Boolean = true,
    onSave: (notificationsEnabled: Boolean, locationEnabled: Boolean, shareProfilePicture: Boolean) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    var notificationsEnabled by remember { mutableStateOf(initialNotificationsEnabled) }
    var locationEnabled by remember { mutableStateOf(initialLocationEnabled) }
    var shareProfilePictureEnabled by remember { mutableStateOf(initialShareProfilePicture) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy & Notifications",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Divider()

            SettingToggleRow(
                title = "Enable Notifications",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it },
            )
            SettingToggleRow(
                title = "Enable Location Services",
                checked = locationEnabled,
                onCheckedChange = { locationEnabled = it },
            )
            SettingToggleRow(
                title = "Share Profile Picture",
                checked = shareProfilePictureEnabled,
                onCheckedChange = { shareProfilePictureEnabled = it },
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = "Save",
                onClick = {
                    // Show success toast
                    ToastManager.showSuccess("Settings saved successfully")
                    onSave(notificationsEnabled, locationEnabled, shareProfilePictureEnabled)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Add bottom padding below the button
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        RoundedRectangleToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun RoundedRectangleToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedOffset by animateFloatAsState(
        targetValue = if (checked) 24f else 4f,
        animationSpec = tween(durationMillis = 200),
        label = "toggle_offset",
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = 200),
        label = "toggle_background",
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "toggle_thumb",
    )

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = animatedOffset.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(thumbColor),
        )
    }
} 
