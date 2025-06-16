package com.example.gooddeedfeed.domain.model

/**
 * Domain models specific to organizer functionality
 */

data class VolunteerEvent(
    val id: Int,
    val title: String,
    val description: String,
    val organizationId: Int,
    val organizationName: String,
    val location: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val maxVolunteers: Int,
    val currentVolunteers: Int,
    val category: OpportunityCategory,
    val requirements: List<String>,
    val status: EventStatus,
    val createdAt: String,
    val updatedAt: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class EventRegistration(
    val id: Int,
    val eventId: Int,
    val volunteerId: Int,
    val volunteerName: String,
    val volunteerEmail: String,
    val registrationDate: String,
    val status: RegistrationStatus,
    val notes: String?
)

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class RegistrationStatus {
    PENDING,
    CONFIRMED,
    WAITLISTED,
    DECLINED,
    NO_SHOW,
    COMPLETED
}

data class OrganizationProfile(
    val id: Int,
    val userId: Int,
    val organizationName: String,
    val description: String,
    val contactEmail: String,
    val contactPhone: String,
    val address: String,
    val website: String?,
    val focusAreas: List<OpportunityCategory>,
    val isVerified: Boolean,
    val totalEventsCreated: Int,
    val totalVolunteersReached: Int,
    val rating: Double?
)

data class EventAnalytics(
    val eventId: Int,
    val totalRegistrations: Int,
    val completedRegistrations: Int,
    val noShowRate: Double,
    val averageRating: Double?,
    val feedbackCount: Int
) 