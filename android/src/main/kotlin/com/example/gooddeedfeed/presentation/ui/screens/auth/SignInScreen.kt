package com.example.gooddeedfeed.presentation.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.BuildConfig
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.components.ToastUtils
import com.example.gooddeedfeed.presentation.ui.components.base.FormTextField
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.base.SecondaryButton
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo or App Icon placeholder
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "GDF",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "Sign in to continue making a difference",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                textAlign = TextAlign.Center,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FormTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        enabled = !isLoading,
                    )

                    FormTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        enabled = !isLoading,
                        isPassword = true,
                    )

                    PrimaryButton(
                        text = "Sign In",
                        onClick = { onSignIn(username, password) },
                        enabled = username.isNotBlank() && password.isNotBlank(),
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SecondaryButton(
                text = "Don't have an account? Sign Up",
                onClick = onNavigateToSignUp,
                enabled = !isLoading,
            )

            // Development Mode Section
            if (BuildConfig.DEV_MODE) {
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(16.dp),
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

                        Text(
                            text = "Quick sign-in with auto-generated accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )

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
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
