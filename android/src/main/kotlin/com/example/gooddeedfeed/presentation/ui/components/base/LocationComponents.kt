package com.example.gooddeedfeed.presentation.ui.components.base

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.data.repository.LocationSettingsRepository
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

@Composable
fun LocationDeniedState(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "😕",
                    style = MaterialTheme.typography.headlineLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Uh Oh!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We need location access to show you nearby volunteer opportunities and help you find events in your area.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "How to enable location:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    LocationInstructionStep(
                        step = "1",
                        text = "Tap \"Open Settings\" below",
                    )

                    LocationInstructionStep(
                        step = "2",
                        text = "Find \"Permissions\" or \"App permissions\"",
                    )

                    LocationInstructionStep(
                        step = "3",
                        text = "Tap \"Location\" and select \"Allow\"",
                    )

                    LocationInstructionStep(
                        step = "4",
                        text = "Return to Good Deed Feed",
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Settings")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You can also enable location services in Privacy & Notifications settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LocationInstructionStep(
    step: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LocationPermissionHandler(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LocationDeniedState(
        onOpenSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        },
        modifier = modifier,
    )
}

/**
 * Comprehensive location permission manager that handles all permission states
 * and checks permission status on screen load
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionManager(
    locationPermissionState: PermissionState,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    if (locationPermissionState.status.isGranted) {
        content()
    } else {
        if (locationPermissionState.status.shouldShowRationale) {
            PermissionRationaleCard {
                locationPermissionState.launchPermissionRequest()
            }
        } else {
            LocationPermissionHandler(
                onOpenSettings = { /* This is handled within the component */ },
                modifier = modifier,
            )
        }
    }
}

/**
 * Enhanced location permission manager that also checks privacy settings
 * and provides better integration with the app's location toggle
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnhancedLocationPermissionManager(
    locationPermissionState: PermissionState,
    locationSettingsRepository: LocationSettingsRepository,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onLocationDisabled: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locationEnabled by locationSettingsRepository.isLocationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val context = LocalContext.current
    LaunchedEffect(locationPermissionState.status.isGranted, locationEnabled) {
        when {
            !locationPermissionState.status.isGranted -> {
                onPermissionDenied()
            }
            !locationEnabled -> {
                onLocationDisabled()
            }
            else -> {
                onPermissionGranted()
            }
        }
    }

    when {
        !locationPermissionState.status.isGranted -> {
            if (locationPermissionState.status.shouldShowRationale) {
                PermissionRationaleCard {
                    locationPermissionState.launchPermissionRequest()
                }
            } else {
                LocationPermissionHandler(
                    onOpenSettings = { /* This is handled within the component */ },
                    modifier = modifier,
                )
            }
        }
        !locationEnabled -> {
            LocationDisabledState(
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = modifier,
            )
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/**
 * State shown when location permission is granted but location services are disabled in app settings
 */
@Composable
fun LocationDisabledState(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⚙️",
                    style = MaterialTheme.typography.headlineLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Location Services Disabled",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Location permission is granted, but location services are disabled in your app settings. Enable them to see nearby events.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "How to enable location services:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    LocationInstructionStep(
                        step = "1",
                        text = "Tap \"Open Settings\" below",
                    )

                    LocationInstructionStep(
                        step = "2",
                        text = "Find \"Privacy & Notifications\"",
                    )

                    LocationInstructionStep(
                        step = "3",
                        text = "Enable \"Location Services\" toggle",
                    )

                    LocationInstructionStep(
                        step = "4",
                        text = "Return to Good Deed Feed",
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Open Settings")
            }
        }
    }
} 
