package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.ActivityReview
import com.example.gooddeedfeed.domain.model.ReviewStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for review management operations
 * Used primarily by institution user type
 */
interface ReviewRepository {
    
    /**
     * Get all pending reviews for the institution
     */
    suspend fun getPendingReviews(): Flow<List<ActivityReview>>
    
    /**
     * Get all completed reviews for the institution
     */
    suspend fun getCompletedReviews(): Flow<List<ActivityReview>>
    
    /**
     * Approve a review
     */
    suspend fun approveReview(reviewId: Int, feedback: String?): Result<Unit>
    
    /**
     * Reject a review
     */
    suspend fun rejectReview(reviewId: Int, reason: String?): Result<Unit>
    
    /**
     * Request more information for a review
     */
    suspend fun requestMoreInfo(reviewId: Int, message: String?): Result<Unit>
    
    /**
     * Get review by ID
     */
    suspend fun getReviewById(reviewId: Int): Result<ActivityReview>
    
    /**
     * Get reviews filtered by status
     */
    suspend fun getReviewsByStatus(status: ReviewStatus): Flow<List<ActivityReview>>
    
    /**
     * Get review statistics for dashboard
     */
    suspend fun getReviewStats(): Result<ReviewStats>
}

data class ReviewStats(
    val totalReviews: Int,
    val pendingCount: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
    val averageProcessingTime: Double // in hours
) 