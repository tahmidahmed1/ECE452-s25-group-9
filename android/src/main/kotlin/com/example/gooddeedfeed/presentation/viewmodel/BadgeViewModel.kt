package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.DomainBadge
import com.example.gooddeedfeed.domain.model.DomainBadgeCheckResponse
import com.example.gooddeedfeed.domain.model.DomainUserBadge
import com.example.gooddeedfeed.domain.usecase.CheckBadgeAchievementsUseCase
import com.example.gooddeedfeed.domain.usecase.GetBadgesUseCase
import com.example.gooddeedfeed.domain.usecase.GetUserBadgesUseCase
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BadgeViewModel @Inject constructor(
    private val getBadgesUseCase: GetBadgesUseCase,
    private val getUserBadgesUseCase: GetUserBadgesUseCase,
    private val checkBadgeAchievementsUseCase: CheckBadgeAchievementsUseCase,
) : ViewModel() {

    private val _allBadgesState = MutableStateFlow<UiState<List<DomainBadge>>>(UiState.Idle)
    val allBadgesState: StateFlow<UiState<List<DomainBadge>>> = _allBadgesState.asStateFlow()

    private val _userBadgesState = MutableStateFlow<UiState<List<DomainUserBadge>>>(UiState.Idle)
    val userBadgesState: StateFlow<UiState<List<DomainUserBadge>>> = _userBadgesState.asStateFlow()

    private val _badgeCheckState = MutableStateFlow<UiState<DomainBadgeCheckResponse>>(UiState.Idle)
    val badgeCheckState: StateFlow<UiState<DomainBadgeCheckResponse>> = _badgeCheckState.asStateFlow()

    init {
        loadAllBadges()
        loadUserBadges()
    }

    fun loadAllBadges() {
        viewModelScope.launch {
            _allBadgesState.value = UiState.Loading

            getBadgesUseCase().collect { result ->
                _allBadgesState.value = result.fold(
                    onSuccess = { badges -> UiState.Success(badges) },
                    onFailure = { exception -> UiState.Error(exception.message ?: "Unknown error") },
                )
            }
        }
    }

    fun loadUserBadges() {
        viewModelScope.launch {
            _userBadgesState.value = UiState.Loading

            getUserBadgesUseCase().collect { result ->
                _userBadgesState.value = result.fold(
                    onSuccess = { badges -> UiState.Success(badges) },
                    onFailure = { exception -> UiState.Error(exception.message ?: "Unknown error") },
                )
            }
        }
    }

    fun checkBadgeAchievements() {
        viewModelScope.launch {
            _badgeCheckState.value = UiState.Loading

            checkBadgeAchievementsUseCase().collect { result ->
                _badgeCheckState.value = result.fold(
                    onSuccess = { response -> UiState.Success(response) },
                    onFailure = { exception -> UiState.Error(exception.message ?: "Unknown error") },
                )
            }
        }
    }

    fun clearBadgeCheckState() {
        _badgeCheckState.value = UiState.Idle
    }
} 
