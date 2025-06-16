package com.example.gooddeedfeed.presentation.navigation

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
import com.example.gooddeedfeed.data.remote.UserType
import com.example.gooddeedfeed.presentation.ui.components.CustomToastHost
import com.example.gooddeedfeed.presentation.ui.components.ToastManager
import com.example.gooddeedfeed.presentation.ui.components.rememberToastState
import com.example.gooddeedfeed.presentation.ui.screens.onboarding.OnboardingScreen
import com.example.gooddeedfeed.presentation.ui.screens.SignInScreen
import com.example.gooddeedfeed.presentation.ui.screens.SignUpScreen
import com.example.gooddeedfeed.presentation.viewmodel.AuthUiState
import com.example.gooddeedfeed.presentation.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")

    object SignUp : Screen("sign_up")

    object Onboarding : Screen("onboarding")

    object AuthenticatedHome : Screen("authenticated_home")
}

@Composable
fun appNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentState = uiState // Store in local variable to enable smart cast
    val toastState by rememberToastState()

    // Handle sign out navigation
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
            else -> {}
        }
    }

    Box {
        NavHost(navController, startDestination = Screen.SignIn.route) {
        composable(Screen.SignIn.route) {
            SignInScreen(
                uiState = currentState,
                onSignIn = { u, p -> viewModel.signIn(u, p) },
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
                // Temporary fix: If user has no user_type (likely during development/testing),
                // default to VOLUNTEER to show the full navigation
                val userWithType = if (user.user_type == null) {
                    println("AppNavHost: User has null user_type, defaulting to VOLUNTEER for development")
                    user.copy(user_type = UserType.VOLUNTEER)
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
            onDismiss = { ToastManager.dismiss() }
        )
    }
}
