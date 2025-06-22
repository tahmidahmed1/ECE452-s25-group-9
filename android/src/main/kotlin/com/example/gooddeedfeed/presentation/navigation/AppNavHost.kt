package com.example.gooddeedfeed.presentation.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.components.CustomToastHost
import com.example.gooddeedfeed.presentation.ui.components.ToastManager
import com.example.gooddeedfeed.presentation.ui.components.rememberToastState
import com.example.gooddeedfeed.presentation.ui.screens.auth.SignInScreen
import com.example.gooddeedfeed.presentation.ui.screens.auth.SignUpScreen
import com.example.gooddeedfeed.presentation.ui.screens.onboarding.OnboardingScreen
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthViewModel

sealed class Screen(val route: String) {
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

    // Handle authentication state navigation
    LaunchedEffect(currentState) {
        when (currentState) {
            is AuthUiState.SignedOut -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != Screen.SignIn.route) {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthUiState.Success -> {
                val currentRoute = navController.currentDestination?.route
                val user = currentState.user

                Log.d("AppNavHost", "AuthUiState.Success - current route: $currentRoute, user: ${user.username}, onboarding: ${user.onboardingCompleted}")

                // If user is authenticated but on auth screens, navigate appropriately
                if (currentRoute == Screen.SignIn.route || currentRoute == Screen.SignUp.route) {
                    if (!user.onboardingCompleted) {
                        Log.d("AppNavHost", "Navigating to onboarding")
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(currentRoute) { inclusive = true }
                        }
                    } else {
                        Log.d("AppNavHost", "Navigating to authenticated home")
                        navController.navigate(Screen.AuthenticatedHome.route) {
                            popUpTo(currentRoute) { inclusive = true }
                        }
                    }
                }
            }
            else -> {}
        }
    }

    Box {
        NavHost(navController, startDestination = Screen.SignIn.route) {
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
                        println("AppNavHost: Missing userType, defaulting to VOLUNTEER")
                        user.copy(userType = DomainUserType.VOLUNTEER)
                    } else {
                        user
                    }
                    TabNavigationScreen(user = userWithType, onLogout = { viewModel.signOut() })
                }
            }
        }

        // Add toast overlay for authentication screens
        CustomToastHost(
            toastData = toastState,
            onDismiss = { ToastManager.dismiss() },
        )
    }
}
