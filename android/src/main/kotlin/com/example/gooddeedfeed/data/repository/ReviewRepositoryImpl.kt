package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.domain.model.ActivityReview
import com.example.gooddeedfeed.domain.model.ReviewStatus
import com.example.gooddeedfeed.domain.repository.ReviewRepository
import com.example.gooddeedfeed.domain.repository.ReviewStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    // TODO: Inject API service when available
) : ReviewRepository {
    
    override suspend fun getPendingReviews(): Flow<List<ActivityReview>> = flow {
        delay(500)
        emit(getMockReviews().filter { it.status == ReviewStatus.PENDING })
    }
    
    override suspend fun getCompletedReviews(): Flow<List<ActivityReview>> = flow {
        delay(500)
        emit(getMockReviews().filter { it.status != ReviewStatus.PENDING })
    }
    
    override suspend fun approveReview(reviewId: Int, feedback: String?): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }
    
    override suspend fun rejectReview(reviewId: Int, reason: String?): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }
    
    override suspend fun requestMoreInfo(reviewId: Int, message: String?): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }
    
    override suspend fun getReviewById(reviewId: Int): Result<ActivityReview> {
        delay(500)
        val review = getMockReviews().find { it.id == reviewId }
        return if (review != null) {
            Result.success(review)
        } else {
            Result.failure(Exception("Review not found"))
        }
    }
    
    override suspend fun getReviewsByStatus(status: ReviewStatus): Flow<List<ActivityReview>> = flow {
        delay(500)
        emit(getMockReviews().filter { it.status == status })
    }
    
    override suspend fun getReviewStats(): Result<ReviewStats> {
        delay(500)
        val reviews = getMockReviews()
        val stats = ReviewStats(
            totalReviews = reviews.size,
            pendingCount = reviews.count { it.status == ReviewStatus.PENDING },
            approvedCount = reviews.count { it.status == ReviewStatus.APPROVED },
            rejectedCount = reviews.count { it.status == ReviewStatus.REJECTED },
            averageProcessingTime = 24.5
        )
        return Result.success(stats)
    }
    
    private fun getMockReviews(): List<ActivityReview> {
        return listOf(
            ActivityReview(
                id = 1,
                volunteerName = "John Doe",
                eventTitle = "Beach Cleanup Drive",
                organizationName = "Ocean Conservation Society",
                completionDate = "2024-01-15",
                hoursCompleted = 4,
                description = "Participated in beach cleanup and collected 50lbs of trash",
                evidenceUrls = listOf("https://example.com/evidence1.jpg"),
                status = ReviewStatus.PENDING,
                submittedAt = "2024-01-16T10:00:00Z",
                reviewedAt = null,
                reviewerNotes = null
            ),
            ActivityReview(
                id = 2,
                volunteerName = "Jane Smith",
                eventTitle = "Reading Program for Kids",
                organizationName = "Learning Together Foundation",
                completionDate = "2024-01-18",
                hoursCompleted = 3,
                description = "Tutored 5 children in reading skills improvement",
                evidenceUrls = listOf("https://example.com/evidence2.jpg", "https://example.com/evidence3.jpg"),
                status = ReviewStatus.APPROVED,
                submittedAt = "2024-01-19T14:00:00Z",
                reviewedAt = "2024-01-20T09:00:00Z",
                reviewerNotes = "Excellent work with the children!"
            ),
            ActivityReview(
                id = 3,
                volunteerName = "Mike Johnson",
                eventTitle = "Food Bank Support",
                organizationName = "City Food Bank",
                completionDate = "2024-01-20",
                hoursCompleted = 5,
                description = "Sorted and packaged 200 food items for distribution",
                evidenceUrls = listOf(),
                status = ReviewStatus.REQUIRES_MORE_INFO,
                submittedAt = "2024-01-21T16:00:00Z",
                reviewedAt = "2024-01-22T11:00:00Z",
                reviewerNotes = "Please provide photos of the completed work"
            )
        )
    }
} 