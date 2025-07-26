package com.example.gooddeedfeed.presentation.viewmodel.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.DomainOrganizerProfile
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import com.example.gooddeedfeed.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    fun completeOnboarding(
        userType: DomainUserType,
        fullName: String,
        phone: String,
        organizationName: String? = null,
        profilePictureFile: File? = null,
        organizerProfile: DomainOrganizerProfile? = null,
        volunteerProfile: DomainVolunteerProfile? = null,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = when (userType) {
                    DomainUserType.VOLUNTEER -> {
                        if (volunteerProfile != null) {
                            authRepository.completeVolunteerOnboarding(
                                profile = volunteerProfile,
                                profilePictureFile = profilePictureFile,
                            )
                        } else {
                            Result.failure(Exception("Volunteer profile is required"))
                        }
                    }
                    DomainUserType.ORGANIZER -> {
                        if (organizerProfile != null) {
                            authRepository.completeOrganizerOnboarding(
                                profile = organizerProfile,
                                profilePictureFile = profilePictureFile,
                            )
                        } else {
                            Result.failure(Exception("Organizer profile is required"))
                        }
                    }
                }

                if (result.isSuccess) {
                    _isSuccess.value = true
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Unknown error occurred"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun completeStepOne(userType: DomainUserType) {
        viewModelScope.launch {
            try {
                authRepository.setUserType(userType)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to set user type"
            }
        }
    }

    fun completeVolunteerOnboarding(
        volunteerProfile: DomainVolunteerProfile,
        profilePictureFile: File? = null,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = authRepository.completeVolunteerOnboarding(
                    profile = volunteerProfile,
                    profilePictureFile = profilePictureFile,
                )

                if (result.isSuccess) {
                    _isSuccess.value = true
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to complete volunteer onboarding"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to complete volunteer onboarding"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 
