package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.gooddeedfeed.presentation.ui.components.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.ScreenTitle
import com.example.gooddeedfeed.presentation.ui.components.FormTextField
import com.example.gooddeedfeed.presentation.ui.components.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.SecondaryButton
import com.example.gooddeedfeed.presentation.ui.components.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.ToastUtils
import com.example.gooddeedfeed.presentation.viewmodel.AuthUiState

@Composable
fun SignInScreen(
    uiState: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = uiState is AuthUiState.Loading
    val context = LocalContext.current

    // Handle success and error states with toasts and navigation
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                ToastUtils.showSuccessToast(context, "Sign in successful! Welcome back!")
                // Navigate based on onboarding status
                val user = uiState.user
                if (!user.onboarding_completed) {
                    onNavigateToOnboarding()
                } else {
                    onNavigateToHome()
                }
            }
            is AuthUiState.Error -> {
                ToastUtils.showErrorToast(context, uiState.message)
            }
            else -> {}
        }
    }

    ScreenContainer {
        ScreenTitle("Sign In")
        
        VerticalSpacer()
        
        FormTextField(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            enabled = !isLoading
        )
        
        VerticalSpacer(SpacingSize.Small)
        
        FormTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            enabled = !isLoading,
            isPassword = true
        )
        
        VerticalSpacer(SpacingSize.Large)
        
        PrimaryButton(
            text = "Sign In",
            onClick = { onSignIn(username, password) },
            enabled = username.isNotBlank() && password.isNotBlank(),
            isLoading = isLoading
        )
        
        VerticalSpacer()
        
        SecondaryButton(
            text = "Don't have an account? Sign Up",
            onClick = onNavigateToSignUp,
            enabled = !isLoading
        )
    }
}
