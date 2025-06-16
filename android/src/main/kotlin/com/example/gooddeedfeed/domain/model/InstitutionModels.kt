package com.example.gooddeedfeed.domain.model

import com.example.gooddeedfeed.data.remote.InstitutionName

/**
 * Domain models specific to institution functionality
 */

data class ActivityReview(
    val id: Int,
    val activityId: Int,
    val volunteerId: Int,
    val volunteerName: String,
    val organizationId: Int,
    val organizationName: String,
    val activityTitle: String,
    val description: String,
    val dateCompleted: String,
    val hoursCompleted: Int,
    val submittedDate: String,
    val status: ReviewStatus,
    val reviewedDate: String?,
    val reviewedBy: String?,
    val notes: String?,
    val evidence: List<String> // URLs to uploaded evidence
)

enum class ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REQUIRES_MORE_INFO
}

data class InstitutionProfile(
    val id: Int,
    val userId: Int,
    val institutionName: InstitutionName,
    val displayName: String,
    val description: String,
    val contactEmail: String,
    val contactPhone: String,
    val address: String,
    val accreditationNumber: String?,
    val totalActivitiesReviewed: Int,
    val totalHoursApproved: Int,
    val averageReviewTime: Double // in days
)

data class ReviewStatistics(
    val institutionId: Int,
    val periodStart: String,
    val periodEnd: String,
    val totalReviews: Int,
    val approvedReviews: Int,
    val rejectedReviews: Int,
    val pendingReviews: Int,
    val averageReviewTimeHours: Double,
    val totalHoursApproved: Int,
    val topOrganizations: List<OrganizationSummary>
)

data class OrganizationSummary(
    val organizationId: Int,
    val organizationName: String,
    val totalActivities: Int,
    val totalHoursApproved: Int,
    val approvalRate: Double
)

data class ReviewAction(
    val reviewId: Int,
    val action: ReviewActionType,
    val notes: String?,
    val reviewedBy: String
)

enum class ReviewActionType {
    APPROVE,
    REJECT,
    REQUEST_MORE_INFO
} 