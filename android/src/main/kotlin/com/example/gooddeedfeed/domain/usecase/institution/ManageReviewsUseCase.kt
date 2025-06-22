package com.example.gooddeedfeed.domain.usecase.institution

import com.example.gooddeedfeed.domain.model.ActivityReview
import com.example.gooddeedfeed.domain.model.ReviewStatus
import com.example.gooddeedfeed.domain.repository.ReviewRepository
import com.example.gooddeedfeed.domain.repository.ReviewStats
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing reviews (institution functionality)
 */
class ManageReviewsUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    suspend fun getPendingReviews(): Flow<List<ActivityReview>> {
        return repository.getPendingReviews()
    }

    suspend fun getCompletedReviews(): Flow<List<ActivityReview>> {
        return repository.getCompletedReviews()
    }

    suspend fun approveReview(reviewId: Int, feedback: String? = null): Result<Unit> {
        return repository.approveReview(reviewId, feedback)
    }

    suspend fun rejectReview(reviewId: Int, reason: String? = null): Result<Unit> {
        return repository.rejectReview(reviewId, reason)
    }

    suspend fun requestMoreInfo(reviewId: Int, message: String? = null): Result<Unit> {
        return repository.requestMoreInfo(reviewId, message)
    }

    suspend fun getReviewById(reviewId: Int): Result<ActivityReview> {
        return repository.getReviewById(reviewId)
    }

    suspend fun getReviewsByStatus(status: ReviewStatus): Flow<List<ActivityReview>> {
        return repository.getReviewsByStatus(status)
    }

    suspend fun getReviewStats(): Result<ReviewStats> {
        return repository.getReviewStats()
    }
} 
