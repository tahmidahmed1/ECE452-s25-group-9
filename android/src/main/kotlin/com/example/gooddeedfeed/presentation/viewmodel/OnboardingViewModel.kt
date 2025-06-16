package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.remote.InstitutionName
import com.example.gooddeedfeed.data.remote.UserType
import com.example.gooddeedfeed.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val errorMessage: String? = null,
    val stepOneCompleted: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun completeStepOne(userType: UserType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val token = (authRepository as com.example.gooddeedfeed.data.repository.AuthRepositoryImpl).getTokenString()
                if (token != null) {
                    val success = authRepository.completeOnboardingStepOne(token, userType)
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            stepOneCompleted = true,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to save user type. Please try again.",
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Authentication token not found. Please log in again.",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Unable to save user type. Please try again.",
                )
            }
        }
    }

    fun completeOnboarding(
        userType: UserType,
        fullName: String,
        phone: String,
        organizationName: String?,
        institutionName: InstitutionName?,
        profilePictureFile: File?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val token = (authRepository as com.example.gooddeedfeed.data.repository.AuthRepositoryImpl).getTokenString()
                if (token != null) {
                    // First complete the onboarding
                    val onboardingSuccess = authRepository.completeOnboarding(
                        token = token,
                        userType = userType,
                        fullName = fullName,
                        phone = phone,
                        organizationName = organizationName,
                        institutionName = institutionName,
                    )

                    if (onboardingSuccess) {
                        // If profile picture is provided, upload it
                        if (profilePictureFile != null) {
                            val uploadResponse = authRepository.uploadProfilePicture(token, profilePictureFile)
                            if (uploadResponse == null) {
                                // Profile picture upload failed, but onboarding succeeded
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isOnboardingCompleted = true,
                                    errorMessage = "Onboarding completed, but profile picture upload failed. You can upload it later from your profile.",
                                )
                                return@launch
                            }
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isOnboardingCompleted = true,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to complete onboarding. Please try again.",
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Authentication token not found. Please log in again.",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Unable to complete setup. Please try again.",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 
