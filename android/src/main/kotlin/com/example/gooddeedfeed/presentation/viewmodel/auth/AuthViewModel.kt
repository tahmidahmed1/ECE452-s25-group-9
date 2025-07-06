package com.example.gooddeedfeed.presentation.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.BuildConfig
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.repository.AuthRepository
import com.example.gooddeedfeed.domain.usecase.GetCurrentUserUseCase
import com.example.gooddeedfeed.domain.usecase.SignInUseCase
import com.example.gooddeedfeed.domain.usecase.SignOutUseCase
import com.example.gooddeedfeed.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: DomainUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object SignedOut : AuthUiState()
}

@HiltViewModel
class AuthViewModel
@Inject
constructor(
    private val signUp: SignUpUseCase,
    private val signIn: SignInUseCase,
    private val signOut: SignOutUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkCurrentUser()
    }

    /**
     * Development mode: Quick sign in with auto-generated user
     */
    fun devModeSignIn(userType: DomainUserType = DomainUserType.VOLUNTEER) {
        if (!BuildConfig.DEV_MODE) {
            _uiState.value = AuthUiState.Error("Dev mode is not available in release builds")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis().toString().takeLast(6)
                val devUsername = "dev_${userType.name.lowercase()}_$timestamp"
                val responseFlow = this@AuthViewModel.signUp.invoke(devUsername, "$devUsername@example.com", "dev_password_123")
                val result = responseFlow.first()
                result.onSuccess { response ->
                    fetchUser()
                }.onFailure { error ->
                    val detailedMessage = "Dev mode sign-in failed: ${error.message ?: "Unknown error"}"
                    _uiState.value = AuthUiState.Error(detailedMessage)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Dev mode error: ${e.message ?: "Unknown exception"}")
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val responseFlow = this@AuthViewModel.signUp.invoke(username, email, password)
                val result = responseFlow.first()
                result.onSuccess { response ->
                    fetchUser()
                }.onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign up failed")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun signIn(username: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val responseFlow = this@AuthViewModel.signIn.invoke(username, password)
                val result = responseFlow.first()
                result.onSuccess { response ->
                    fetchUser()
                }.onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign in failed")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                val responseFlow = this@AuthViewModel.signOut.invoke()
                val result = responseFlow.first()
                result.onSuccess {
                    _uiState.value = AuthUiState.SignedOut
                }.onFailure { error ->
                    _uiState.value = AuthUiState.Error("Sign out failed: ${error.message}")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Sign out failed: ${e.message}")
            }
        }
    }

    private fun fetchUser() {
        viewModelScope.launch {
            try {
                val userFlow = this@AuthViewModel.getCurrentUser.invoke()
                val result = userFlow.first()
                result.onSuccess { user ->
                    _uiState.value = AuthUiState.Success(user)
                }.onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to fetch user")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to fetch user")
            }
        }
    }

    fun refreshUser() = fetchUser()

    // ------------------ Profile Update ------------------

    fun updateUserProfile(update: DomainUserUpdate) {
        // keep current UI state but show loading maybe
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.updateUserProfile(update)
            result.onSuccess {
                // Fetch updated user
                fetchUser()
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(error.message ?: "Failed to update profile")
            }
        }
    }

    // ------------------ Profile Picture ------------------

    fun uploadProfilePicture(file: File) {
        Log.d(TAG, "AuthViewModel.uploadProfilePicture called with file: ${file.absolutePath}")
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                authRepository.uploadProfilePicture(file).first().onSuccess {
                    Log.d(TAG, "Profile picture upload succeeded; refreshing user")
                    // Refresh user after successful upload
                    fetchUser()
                }.onFailure { err ->
                    Log.e(TAG, "Profile picture upload failed: ${err.message}")
                    _uiState.value = AuthUiState.Error(err.message ?: "Failed to upload picture")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception uploading profile picture: ${e.message}", e)
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to upload picture")
            }
        }
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                val userFlow = this@AuthViewModel.getCurrentUser.invoke()
                val result = userFlow.first()
                result.onSuccess { user ->
                    _uiState.value = AuthUiState.Success(user)
                }.onFailure { error ->
                    _uiState.value = AuthUiState.SignedOut
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.SignedOut
            }
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
