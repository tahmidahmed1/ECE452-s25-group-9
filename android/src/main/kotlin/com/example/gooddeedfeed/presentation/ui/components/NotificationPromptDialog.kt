package com.example.gooddeedfeed.presentation.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPromptDialog(
    userType: DomainUserType,
    onEnableNotifications: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            onPermissionResult = { granted ->
                if (granted) {
                    onEnableNotifications()
                }
            },
        )
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "Stay Connected!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = when (userType) {
                        DomainUserType.VOLUNTEER ->
                            "Get notified when new volunteer opportunities are posted by organizations you follow."
                        DomainUserType.ORGANIZER ->
                            "Get notified about volunteer applications, event updates, and important announcements."
                        else ->
                            "Get notified about important updates and new opportunities."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "You can change this setting later in Privacy & Notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (notificationPermissionState != null && !notificationPermissionState.status.isGranted) {
                        notificationPermissionState.launchPermissionRequest()
                    } else {
                        onEnableNotifications()
                    }
                },
            ) {
                Text("Enable Notifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip for Now")
            }
        },
        modifier = modifier,
    )
}
