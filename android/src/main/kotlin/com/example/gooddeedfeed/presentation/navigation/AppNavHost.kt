package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import com.example.gooddeedfeed.presentation.ui.components.ShutterLoadingView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.components.CustomToastHost
import com.example.gooddeedfeed.presentation.ui.components.ToastManager
import com.example.gooddeedfeed.presentation.ui.components.rememberToastState
import com.example.gooddeedfeed.presentation.ui.screens.EditOrganizerProfileScreen
import com.example.gooddeedfeed.presentation.ui.screens.EditVolunteerProfileScreen
import com.example.gooddeedfeed.presentation.ui.screens.SplashScreen
import com.example.gooddeedfeed.presentation.ui.screens.auth.SignInScreen
import com.example.gooddeedfeed.presentation.ui.screens.auth.SignUpScreen
import com.example.gooddeedfeed.presentation.ui.screens.onboarding.OnboardingScreen
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object Onboarding : Screen("onboarding")
    object AuthenticatedHome : Screen("authenticated_home")
    object EditVolunteerProfile : Screen("edit_volunteer_profile")
    object EditOrganizerProfile : Screen("edit_organizer_profile")
}

@Composable
fun appNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentState = uiState // Store in local variable to enable smart cast
    val toastState by rememberToastState()

    var isNavigatingFromOnboarding by remember { mutableStateOf(false) }
    var showLoadingOverlay by remember { mutableStateOf(false) }

    val hasProcessedInitialSignedOut = remember { mutableStateOf(false) }

    LaunchedEffect(currentState) {
        if (currentState is AuthUiState.SignedOut) {
            if (hasProcessedInitialSignedOut.value) {
                ToastManager.showSuccess("Signed out successfully")
            } else {
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
                    onDevModeCreateOnboardingAccount = { viewModel.devModeCreateOnboardingAccount() },
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
                        isNavigatingFromOnboarding = true
                        showLoadingOverlay = true
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
                    LaunchedEffect(Unit) {
                        if (showLoadingOverlay) {
                            kotlinx.coroutines.delay(500)
                            showLoadingOverlay = false
                        }
                    }

                    LaunchedEffect(isNavigatingFromOnboarding) {
                        if (isNavigatingFromOnboarding && user.userType != null) {
                            kotlinx.coroutines.delay(600)

                            when (user.userType) {
                                DomainUserType.VOLUNTEER -> {
                                    ToastManager.showSuccess("Welcome to Good Deed Feed! Start exploring volunteer opportunities nearby.")
                                }
                                DomainUserType.ORGANIZER -> {
                                    ToastManager.showSuccess("Welcome to Good Deed Feed! Ready to create your first volunteer event?")
                                }
                            }

                            isNavigatingFromOnboarding = false
                        }
                    }

                    TabNavigationScreen(
                        user = user,
                        onLogout = { viewModel.signOut() },
                        onEditProfile = {
                            when (user.userType) {
                                DomainUserType.VOLUNTEER -> {
                                    navController.navigate(Screen.EditVolunteerProfile.route)
                                }
                                DomainUserType.ORGANIZER -> {
                                    navController.navigate(Screen.EditOrganizerProfile.route)
                                }
                                null -> {
                                    navController.navigate(Screen.EditVolunteerProfile.route)
                                }
                            }
                        },
                    )
                }
            }

            composable(Screen.EditVolunteerProfile.route) {
                val user = (currentState as? AuthUiState.Success)?.user
                if (user != null) {
                    EditVolunteerProfileScreen(
                        user = user,
                        onSave = { userUpdate, profilePictureFile ->
                            viewModel.updateProfile(userUpdate, profilePictureFile)
                            navController.popBackStack()
                        },
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }
            }

            composable(Screen.EditOrganizerProfile.route) {
                val user = (currentState as? AuthUiState.Success)?.user
                if (user != null) {
                    EditOrganizerProfileScreen(
                        user = user,
                        onSave = { userUpdate, profilePictureFile ->
                            viewModel.updateProfile(userUpdate, profilePictureFile)
                            navController.popBackStack()
                        },
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }
            }
        }

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

        if (showLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                com.example.gooddeedfeed.presentation.ui.components.ShutterLoadingView(modifier = Modifier.size(48.dp))
            }
        }
    }
}
