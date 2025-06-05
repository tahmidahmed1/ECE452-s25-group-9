package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gooddeedfeed.presentation.ui.screens.homeScreen
import com.example.gooddeedfeed.presentation.ui.screens.signInScreen
import com.example.gooddeedfeed.presentation.ui.screens.signUpScreen
import com.example.gooddeedfeed.presentation.viewmodel.AuthUiState
import com.example.gooddeedfeed.presentation.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")

    object SignUp : Screen("sign_up")

    object Home : Screen("home")
}

@Composable
fun appNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is AuthUiState.Success -> navController.navigate(Screen.Home.route) { popUpTo(0) }
        is AuthUiState.SignedOut -> navController.navigate(Screen.SignIn.route) { popUpTo(0) }
        else -> {}
    }

    NavHost(navController, startDestination = Screen.SignIn.route) {
        composable(Screen.SignIn.route) {
            signInScreen(
                uiState = uiState,
                onSignIn = { u, p -> viewModel.signIn(u, p) },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
            )
        }
        composable(Screen.SignUp.route) {
            signUpScreen(
                uiState = uiState,
                onSignUp = { u, p -> viewModel.signUp(u, p) },
                onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
            )
        }
        composable(Screen.Home.route) {
            val user = (uiState as? AuthUiState.Success)?.user
            if (user != null) {
                homeScreen(user = user, onLogout = { viewModel.signOut() })
            }
        }
    }
}
