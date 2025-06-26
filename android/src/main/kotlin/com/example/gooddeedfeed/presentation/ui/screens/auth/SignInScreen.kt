package com.example.gooddeedfeed.presentation.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gooddeedfeed.BuildConfig
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.components.*
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState

@Composable
fun SignInScreen(
    uiState: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onDevModeSignIn: (DomainUserType) -> Unit = {},
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
                ToastUtils.showSuccessToast(context, "Welcome back to GoodDeedFeed!")
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
        ScreenTitle(text = "Welcome Back")

        VerticalSpacer()

        FormTextField(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            enabled = !isLoading,
        )

        VerticalSpacer(SpacingSize.Small)

        FormTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            enabled = !isLoading,
            isPassword = true,
        )

        VerticalSpacer(SpacingSize.Large)

        PrimaryButton(
            text = "Sign In",
            onClick = { onSignIn(username, password) },
            enabled = username.isNotBlank() && password.isNotBlank(),
            isLoading = isLoading,
        )

        VerticalSpacer()

        SecondaryButton(
            text = "Don't have an account? Sign Up",
            onClick = onNavigateToSignUp,
            enabled = !isLoading,
        )

        // Development Mode Section
        if (BuildConfig.DEV_MODE) {
            VerticalSpacer(SpacingSize.Large)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "🚀 Development Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    VerticalSpacer(SpacingSize.Small)

                    Text(
                        text = "Quick sign-in with auto-generated accounts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    VerticalSpacer(SpacingSize.Medium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DevModeButton(
                            icon = "👤",
                            text = "Volunteer",
                            userType = DomainUserType.VOLUNTEER,
                            onDevModeSignIn = onDevModeSignIn,
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f),
                        )

                        DevModeButton(
                            icon = "⭐",
                            text = "Organizer",
                            userType = DomainUserType.ORGANIZER,
                            onDevModeSignIn = onDevModeSignIn,
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f),
                        )

                        DevModeButton(
                            icon = "🏛️",
                            text = "Institution",
                            userType = DomainUserType.INSTITUTION,
                            onDevModeSignIn = onDevModeSignIn,
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevModeButton(
    icon: String,
    text: String,
    userType: DomainUserType,
    onDevModeSignIn: (DomainUserType) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = { onDevModeSignIn(userType) },
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
