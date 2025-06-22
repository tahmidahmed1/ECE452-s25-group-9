package com.example.gooddeedfeed.presentation.ui.screens.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.components.LoadingIndicator
import com.example.gooddeedfeed.presentation.ui.components.ToastUtils
import com.example.gooddeedfeed.presentation.ui.components.onboarding.OnboardingDotIndicators
import com.example.gooddeedfeed.presentation.viewmodel.onboarding.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel<OnboardingViewModel>(),
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(1) }
    var selectedUserType by remember { mutableStateOf<DomainUserType?>(null) }
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()

    // Calculate total steps based on user type
    val totalSteps = when (selectedUserType) {
        DomainUserType.VOLUNTEER -> 3 // Step 1: User type, Step 2: Basic info, Step 3: Detailed volunteer profile
        else -> 2 // Step 1: User type, Step 2: Basic info
    }

    // Show loading indicator with consistent theme
    if (uiState.isLoading) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            LoadingIndicator()
        }
        return
    }

    // Show error message if any
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            ToastUtils.showErrorToast(context, errorMessage)
            viewModel.clearError() // Clear the error after showing it
        }
    }

    // Handle onboarding completion
    LaunchedEffect(uiState.isOnboardingCompleted) {
        if (uiState.isOnboardingCompleted) {
            onOnboardingComplete()
        }
    }

    // Main content with consistent theme
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column {
            // Dot indicators at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                OnboardingDotIndicators(
                    totalSteps = totalSteps,
                    currentStep = currentStep,
                )
            }

            // Screen content
            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    1 -> {
                        OnboardingStepOneScreen(
                            onUserTypeSelected = { userType ->
                                selectedUserType = userType
                                viewModel.completeStepOne(userType)
                                currentStep = 2
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    2 -> {
                        selectedUserType?.let { userType ->
                            when (userType) {
                                DomainUserType.VOLUNTEER -> {
                                    OnboardingStepTwoBasicScreen(
                                        userType = userType,
                                        onComplete = { fullName, phone, profilePictureFile ->
                                            // Store basic info and move to detailed volunteer profile
                                            currentStep = 3
                                        },
                                        onBack = {
                                            currentStep = 1
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                else -> {
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
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                    3 -> {
                        // Detailed volunteer profile (only for volunteers)
                        OnboardingStepThreeVolunteerScreen(
                            onComplete = { volunteerProfile, profilePictureFile ->
                                viewModel.completeVolunteerOnboarding(
                                    volunteerProfile = volunteerProfile,
                                    profilePictureFile = profilePictureFile,
                                )
                            },
                            onBack = {
                                currentStep = 2
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
} 
