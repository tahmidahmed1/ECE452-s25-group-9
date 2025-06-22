package com.example.gooddeedfeed.presentation.viewmodel.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.DomainInstitutionName
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import com.example.gooddeedfeed.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    fun completeStepOne(userType: DomainUserType) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                // Call the repository to update user type
                val result = authRepository.updateUserType(userType)
                result.onFailure { error ->
                    throw error
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stepOneCompleted = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to update user type",
                )
            }
        }
    }

    fun completeOnboarding(
        userType: DomainUserType,
        fullName: String,
        phone: String,
        organizationName: String? = null,
        institutionName: DomainInstitutionName? = null,
        profilePictureFile: File? = null,
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                // Upload profile picture if provided
                var profilePictureUrl: String? = null
                profilePictureFile?.let { file ->
                    val uploadResult = authRepository.uploadProfilePicture(file).first()
                    uploadResult.onSuccess { response ->
                        profilePictureUrl = response.profilePictureUrl
                    }.onFailure { error ->
                        // Log the error but don't fail the entire onboarding
                        // profilePictureUrl remains null
                    }
                }

                // Complete onboarding with basic profile
                val result = authRepository.completeOnboarding(
                    userType = userType,
                    fullName = fullName,
                    phone = phone,
                    organizationName = organizationName,
                    institutionName = institutionName,
                    profilePictureUrl = profilePictureUrl,
                )
                result.onFailure { error ->
                    throw error
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOnboardingCompleted = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to complete onboarding",
                )
            }
        }
    }

    fun completeVolunteerOnboarding(
        volunteerProfile: DomainVolunteerProfile,
        profilePictureFile: File? = null,
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                // Upload profile picture if provided
                var profilePictureUrl: String? = null
                profilePictureFile?.let { file ->
                    val uploadResult = authRepository.uploadProfilePicture(file).first()
                    uploadResult.onSuccess { response ->
                        profilePictureUrl = response.profilePictureUrl
                    }.onFailure { error ->
                        // Log the error but don't fail the entire onboarding
                        // profilePictureUrl remains null
                    }
                }

                // Complete volunteer onboarding with comprehensive profile
                val result = authRepository.completeVolunteerOnboarding(
                    volunteerProfile = volunteerProfile,
                    profilePictureUrl = profilePictureUrl,
                )
                result.onFailure { error ->
                    throw error
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOnboardingCompleted = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to complete volunteer profile",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetOnboarding() {
        _uiState.value = OnboardingUiState()
    }
} 
