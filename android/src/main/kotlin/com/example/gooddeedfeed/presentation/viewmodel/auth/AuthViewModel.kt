package com.example.gooddeedfeed.presentation.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.BuildConfig
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.usecase.GetCurrentUserUseCase
import com.example.gooddeedfeed.domain.usecase.SignInUseCase
import com.example.gooddeedfeed.domain.usecase.SignOutUseCase
import com.example.gooddeedfeed.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
                // Create a unique dev username for each user type
                val timestamp = System.currentTimeMillis().toString().takeLast(6)
                val devUsername = "dev_${userType.name.lowercase()}_$timestamp"
                val responseFlow = this@AuthViewModel.signUp.invoke(devUsername, "${devUsername}@example.com", "dev_password_123")
                val result = responseFlow.first()
                result.onSuccess { response ->
                    Log.d("AuthViewModel", "Dev sign up successful, token received: ${response.token != null}")
                    Log.d("AuthViewModel", "Response: success=${response.success}, message=${response.message}")
                    // For dev users, onboarding is completed automatically on the server
                    // Add a small delay to ensure token is saved before fetching user
                    delay(100)
                    fetchUser()
                }.onFailure { error ->
                    Log.e("AuthViewModel", "Dev sign up failed: ${error.message}")
                    Log.e("AuthViewModel", "Error type: ${error.javaClass.simpleName}")
                    val detailedMessage = "Dev mode sign-in failed: ${error.message ?: "Unknown error"}"
                    _uiState.value = AuthUiState.Error(detailedMessage)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Dev mode exception: ${e.message}")
                Log.e("AuthViewModel", "Exception type: ${e.javaClass.simpleName}")
                e.printStackTrace()
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

    fun fetchUser() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Fetching user...")
                val userFlow = this@AuthViewModel.getCurrentUser.invoke()
                val result = userFlow.first()
                result.onSuccess { user ->
                    Log.d("AuthViewModel", "User fetched successfully: ${user.username}, onboarding: ${user.onboardingCompleted}")
                    _uiState.value = AuthUiState.Success(user)
                }.onFailure { error ->
                    Log.e("AuthViewModel", "Failed to fetch user: ${error.message}")
                    _uiState.value = AuthUiState.SignedOut
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception fetching user: ${e.message}")
                _uiState.value = AuthUiState.Error("Failed to fetch user: ${e.message}")
            }
        }
    }

    fun refreshUser() = fetchUser()

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
}
