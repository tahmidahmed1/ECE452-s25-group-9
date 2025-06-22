package com.example.gooddeedfeed.presentation.viewmodel.institution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.ActivityReview
import com.example.gooddeedfeed.domain.usecase.institution.ManageReviewsUseCase
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewData(
    val pendingReviews: List<ActivityReview>,
    val completedReviews: List<ActivityReview>,
    val selectedTab: ReviewTab,
    val selectedReview: ActivityReview? = null
)

enum class ReviewTab {
    PENDING, COMPLETED
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val manageReviewsUseCase: ManageReviewsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState<ReviewData>>(UiState.Loading)
    val uiState: StateFlow<UiState<ReviewData>> = _uiState.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(ReviewTab.PENDING)
    
    init {
        loadReviews()
    }
    
    fun loadReviews() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            combine(
                manageReviewsUseCase.getPendingReviews(),
                manageReviewsUseCase.getCompletedReviews()
            ) { pending, completed ->
                ReviewData(
                    pendingReviews = pending,
                    completedReviews = completed,
                    selectedTab = _selectedTab.value
                )
            }
                .catch { e ->
                    _uiState.value = UiState.Error("Failed to load reviews: ${e.message}")
                }
                .collect { data ->
                    _uiState.value = UiState.Success(data)
                }
        }
    }
    
    fun selectTab(tab: ReviewTab) {
        _selectedTab.value = tab
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(selectedTab = tab)
            )
        }
    }
    
    fun selectReview(review: ActivityReview?) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(selectedReview = review)
            )
        }
    }
    
    fun approveReview(reviewId: Int, feedback: String? = null) {
        viewModelScope.launch {
            manageReviewsUseCase.approveReview(reviewId, feedback)
                .onSuccess {
                    loadReviews() // Refresh the data
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error("Failed to approve review: ${e.message}")
                }
        }
    }
    
    fun rejectReview(reviewId: Int, reason: String? = null) {
        viewModelScope.launch {
            manageReviewsUseCase.rejectReview(reviewId, reason)
                .onSuccess {
                    loadReviews() // Refresh the data
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error("Failed to reject review: ${e.message}")
                }
        }
    }
    
    fun requestMoreInfo(reviewId: Int, message: String? = null) {
        viewModelScope.launch {
            manageReviewsUseCase.requestMoreInfo(reviewId, message)
                .onSuccess {
                    loadReviews() // Refresh the data
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error("Failed to request more info: ${e.message}")
                }
        }
    }
} 