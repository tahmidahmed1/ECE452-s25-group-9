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
    private val signUpUseCase: SignUpUseCase,
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase,
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
        Log.d(TAG, "🎯 DevMode signIn initiated")
        Log.d(TAG, "📝 DevMode userType: $userType")

        if (!BuildConfig.DEV_MODE) {
            Log.w(TAG, "⚠️ DevMode not available in release builds")
            _uiState.value = AuthUiState.Error("Dev mode is not available in release builds")
            return
        }

        Log.d(TAG, "🔄 Setting loading state...")
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis().toString().takeLast(6)
                val devUsername = "dev_${userType.name.lowercase()}_$timestamp"

                Log.d(TAG, "🔄 Generated dev username: $devUsername")
                Log.d(TAG, "📞 Calling signUpUseCase...")

                val result = signUpUseCase.invoke(devUsername, "$devUsername@example.com", "dev_password_123")

                result.onSuccess { response ->
                    Log.d(TAG, "✅ DevMode signUp successful")
                    Log.d(TAG, "🔄 Fetching user details...")
                    fetchUser()
                }.onFailure { error ->
                    Log.e(TAG, "❌ DevMode signUp failed", error)
                    val detailedMessage = "Dev mode sign-in failed: ${error.message ?: "Unknown error"}"
                    _uiState.value = AuthUiState.Error(detailedMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ DevMode exception", e)
                _uiState.value = AuthUiState.Error("Dev mode error: ${e.message ?: "Unknown exception"}")
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        Log.d(TAG, "🎯 SignUp initiated")
        Log.d(TAG, "📝 SignUp params - Username: $username, Email: $email")

        Log.d(TAG, "🔄 Setting loading state...")
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                Log.d(TAG, "📞 Calling signUpUseCase...")
                val result = signUpUseCase.invoke(username, email, password)

                result.onSuccess { response ->
                    Log.d(TAG, "✅ SignUp successful")
                    Log.d(TAG, "🔄 Fetching user details...")
                    fetchUser()
                }.onFailure { error ->
                    Log.e(TAG, "❌ SignUp failed", error)
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign up failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ SignUp exception", e)
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun signIn(username: String, password: String) {
        Log.d(TAG, "🎯 SignIn initiated")
        Log.d(TAG, "📝 SignIn params - Username: $username")

        Log.d(TAG, "🔄 Setting loading state...")
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                Log.d(TAG, "📞 Calling signInUseCase...")
                val result = signInUseCase.invoke(username, password)

                result.onSuccess { response ->
                    Log.d(TAG, "✅ SignIn successful")
                    Log.d(TAG, "🔄 Fetching user details...")
                    fetchUser()
                }.onFailure { error ->
                    Log.e(TAG, "❌ SignIn failed", error)
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign in failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ SignIn exception", e)
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun signOut() {
        Log.d(TAG, "🎯 SignOut initiated")

        viewModelScope.launch {
            try {
                Log.d(TAG, "📞 Calling signOutUseCase...")
                val result = signOutUseCase.invoke()

                result.onSuccess {
                    Log.d(TAG, "✅ SignOut successful")
                    _uiState.value = AuthUiState.SignedOut
                }.onFailure { error ->
                    Log.e(TAG, "❌ SignOut failed", error)
                    _uiState.value = AuthUiState.Error("Sign out failed: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ SignOut exception", e)
                _uiState.value = AuthUiState.Error("Sign out failed: ${e.message}")
            }
        }
    }

    private fun fetchUser() {
        Log.d(TAG, "🔄 FetchUser initiated")

        viewModelScope.launch {
            try {
                Log.d(TAG, "📞 Calling getCurrentUser...")
                val result = getCurrentUser.invoke()

                result.onSuccess { user: DomainUser ->
                    Log.d(TAG, "✅ FetchUser successful")
                    Log.d(TAG, "✅ User details - ID: ${user.id}, Username: ${user.username}")
                    Log.d(TAG, "✅ User onboarding completed: ${user.onboardingCompleted}")
                    _uiState.value = AuthUiState.Success(user)
                }.onFailure { error: Throwable ->
                    Log.e(TAG, "❌ FetchUser failed", error)
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to fetch user")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ FetchUser exception", e)
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
            val result = authRepository.updateProfile(update)
            result.onSuccess { user: DomainUser ->
                // Update with the returned user
                _uiState.value = AuthUiState.Success(user)
            }.onFailure { error: Throwable ->
                _uiState.value = AuthUiState.Error(error.message ?: "Failed to update profile")
            }
        }
    }

    // ------------------ Profile Picture ------------------

    fun uploadProfilePicture(file: File) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.uploadProfilePicture(file)
                result.onSuccess { url: String ->
                    // Refresh user after successful upload
                    fetchUser()
                }.onFailure { err: Throwable ->
                    _uiState.value = AuthUiState.Error(err.message ?: "Failed to upload picture")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to upload picture")
            }
        }
    }

    private fun checkCurrentUser() {
        Log.d(TAG, "🔄 CheckCurrentUser initiated")

        viewModelScope.launch {
            try {
                Log.d(TAG, "📞 Calling getCurrentUser for auth check...")
                val result = getCurrentUser.invoke()

                result.onSuccess { user: DomainUser ->
                    Log.d(TAG, "✅ CheckCurrentUser successful - user is authenticated")
                    Log.d(TAG, "✅ User details - ID: ${user.id}, Username: ${user.username}")
                    _uiState.value = AuthUiState.Success(user)
                }.onFailure { error: Throwable ->
                    Log.d(TAG, "ℹ️ CheckCurrentUser failed - user not authenticated")
                    Log.d(TAG, "ℹ️ Error: ${error.message}")
                    _uiState.value = AuthUiState.SignedOut
                }
            } catch (e: Exception) {
                Log.d(TAG, "ℹ️ CheckCurrentUser exception - user not authenticated")
                Log.d(TAG, "ℹ️ Exception: ${e.message}")
                _uiState.value = AuthUiState.SignedOut
            }
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
