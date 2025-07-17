package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.DomainLeaderboardEntry
import com.example.gooddeedfeed.domain.repository.LeaderboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val entries: List<DomainLeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasNextPage: Boolean = true,
    val totalEntries: Int = 0
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val pageSize = 20

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                entries = emptyList(),
                currentPage = 1
            )

            leaderboardRepository.getLeaderboard(1, pageSize).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            entries = response.entries,
                            currentPage = response.page,
                            hasNextPage = response.hasNext,
                            totalEntries = response.totalEntries
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load leaderboard"
                        )
                    }
                )
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasNextPage) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoadingMore = true)

            val nextPage = currentState.currentPage + 1
            leaderboardRepository.getLeaderboard(nextPage, pageSize).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            entries = currentState.entries + response.entries,
                            currentPage = response.page,
                            hasNextPage = response.hasNext
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            errorMessage = error.message ?: "Failed to load more entries"
                        )
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 