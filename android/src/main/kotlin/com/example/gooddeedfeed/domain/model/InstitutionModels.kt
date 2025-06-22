package com.example.gooddeedfeed.domain.model

/**
 * Domain models specific to institution functionality
 */

data class ActivityReview(
    val id: Int,
    val volunteerName: String,
    val eventTitle: String,
    val organizationName: String,
    val completionDate: String,
    val hoursCompleted: Int,
    val description: String,
    val evidenceUrls: List<String>,
    val status: ReviewStatus,
    val submittedAt: String,
    val reviewedAt: String?,
    val reviewerNotes: String?,
)

enum class ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REQUIRES_MORE_INFO,
}

enum class ReviewActionType {
    APPROVE,
    REJECT,
    REQUEST_MORE_INFO,
} 
