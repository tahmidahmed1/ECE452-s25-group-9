package com.example.gooddeedfeed.presentation.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SplashScreen(
    authState: AuthUiState,
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Early location permission detection
    LaunchedEffect(locationPermissionState.status) {
        val hasLocationPermission = locationPermissionState.status.isGranted
        // Log location permission status for debugging
        android.util.Log.d("SplashScreen", "Location permission status: $hasLocationPermission")

        // Store location permission status for later use
        if (!hasLocationPermission) {
            android.util.Log.w("SplashScreen", "Location permission not granted - user will see location prompts")
        }
    }

    // As soon as we have a definitive auth state, move on
    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success, is AuthUiState.SignedOut -> onSplashFinished()
            else -> Unit
        }
    }

    // Minimal placeholder while the auth check completes
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GoodDeedFeed",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Making the world better, one deed at a time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
} 
