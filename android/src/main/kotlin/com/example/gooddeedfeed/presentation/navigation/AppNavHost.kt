package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gooddeedfeed.presentation.ui.screens.onboarding.OnboardingScreen
import com.example.gooddeedfeed.presentation.ui.screens.signInScreen
import com.example.gooddeedfeed.presentation.ui.screens.signUpScreen
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

    NavHost(navController, startDestination = Screen.SignIn.route) {
        composable(Screen.SignIn.route) {
            signInScreen(
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
                }
            )
        }
        composable(Screen.SignUp.route) {
            signUpScreen(
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
                }
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
                TabNavigationScreen(user = user, onLogout = { viewModel.signOut() })
            }
        }
    }
}
