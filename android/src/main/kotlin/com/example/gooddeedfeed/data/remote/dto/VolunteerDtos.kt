package com.example.gooddeedfeed.data.remote.dto

import com.example.gooddeedfeed.domain.model.JoinedVolunteer
import kotlinx.serialization.Serializable

@Serializable
data class VolunteerDto(
    val id: Int,
    val username: String? = null,
    val email: String? = null,
    val full_name: String? = null,
    val profile_picture_url: String? = null,
    val phone: String? = null,
    val sex: String? = null,
    val age: Int? = null,
    val description: String? = null,
    val skills: List<String>? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null,
    val location_area: String? = null,
    val has_drivers_license: Boolean? = null,
    val disabilities: String? = null,
    val karma_points: Int? = null,
    val user_type: String? = null,
    val is_active: Boolean? = null,
    val onboarding_completed: Boolean? = null,
)

@Serializable
data class EventVolunteersResponseDto(
    val volunteers: List<VolunteerDto>,
    val total_count: Int,
)

fun VolunteerDto.toDomain(): JoinedVolunteer = JoinedVolunteer(
    id = id,
    username = username ?: "",
    email = email ?: "",
    fullName = full_name ?: username ?: "",
    profilePictureUrl = profile_picture_url,
    phone = phone,
    sex = sex,
    age = age,
    description = description,
    skills = skills,
    emergencyContactName = emergency_contact_name,
    emergencyContactPhone = emergency_contact_phone,
    locationArea = location_area,
    hasDriversLicense = has_drivers_license,
    disabilities = disabilities,
    karmaPoints = karma_points ?: 0,
    userType = user_type ?: "volunteer",
    isActive = is_active ?: true,
    onboardingCompleted = onboarding_completed ?: true,
) 
