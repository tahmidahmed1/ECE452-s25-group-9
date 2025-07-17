package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.repository.LocationSettingsRepository
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
    val errorMessage: String? = null
)

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val locationSettingsRepository: LocationSettingsRepository
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
                    locationSettingsRepository.isShareProfilePictureEnabled
                ) { locationEnabled, notificationsEnabled, shareProfilePictureEnabled ->
                    PrivacySettingsState(
                        locationEnabled = locationEnabled,
                        notificationsEnabled = notificationsEnabled,
                        shareProfilePictureEnabled = shareProfilePictureEnabled,
                        isLoading = false,
                        errorMessage = null
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load settings: ${e.message}"
                )
            }
        }
    }

    fun updateLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                locationSettingsRepository.setLocationEnabled(enabled)
                
                _uiState.value = _uiState.value.copy(
                    locationEnabled = enabled,
                    isLoading = false,
                    errorMessage = if (enabled) {
                        "Location services enabled! You may need to restart the app for changes to take effect."
                    } else {
                        "Location services disabled. Nearby events may not be shown accurately."
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update location setting: ${e.message}"
                )
            }
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                locationSettingsRepository.setNotificationsEnabled(enabled)
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update notifications setting: ${e.message}"
                )
            }
        }
    }

    fun updateShareProfilePictureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                locationSettingsRepository.setShareProfilePictureEnabled(enabled)
                _uiState.value = _uiState.value.copy(shareProfilePictureEnabled = enabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update profile picture setting: ${e.message}"
                )
            }
        }
    }

    fun saveAllSettings(
        locationEnabled: Boolean,
        notificationsEnabled: Boolean,
        shareProfilePictureEnabled: Boolean
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                locationSettingsRepository.updateAllSettings(
                    locationEnabled,
                    notificationsEnabled,
                    shareProfilePictureEnabled
                )
                
                _uiState.value = _uiState.value.copy(
                    locationEnabled = locationEnabled,
                    notificationsEnabled = notificationsEnabled,
                    shareProfilePictureEnabled = shareProfilePictureEnabled,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save settings: ${e.message}"
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 