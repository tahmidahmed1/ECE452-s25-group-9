package com.example.gooddeedfeed.presentation.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.gooddeedfeed.presentation.ui.components.FormTextField
import com.example.gooddeedfeed.presentation.ui.components.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.ScreenTitle
import com.example.gooddeedfeed.presentation.ui.components.SecondaryButton
import com.example.gooddeedfeed.presentation.ui.components.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.ToastUtils
import com.example.gooddeedfeed.presentation.ui.components.VerticalSpacer
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState

@Composable
fun SignUpScreen(
    uiState: AuthUiState,
    onSignUp: (String, String, String) -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = uiState is AuthUiState.Loading
    val context = LocalContext.current

    // Handle success and error states with toasts and navigation
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                ToastUtils.showSuccessToast(context, "Account created successfully! Welcome to GoodDeedFeed!")
                // Navigate based on onboarding status
                val user = uiState.user
                if (!user.onboardingCompleted) {
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
        ScreenTitle(text = "Create Account")
        
        VerticalSpacer()
        
        FormTextField(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            enabled = !isLoading
        )
        
        VerticalSpacer(SpacingSize.Small)
        
        FormTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
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
            text = "Sign Up",
            onClick = { onSignUp(username, email, password) },
            enabled = username.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            isLoading = isLoading
        )
        
        VerticalSpacer()
        
        SecondaryButton(
            text = "Already have an account? Sign In",
            onClick = onNavigateToSignIn,
            enabled = !isLoading
        )
    }
}
