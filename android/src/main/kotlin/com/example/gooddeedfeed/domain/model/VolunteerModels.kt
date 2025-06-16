package com.example.gooddeedfeed.domain.model

import com.example.gooddeedfeed.data.remote.UserType

/**
 * Domain models specific to volunteer functionality
 */

data class VolunteerOpportunity(
    val id: Int,
    val title: String,
    val organizationName: String,
    val location: String,
    val date: String,
    val description: String,
    val requiredVolunteers: Int,
    val currentVolunteers: Int,
    val category: OpportunityCategory,
    val isActive: Boolean = true
)



data class VolunteerActivity(
    val id: Int,
    val opportunityId: Int,
    val opportunityTitle: String,
    val organizationName: String,
    val dateCompleted: String,
    val hoursCompleted: Int,
    val status: ActivityStatus,
    val description: String?
)

enum class OpportunityCategory {
    COMMUNITY_SERVICE,
    EDUCATION,
    ENVIRONMENTAL,
    HEALTHCARE,
    SOCIAL_SERVICES,
    DISASTER_RELIEF,
    OTHER
}

enum class ActivityStatus {
    REGISTERED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class VolunteerProfile(
    val userId: Int,
    val interests: List<OpportunityCategory>,
    val skills: List<String>,
    val availableDays: List<DayOfWeek>,
    val maxTravelDistance: Int, // in kilometers
    val totalHoursCompleted: Int,
    val rating: Double?
)

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
} 