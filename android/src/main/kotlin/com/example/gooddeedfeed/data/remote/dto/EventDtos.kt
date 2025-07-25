package com.example.gooddeedfeed.data.remote.dto

import com.example.gooddeedfeed.data.mapper.toEmulatorAccessibleUrl
import com.example.gooddeedfeed.domain.model.AttendanceSubmission
import com.example.gooddeedfeed.domain.model.CreateEventData
import com.example.gooddeedfeed.domain.model.EventStatus
import com.example.gooddeedfeed.domain.model.EventVolunteer
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerAttendanceRecord
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.model.toApiValue
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: Int? = null,
    val title: String,
    val description: String? = null,
    val organizer_id: Int? = null,
    val organizer_name: String? = null,
    val location: String? = null,
    val date: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val max_volunteers: Int? = null,
    val current_volunteers: Int? = null,
    val category: String = "other",
    val requirements: List<String> = emptyList(),
    val status: EventStatus = EventStatus.DRAFT,
    val created_at: String? = null,
    val updated_at: String? = null,
    val image_url: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val karma_points: Int = 10,
    val images: List<EventImageDto> = emptyList(),
)

fun EventDto.toDomain(): VolunteerEvent = VolunteerEvent(
    id = id ?: 0,
    title = title,
    description = description ?: "",
    organizationId = organizer_id ?: 0,
    organizationName = organizer_name ?: "",
    location = location ?: "",
    date = date ?: "",
    startTime = start_time ?: "",
    endTime = end_time ?: "",
    maxVolunteers = max_volunteers ?: 0,
    currentVolunteers = current_volunteers ?: 0,
    category = try {
        OpportunityCategory.valueOf(category.uppercase())
    } catch (e: Exception) {
        OpportunityCategory.OTHER
    },
    requirements = requirements,
    status = status,
    createdAt = created_at ?: "",
    updatedAt = updated_at ?: "",
    latitude = latitude ?: 0.0,
    longitude = longitude ?: 0.0,
    karmaPoints = karma_points,
    imageUrl = image_url?.toEmulatorAccessibleUrl(),
    images = images.map { it.copy(image_url = it.image_url.toEmulatorAccessibleUrl()) },
)

fun CreateEventData.toDto(): EventDto = EventDto(
    title = title,
    description = description,
    location = location,
    date = date,
    start_time = startTime,
    end_time = endTime,
    max_volunteers = maxVolunteers,
    category = category.toApiValue(),
    requirements = requirements,
    latitude = latitude,
    longitude = longitude,
    karma_points = karmaPoints,
)

@Serializable
data class EventActionResponseDto(
    val message: String,
    val current_volunteers: Int? = null,
)

/**
 * DTOs for volunteer attendance tracking
 */
@Serializable
data class EventVolunteerDto(
    val id: Int,
    val user_id: Int,
    val name: String,
    val username: String,
    val email: String,
    val profile_picture_url: String? = null,
    val joined_at: String,
)

@Serializable
data class VolunteerAttendanceRecordDto(
    val volunteer_id: Int,
    val hours_worked: Double? = null,
    val is_approved: Boolean,
    val rejection_reason: String? = null,
)

@Serializable
data class AttendanceSubmissionDto(
    val event_id: Int,
    val attendance_records: List<VolunteerAttendanceRecordDto>,
)

@Serializable
data class AttendanceResponseDto(
    val success: Boolean,
    val message: String,
    val karma_points_awarded: Map<String, Int>? = null,
)

/**
 * Mapper functions for attendance DTOs
 */
fun EventVolunteerDto.toDomain(): EventVolunteer = EventVolunteer(
    id = id,
    userId = user_id,
    name = name,
    username = username,
    email = email,
    profilePictureUrl = profile_picture_url?.toEmulatorAccessibleUrl(),
    joinedAt = joined_at,
)

fun VolunteerAttendanceRecord.toDto(): VolunteerAttendanceRecordDto = VolunteerAttendanceRecordDto(
    volunteer_id = volunteerId,
    hours_worked = hoursWorked,
    is_approved = isApproved,
    rejection_reason = rejectionReason,
)

fun AttendanceSubmission.toDto(): AttendanceSubmissionDto = AttendanceSubmissionDto(
    event_id = eventId,
    attendance_records = attendanceRecords.map { it.toDto() },
)
