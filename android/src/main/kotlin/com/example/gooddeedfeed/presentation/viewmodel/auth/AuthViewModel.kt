package com.example.gooddeedfeed.presentation.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.BuildConfig
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.repository.AuthRepository
import com.example.gooddeedfeed.domain.repository.NotificationRepository
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
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkCurrentUser()
    }

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
                    Log.d(TAG, "🔄 Setting user type to: $userType")

                    authRepository.setUserType(userType).onSuccess {
                        Log.d(TAG, "✅ User type set successfully")
                        Log.d(TAG, "🔄 Fetching user details...")
                        fetchUser()
                    }.onFailure { error ->
                        Log.e(TAG, "❌ Failed to set user type", error)
                        val detailedMessage = "Failed to set user type: ${error.message ?: "Unknown error"}"
                        _uiState.value = AuthUiState.Error(detailedMessage)
                    }
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

    fun devModeCreateOnboardingAccount() {
        Log.d(TAG, "🎯 DevMode onboarding account creation initiated")

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
                val devUsername = "test_user_$timestamp"
                val devEmail = "$devUsername@example.com"
                val devPassword = "test_password_123"

                Log.d(TAG, "🔄 Generated dev credentials: $devUsername")
                Log.d(TAG, "📞 Calling signUpUseCase for regular account...")

                val result = signUpUseCase.invoke(devUsername, devEmail, devPassword)

                result.onSuccess { response ->
                    Log.d(TAG, "✅ DevMode test account created successfully")
                    Log.d(TAG, "🔄 Fetching user details (should require onboarding)...")
                    fetchUser()
                }.onFailure { error ->
                    Log.e(TAG, "❌ DevMode test account creation failed", error)
                    val detailedMessage = "Dev mode test account creation failed: ${error.message ?: "Unknown error"}"
                    _uiState.value = AuthUiState.Error(detailedMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ DevMode test account creation exception", e)
                _uiState.value = AuthUiState.Error("Dev mode error: ${e.message ?: "Unknown exception"}")
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        Log.d(TAG, "📝 SignUp params - Username: $username, Email: $email")

        Log.d(TAG, "🔄 Setting loading state...")
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                Log.d(TAG, "🧹 Ensuring clean auth state before sign up...")

                Log.d(TAG, "📞 Calling signUpUseCase...")
                val result = signUpUseCase.invoke(username, email, password)

                result.onSuccess { domainUser ->
                    Log.d(TAG, "✅ SignUp successful")
                    Log.d(TAG, "✅ User data from signup - ID: ${domainUser.id}, Username: ${domainUser.username}")
                    Log.d(TAG, "✅ Setting user state directly from signup response...")
                    _uiState.value = AuthUiState.Success(domainUser)
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

                result.onSuccess { domainUser ->
                    Log.d(TAG, "✅ SignIn successful")
                    Log.d(TAG, "✅ User data from signin - ID: ${domainUser.id}, Username: ${domainUser.username}")
                    Log.d(TAG, "✅ Setting user state directly from signin response...")
                    _uiState.value = AuthUiState.Success(domainUser)
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
                Log.d(TAG, "🧹 Clearing UI state immediately...")
                _uiState.value = AuthUiState.SignedOut

                Log.d(TAG, "📞 Calling signOutUseCase...")
                val result = signOutUseCase.invoke()

                result.onSuccess {
                    Log.d(TAG, "✅ SignOut successful")
                    _uiState.value = AuthUiState.SignedOut
                }.onFailure { error ->
                    Log.e(TAG, "❌ SignOut failed", error)
                    _uiState.value = AuthUiState.SignedOut
                    Log.w(TAG, "⚠️ Keeping local state as signed out despite server error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ SignOut exception", e)
                _uiState.value = AuthUiState.SignedOut
                Log.w(TAG, "⚠️ Keeping local state as signed out despite exception")
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

    fun updateUserState(user: DomainUser) {
        _uiState.value = AuthUiState.Success(user)
    }


    fun updateUserProfile(update: DomainUserUpdate) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.updateProfile(update)
            result.onSuccess { user: DomainUser ->
                _uiState.value = AuthUiState.Success(user)

                fetchUser()
            }.onFailure { error: Throwable ->
                _uiState.value = AuthUiState.Error(error.message ?: "Failed to update profile")
            }
        }
    }


    fun uploadProfilePicture(file: File) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.uploadProfilePicture(file)
                result.onSuccess { url: String ->
                    fetchUser()
                }.onFailure { err: Throwable ->
                    _uiState.value = AuthUiState.Error(err.message ?: "Failed to upload picture")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to upload picture")
            }
        }
    }

    fun removeProfilePicture() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.removeProfilePicture()
                result.onSuccess {
                    fetchUser()
                }.onFailure { err: Throwable ->
                    _uiState.value = AuthUiState.Error(err.message ?: "Failed to remove picture")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to remove picture")
            }
        }
    }

    fun updateProfile(userUpdate: DomainUserUpdate, profilePictureFile: File? = null) {
        Log.d(TAG, "📝 updateProfile called with: $userUpdate, hasFile=${profilePictureFile != null}")
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                profilePictureFile?.let { file ->
                    Log.d(TAG, "📤 Uploading new profile picture: ${file.absolutePath}")
                    val uploadResult = authRepository.uploadProfilePicture(file)
                    uploadResult.onSuccess {
                        Log.d(TAG, "✅ Profile picture uploaded successfully")
                    }.onFailure { error ->
                        Log.e(TAG, "❌ Failed to upload profile picture", error)
                        com.example.gooddeedfeed.presentation.ui.components.ToastManager.showError(error.message ?: "Failed to upload profile picture")
                        _uiState.value = AuthUiState.Error(error.message ?: "Failed to upload profile picture")
                        return@launch
                    }
                }

                Log.d(TAG, "📤 Sending profile update payload to repository")
                val updateResult = authRepository.updateProfile(userUpdate)
                updateResult.onSuccess { user: DomainUser ->
                    Log.d(TAG, "✅ Profile updated on server. New karma: ${user.karmaPoints}")
                    com.example.gooddeedfeed.presentation.ui.components.ToastManager.showSuccess("Profile updated successfully")

                    _uiState.value = AuthUiState.Success(user)

                    fetchUser()
                }.onFailure { error: Throwable ->
                    Log.e(TAG, "❌ Profile update failed", error)
                    com.example.gooddeedfeed.presentation.ui.components.ToastManager.showError(error.message ?: "Failed to update profile")

                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to update profile")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during updateProfile", e)
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to update profile")
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

    fun initializeFcmToken() {
        viewModelScope.launch {
            try {
                notificationRepository.getFcmToken().onSuccess { token ->
                    token?.let {
                        Log.d(TAG, "FCM token initialized: ${it.take(20)}...")
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to initialize FCM token", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing FCM token", e)
            }
        }
    }

    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            notificationRepository.updateFcmToken(token).onFailure { e ->
                Log.e(TAG, "Failed to update FCM token", e)
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.setNotificationsEnabled(enabled).onFailure { e ->
                Log.e(TAG, "Failed to update notification preferences", e)
            }
        }
    }


    fun subscribeToOrganizer(organizerId: Int) {
        viewModelScope.launch {
            notificationRepository.subscribeToOrganizer(organizerId).onSuccess {
                Log.d(TAG, "Successfully subscribed to organizer: $organizerId")
            }.onFailure { e ->
                Log.e(TAG, "Failed to subscribe to organizer: $organizerId", e)
            }
        }
    }

    fun unsubscribeFromOrganizer(organizerId: Int) {
        viewModelScope.launch {
            notificationRepository.unsubscribeFromOrganizer(organizerId).onSuccess {
                Log.d(TAG, "Successfully unsubscribed from organizer: $organizerId")
            }.onFailure { e ->
                Log.e(TAG, "Failed to unsubscribe from organizer: $organizerId", e)
            }
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
