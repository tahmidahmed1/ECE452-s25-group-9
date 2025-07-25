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
    val longitude: Double = 0.0,
    val karmaPoints: Int = 10,
    val imageUrl: String? = null,
    val images: List<com.example.gooddeedfeed.data.remote.dto.EventImageDto> = emptyList(),
)

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

/**
 * Data class for creating/updating events
 */
data class CreateEventData(
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val maxVolunteers: Int,
    val category: OpportunityCategory,
    val requirements: List<String>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val karmaPoints: Int = 10,
)

/**
 * Domain models for volunteer attendance tracking
 */
data class EventVolunteer(
    val id: Int,
    val userId: Int,
    val name: String,
    val username: String,
    val email: String,
    val profilePictureUrl: String? = null,
    val joinedAt: String,
)

data class VolunteerAttendanceRecord(
    val volunteerId: Int,
    val hoursWorked: Double?,
    val isApproved: Boolean,
    val rejectionReason: String? = null,
)

data class AttendanceSubmission(
    val eventId: Int,
    val attendanceRecords: List<VolunteerAttendanceRecord>,
)
