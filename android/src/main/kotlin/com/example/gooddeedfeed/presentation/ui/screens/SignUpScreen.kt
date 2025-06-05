package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.gooddeedfeed.presentation.theme.PADDING_LARGE
import com.example.gooddeedfeed.presentation.theme.PADDING_MEDIUM
import com.example.gooddeedfeed.presentation.theme.PADDING_SMALL
import com.example.gooddeedfeed.presentation.viewmodel.AuthUiState

@Composable
fun signUpScreen(
    uiState: AuthUiState,
    onSignUp: (String, String) -> Unit,
    onNavigateToSignIn: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = uiState is AuthUiState.Loading
    val error = (uiState as? AuthUiState.Error)?.message

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(PADDING_MEDIUM),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PADDING_LARGE),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Sign Up",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(PADDING_MEDIUM))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(PADDING_SMALL))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(PADDING_LARGE))
            Button(
                onClick = { onSignUp(username, password) },
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign Up")
            }
            Spacer(Modifier.height(PADDING_MEDIUM))
            TextButton(onClick = onNavigateToSignIn, enabled = !isLoading) {
                Text("Already have an account? Sign In")
            }
            if (error != null) {
                Spacer(Modifier.height(PADDING_SMALL))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
