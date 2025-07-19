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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionCard(
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            onPermissionResult = { granted ->
                if (granted) {
                    onPermissionGranted()
                }
            },
        )
    } else {
        null
    }

    if (notificationPermissionState != null && !notificationPermissionState.status.isGranted) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Stay Updated!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (notificationPermissionState.status.shouldShowRationale) {
                        "Please enable notifications to get notified when organizations you follow post new volunteer opportunities."
                    } else {
                        "Get notified when new volunteer opportunities are posted by organizations you follow."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        notificationPermissionState.launchPermissionRequest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Enable Notifications")
                }
            }
        }
    } else if (notificationPermissionState?.status?.isGranted == true || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Permission granted or not needed, call the callback
        LaunchedEffect(Unit) {
            onPermissionGranted()
        }
    }
}
