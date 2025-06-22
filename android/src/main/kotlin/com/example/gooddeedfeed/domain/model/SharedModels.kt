package com.example.gooddeedfeed.domain.model

/**
 * Domain models shared across multiple user types
 */

/**
 * Application status for volunteer applications
 */
enum class ApplicationStatus {
    PENDING, APPROVED, REJECTED, WITHDRAWN
}

/**
 * Volunteer application from organizer's perspective
 */
data class VolunteerApplicationForOrganizer(
    val id: Int,
    val volunteerId: Int,
    val volunteerName: String,
    val volunteerEmail: String,
    val applicationDate: String,
    val status: ApplicationStatus,
    val message: String?,
)

/**
 * Volunteer application from volunteer's perspective
 */
data class VolunteerApplicationForVolunteer(
    val id: Int,
    val opportunityId: Int,
    val opportunityTitle: String,
    val organizationName: String,
    val applicationDate: String,
    val status: ApplicationStatus,
    val message: String?,
) 
