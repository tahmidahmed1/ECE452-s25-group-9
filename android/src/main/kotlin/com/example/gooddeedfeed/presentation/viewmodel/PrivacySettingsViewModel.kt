package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.repository.AuthRepositoryImpl
import com.example.gooddeedfeed.data.repository.LocationSettingsRepository
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacySettingsState(
    val locationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val shareProfilePictureEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val locationSettingsRepository: LocationSettingsRepository,
    private val authRepository: AuthRepositoryImpl,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacySettingsState())
    val uiState: StateFlow<PrivacySettingsState> = _uiState.asStateFlow()

    init {
        // Load initial settings
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                combine(
                    locationSettingsRepository.isLocationEnabled,
                    locationSettingsRepository.isNotificationsEnabled,
                    locationSettingsRepository.isShareProfilePictureEnabled,
                ) { locationEnabled, notificationsEnabled, shareProfilePictureEnabled ->
                    PrivacySettingsState(
                        locationEnabled = locationEnabled,
                        notificationsEnabled = notificationsEnabled,
                        shareProfilePictureEnabled = shareProfilePictureEnabled,
                        isLoading = false,
                        errorMessage = null,
                        successMessage = null,
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load settings: ${e.message}",
                    successMessage = null,
                )
            }
        }
    }

    fun updateLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

                locationSettingsRepository.setLocationEnabled(enabled)

                _uiState.value = _uiState.value.copy(
                    locationEnabled = enabled,
                    isLoading = false,
                    errorMessage = null,
                    successMessage = if (enabled) {
                        "Location services enabled! You may need to restart the app for changes to take effect."
                    } else {
                        "Location services disabled. Nearby events may not be shown accurately."
                    },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update location setting: ${e.message}",
                    successMessage = null,
                )
            }
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                locationSettingsRepository.setNotificationsEnabled(enabled)
                _uiState.value = _uiState.value.copy(
                    notificationsEnabled = enabled,
                    successMessage = if (enabled) "Notifications enabled" else "Notifications disabled",
                    errorMessage = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update notifications setting: ${e.message}",
                    successMessage = null,
                )
            }
        }
    }

    fun updateShareProfilePictureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Update local settings first
                locationSettingsRepository.setShareProfilePictureEnabled(enabled)

                // Update backend
                val userUpdate = DomainUserUpdate(shareProfilePicture = enabled)
                authRepository.updateProfile(userUpdate)

                _uiState.value = _uiState.value.copy(
                    shareProfilePictureEnabled = enabled,
                    successMessage = if (enabled) "Profile picture sharing enabled" else "Profile picture sharing disabled",
                    errorMessage = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update profile picture setting: ${e.message}",
                    successMessage = null,
                )
            }
        }
    }

    fun saveAllSettings(
        locationEnabled: Boolean,
        notificationsEnabled: Boolean,
        shareProfilePictureEnabled: Boolean,
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                locationSettingsRepository.updateAllSettings(
                    locationEnabled,
                    notificationsEnabled,
                    shareProfilePictureEnabled,
                )

                _uiState.value = _uiState.value.copy(
                    locationEnabled = locationEnabled,
                    notificationsEnabled = notificationsEnabled,
                    shareProfilePictureEnabled = shareProfilePictureEnabled,
                    isLoading = false,
                    errorMessage = null,
                    successMessage = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save settings: ${e.message}",
                    successMessage = null,
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
} 
