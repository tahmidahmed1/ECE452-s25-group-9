package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(val username: String, val email: String, val password: String)

@Serializable
data class SignInRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val success: Boolean, val token: String? = null, val message: String? = null)

@Serializable
data class TokenResponse(val access_token: String, val token_type: String)

@Serializable
data class ValidationError(
    val type: String,
    val loc: List<String>,
    val msg: String,
    val input: String? = null,
)

@Serializable
data class ErrorResponse(val success: Boolean = false, val message: String, val errors: List<ValidationError>? = null)

// ------------------ Enums ------------------
@Serializable
enum class UserType {
    @SerialName("volunteer")
    VOLUNTEER,

    @SerialName("organizer")
    ORGANIZER,

    @SerialName("institution")
    INSTITUTION,
}

@Serializable
enum class InstitutionName {
    @SerialName("Institution 1")
    INSTITUTION_1,

    @SerialName("Institution 2")
    INSTITUTION_2,

    @SerialName("Institution 3")
    INSTITUTION_3,
}

@Serializable
enum class Sex {
    @SerialName("male")
    MALE,

    @SerialName("female")
    FEMALE,

    @SerialName("non_binary")
    NON_BINARY,

    @SerialName("prefer_not_to_say")
    PREFER_NOT_TO_SAY,
}

// ------------------ Complex DTOs ------------------
@Serializable
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val is_active: Boolean,
    val user_type: UserType? = null,
    val onboarding_completed: Boolean = false,
    val full_name: String? = null,
    val phone: String? = null,
    val profile_picture_url: String? = null,
    val organization_name: String? = null,
    val institution_name: InstitutionName? = null,
    val created_at: String? = null,

    // Enhanced volunteer profile fields
    val sex: Sex? = null,
    val description: String? = null,
    val skills: List<String>? = null,
    val age: Int? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null,
    val location_area: String? = null,
    val has_drivers_license: Boolean? = null,
    val disabilities: String? = null,
)

@Serializable
data class OnboardingStepOneRequest(val user_type: UserType)

@Serializable
data class OnboardingCompleteRequest(
    val user_type: UserType,
    val full_name: String,
    val phone: String,
    val organization_name: String? = null,
    val institution_name: InstitutionName? = null,
    // Volunteer-specific fields
    val sex: Sex? = null,
    val description: String? = null,
    val skills: List<String>? = null,
    val age: Int? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null,
    val location_area: String? = null,
    val has_drivers_license: Boolean? = null,
    val disabilities: String? = null,
)

@Serializable
data class InstitutionOption(val value: String, val label: String)

@Serializable
data class ProfilePictureUploadResponse(
    val profile_picture_url: String,
    val message: String,
)

@Serializable
data class OnboardingResponse(val message: String, val user_type: UserType? = null) 
