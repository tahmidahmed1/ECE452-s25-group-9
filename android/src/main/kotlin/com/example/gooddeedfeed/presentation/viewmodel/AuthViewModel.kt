package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.domain.usecase.GetCurrentUserUseCase
import com.example.gooddeedfeed.domain.usecase.SignInUseCase
import com.example.gooddeedfeed.domain.usecase.SignOutUseCase
import com.example.gooddeedfeed.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()

    object Loading : AuthUiState()

    data class Success(val user: User) : AuthUiState()

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

    fun signUp(
        username: String,
        email: String,
        password: String,
    ) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val resp = signUp.invoke(username, email, password)
            if (resp.success) {
                fetchUser()
            } else {
                _uiState.value = AuthUiState.Error(resp.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(
        username: String,
        password: String,
    ) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val resp = signIn.invoke(username, password)
            if (resp.success) {
                fetchUser()
            } else {
                _uiState.value = AuthUiState.Error(resp.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOut.invoke()
            _uiState.value = AuthUiState.SignedOut
        }
    }

    fun fetchUser() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val user = getCurrentUser.invoke()
            if (user != null) {
                _uiState.value = AuthUiState.Success(user)
            } else {
                _uiState.value = AuthUiState.SignedOut
            }
        }
    }

    // Alias for fetchUser for clarity when refreshing after onboarding
    fun refreshUser() = fetchUser()
}
