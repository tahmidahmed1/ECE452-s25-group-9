package com.example.gooddeedfeed.domain.model

/**
 * Domain models for user-related entities
 * These are independent of data layer implementations
 */

data class DomainUser(
    val id: Int,
    val username: String,
    val email: String,
    val isActive: Boolean,
    val userType: DomainUserType? = null,
    val onboardingCompleted: Boolean = false,
    val fullName: String? = null,
    val phone: String? = null,
    val profilePictureUrl: String? = null,
    val bannerUrl: String? = null,
    val organizationName: String? = null,
    val organizationDescription: String? = null,
    val organizationWebsite: String? = null,
    val organizationSocialMedia: List<SocialMediaLink>? = null,
    val organizationImages: List<String>? = null,
    val sex: DomainSex? = null,
    val description: String? = null,
    val skills: List<String>? = null,
    val age: Int? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val locationArea: String? = null,
    val hasDriversLicense: Boolean? = null,
    val disabilities: String? = null,
    val karmaPoints: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class DomainAuthResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String? = null,
)

data class DomainProfilePictureUploadResponse(
    val profilePictureUrl: String,
    val message: String,
)

/**
 * Data class for updating volunteer profile during onboarding
 */
data class DomainVolunteerProfile(
    val fullName: String,
    val phone: String,
    val sex: DomainSex,
    val description: String,
    val skills: List<String>,
    val age: Int,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val locationArea: String,
    val hasDriversLicense: Boolean,
    val disabilities: String? = null,
)

/**
 * Data class for updating organizer profile during onboarding
 */
data class DomainOrganizerProfile(
    val fullName: String,
    val phone: String,
    val organizationName: String,
    val organizationDescription: String? = null,
    val organizationWebsite: String? = null,
    val organizationSocialMedia: List<SocialMediaLink>? = null,
    val organizationImages: List<String>? = null,
)

/**
 * Data class for updating any subset of user profile fields
 */
data class DomainUserUpdate(
    val fullName: String? = null,
    val phone: String? = null,
    val organizationName: String? = null,
    val organizationDescription: String? = null,
    val organizationWebsite: String? = null,
    val organizationSocialMedia: List<SocialMediaLink>? = null,
    val organizationImages: List<String>? = null,
    val sex: DomainSex? = null,
    val description: String? = null,
    val skills: List<String>? = null,
    val age: Int? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val locationArea: String? = null,
    val hasDriversLicense: Boolean? = null,
    val disabilities: String? = null,
)

/**
 * Response for organization image uploads
 */
data class DomainOrganizationImagesResponse(
    val organizationImages: List<String>,
    val message: String,
    val totalImages: Int,
)

// Enumerations are defined in DomainEnums.kt to avoid leaking DTO types into the domain layer.
