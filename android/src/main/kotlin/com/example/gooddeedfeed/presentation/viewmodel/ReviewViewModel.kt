package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.ActivityReview
import com.example.gooddeedfeed.domain.model.ReviewStatus
import com.example.gooddeedfeed.domain.model.ReviewActionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReviewUiState {
    object Loading : ReviewUiState()
    data class Success(
        val pendingReviews: List<ActivityReview>,
        val completedReviews: List<ActivityReview>,
        val selectedTab: ReviewTab,
        val selectedReview: ActivityReview? = null
    ) : ReviewUiState()
    data class Error(val message: String) : ReviewUiState()
}

enum class ReviewTab {
    PENDING, COMPLETED
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    // TODO: Inject ReviewRepository when created
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()
    
    init {
        loadReviews()
    }
    
    fun loadReviews() {
        viewModelScope.launch {
            try {
                _uiState.value = ReviewUiState.Loading
                
                // TODO: Replace with actual repository call
                val mockReviews = getMockReviews()
                val pendingReviews = mockReviews.filter { it.status == ReviewStatus.PENDING }
                val completedReviews = mockReviews.filter { it.status != ReviewStatus.PENDING }
                
                _uiState.value = ReviewUiState.Success(
                    pendingReviews = pendingReviews,
                    completedReviews = completedReviews,
                    selectedTab = ReviewTab.PENDING
                )
            } catch (e: Exception) {
                _uiState.value = ReviewUiState.Error("Failed to load reviews")
            }
        }
    }
    
    fun selectTab(tab: ReviewTab) {
        val currentState = _uiState.value
        if (currentState is ReviewUiState.Success) {
            _uiState.value = currentState.copy(
                selectedTab = tab,
                selectedReview = null // Clear selection when switching tabs
            )
        }
    }
    
    fun selectReview(review: ActivityReview?) {
        val currentState = _uiState.value
        if (currentState is ReviewUiState.Success) {
            _uiState.value = currentState.copy(selectedReview = review)
        }
    }
    
    fun approveReview(reviewId: Int, notes: String?) {
        performReviewAction(reviewId, ReviewActionType.APPROVE, notes)
    }
    
    fun rejectReview(reviewId: Int, notes: String?) {
        performReviewAction(reviewId, ReviewActionType.REJECT, notes)
    }
    
    fun requestMoreInfo(reviewId: Int, notes: String?) {
        performReviewAction(reviewId, ReviewActionType.REQUEST_MORE_INFO, notes)
    }
    
    private fun performReviewAction(reviewId: Int, actionType: ReviewActionType, notes: String?) {
        viewModelScope.launch {
            try {
                // TODO: Implement with repository
                val currentState = _uiState.value
                if (currentState is ReviewUiState.Success) {
                    val updatedPendingReviews = currentState.pendingReviews.map { review ->
                        if (review.id == reviewId) {
                            val newStatus = when (actionType) {
                                ReviewActionType.APPROVE -> ReviewStatus.APPROVED
                                ReviewActionType.REJECT -> ReviewStatus.REJECTED
                                ReviewActionType.REQUEST_MORE_INFO -> ReviewStatus.REQUIRES_MORE_INFO
                            }
                            review.copy(
                                status = newStatus,
                                reviewedDate = "2024-03-01T12:00:00Z",
                                notes = notes
                            )
                        } else {
                            review
                        }
                    }
                    
                    val stillPending = updatedPendingReviews.filter { it.status == ReviewStatus.PENDING }
                    val newlyCompleted = updatedPendingReviews.filter { it.status != ReviewStatus.PENDING }
                    val updatedCompleted = currentState.completedReviews + newlyCompleted
                    
                    _uiState.value = currentState.copy(
                        pendingReviews = stillPending,
                        completedReviews = updatedCompleted,
                        selectedReview = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ReviewUiState.Error("Failed to process review action")
                loadReviews()
            }
        }
    }
    
    private fun getMockReviews(): List<ActivityReview> {
        return listOf(
            ActivityReview(
                id = 1,
                activityId = 101,
                volunteerId = 201,
                volunteerName = "John Doe",
                organizationId = 301,
                organizationName = "Green City Initiative",
                activityTitle = "Community Garden Cleanup",
                description = "Helped clean and maintain the community garden for 4 hours",
                dateCompleted = "2024-02-28",
                hoursCompleted = 4,
                submittedDate = "2024-03-01T10:00:00Z",
                status = ReviewStatus.PENDING,
                reviewedDate = null,
                reviewedBy = null,
                notes = null,
                evidence = listOf("https://example.com/photo1.jpg")
            ),
            ActivityReview(
                id = 2,
                activityId = 102,
                volunteerId = 202,
                volunteerName = "Jane Smith",
                organizationId = 302,
                organizationName = "City Food Bank",
                activityTitle = "Food Distribution",
                description = "Assisted with food sorting and distribution for 3 hours",
                dateCompleted = "2024-02-27",
                hoursCompleted = 3,
                submittedDate = "2024-02-28T09:00:00Z",
                status = ReviewStatus.APPROVED,
                reviewedDate = "2024-02-28T15:00:00Z",
                reviewedBy = "Admin User",
                notes = "Well documented activity",
                evidence = listOf("https://example.com/photo2.jpg", "https://example.com/photo3.jpg")
            )
        )
    }
} 