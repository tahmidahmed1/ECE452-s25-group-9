package com.example.gooddeedfeed.presentation.ui.screens

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
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    authState: AuthUiState = AuthUiState.Idle,
    modifier: Modifier = Modifier,
) {
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
