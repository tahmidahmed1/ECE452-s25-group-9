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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.components.ToastUtils
import com.example.gooddeedfeed.presentation.ui.components.base.LoadingIndicator
import com.example.gooddeedfeed.presentation.ui.components.onboarding.OnboardingDotIndicators
import com.example.gooddeedfeed.presentation.viewmodel.onboarding.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedUserType by remember { mutableStateOf<DomainUserType?>(null) }
    var basicFullName by remember { mutableStateOf("") }
    var basicPhone by remember { mutableStateOf("") }
    var basicProfilePicture: java.io.File? by remember { mutableStateOf(null) }
    val context = LocalContext.current

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isSuccess by viewModel.isSuccess.collectAsStateWithLifecycle()

    val totalSteps = when (selectedUserType) {
        DomainUserType.VOLUNTEER -> 3 // Step 1: User type, Step 2: Basic info, Step 3: Detailed volunteer profile
        else -> 2 // Step 1: User type, Step 2: Basic info
    }

    if (isLoading) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            LoadingIndicator()
        }
        return
    }

    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            ToastUtils.showErrorToast(context, errorMessage)
            viewModel.clearError() // Clear the error after showing it
        }
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            onOnboardingComplete()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column {
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
                                            basicFullName = fullName
                                            basicPhone = phone
                                            basicProfilePicture = profilePictureFile
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
                                        onComplete = { fullName, phone, organizationName, profilePictureFile, organizerProfile ->
                                            viewModel.completeOnboarding(
                                                userType = userType,
                                                fullName = fullName,
                                                phone = phone,
                                                organizationName = organizationName,
                                                profilePictureFile = profilePictureFile,
                                                organizerProfile = organizerProfile,
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
                        OnboardingStepThreeVolunteerScreen(
                            fullName = basicFullName,
                            phone = basicPhone,
                            onComplete = { volunteerProfile, profilePictureFile ->
                                val picture = profilePictureFile ?: basicProfilePicture
                                viewModel.completeVolunteerOnboarding(
                                    volunteerProfile = volunteerProfile,
                                    profilePictureFile = picture,
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
