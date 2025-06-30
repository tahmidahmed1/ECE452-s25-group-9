package com.example.gooddeedfeed.presentation.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.components.CustomToastHost
import com.example.gooddeedfeed.presentation.ui.components.ToastManager
import com.example.gooddeedfeed.presentation.ui.components.rememberToastState
import com.example.gooddeedfeed.presentation.ui.screens.SplashScreen
import com.example.gooddeedfeed.presentation.ui.screens.auth.SignInScreen
import com.example.gooddeedfeed.presentation.ui.screens.auth.SignUpScreen
import com.example.gooddeedfeed.presentation.ui.screens.onboarding.OnboardingScreen
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthViewModel

private const val TAG = "AppNavHost"

sealed class Screen(val route: String) {
    object Splash : Screen("splash")

    object SignIn : Screen("sign_in")

    object SignUp : Screen("sign_up")

    object Onboarding : Screen("onboarding")

    object AuthenticatedHome : Screen("authenticated_home")
}

@Composable
fun appNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentState = uiState // Store in local variable to enable smart cast
    val toastState by rememberToastState()

    // Navigate to SignIn on sign-out
    // Skip the toast on first launch when the user is already signed out
    val hasProcessedInitialSignedOut = remember { mutableStateOf(false) }

    LaunchedEffect(currentState) {
        if (currentState is AuthUiState.SignedOut) {
            if (hasProcessedInitialSignedOut.value) {
                // Real sign-out triggered in-app – show confirmation
            ToastManager.showSuccess("Signed out successfully")
            } else {
                // Initial SignedOut (no prior user session)
                hasProcessedInitialSignedOut.value = true
            }

            navController.navigate(Screen.SignIn.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        NavHost(navController, startDestination = Screen.Splash.route) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    authState = currentState,
                    onSplashFinished = {
                        // Don't navigate yet - let the auth state handler decide
                        when (currentState) {
                            is AuthUiState.Success -> {
                                val user = currentState.user
                                if (!user.onboardingCompleted) {
                                    navController.navigate(Screen.Onboarding.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.AuthenticatedHome.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            }
                            is AuthUiState.SignedOut -> {
                                navController.navigate(Screen.SignIn.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                            else -> {
                                // Still loading auth state, stay on splash
                            }
                        }
                    },
                )
            }

            composable(Screen.SignIn.route) {
                SignInScreen(
                    uiState = currentState,
                    onSignIn = { u, p -> viewModel.signIn(u, p) },
                    onDevModeSignIn = { userType -> viewModel.devModeSignIn(userType) },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.AuthenticatedHome.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    uiState = currentState,
                    onSignUp = { u, e, p -> viewModel.signUp(u, e, p) },
                    onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.AuthenticatedHome.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        // Refresh user data and navigate to home
                        viewModel.refreshUser()
                        navController.navigate(Screen.AuthenticatedHome.route) {
                            popUpTo(0)
                        }
                    },
                )
            }
            composable(Screen.AuthenticatedHome.route) {
                val user = (currentState as? AuthUiState.Success)?.user
                if (user != null) {
                    // Default missing userType to VOLUNTEER for dev preview
                    val userWithType = if (user.userType == null) {
                        user.copy(userType = DomainUserType.VOLUNTEER)
                    } else {
                        user
                    }
                    TabNavigationScreen(user = userWithType, onLogout = { viewModel.signOut() })
                }
            }
        }

        // Global toast overlay positioned at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.BottomCenter,
        ) {
            CustomToastHost(
                toastData = toastState,
                onDismiss = { ToastManager.dismiss() },
            )
        }
    }
}
