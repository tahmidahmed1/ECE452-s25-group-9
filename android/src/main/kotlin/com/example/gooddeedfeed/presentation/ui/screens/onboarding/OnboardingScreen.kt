package com.example.gooddeedfeed.presentation.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gooddeedfeed.data.remote.UserType
import com.example.gooddeedfeed.presentation.viewmodel.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(1) }
    var selectedUserType by remember { mutableStateOf<UserType?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    // Show loading indicator
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Show error message if any
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // You can show a snackbar or dialog here
        }
    }

    // Handle onboarding completion
    LaunchedEffect(uiState.isOnboardingCompleted) {
        if (uiState.isOnboardingCompleted) {
            onOnboardingComplete()
        }
    }

    when (currentStep) {
        1 -> {
            OnboardingStepOneScreen(
                onUserTypeSelected = { userType ->
                    selectedUserType = userType
                    viewModel.completeStepOne(userType)
                    currentStep = 2
                },
                modifier = modifier,
            )
        }
        2 -> {
            selectedUserType?.let { userType ->
                OnboardingStepTwoScreen(
                    userType = userType,
                    onComplete = { fullName, phone, organizationName, institutionName, profilePictureFile ->
                        viewModel.completeOnboarding(
                            userType = userType,
                            fullName = fullName,
                            phone = phone,
                            organizationName = organizationName,
                            institutionName = institutionName,
                            profilePictureFile = profilePictureFile,
                        )
                    },
                    onBack = {
                        currentStep = 1
                    },
                    modifier = modifier,
                )
            }
        }
    }
} 
