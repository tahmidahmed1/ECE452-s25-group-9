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
    val organizationName: String? = null,
    val institutionName: DomainInstitutionName? = null,
    val createdAt: String? = null,

    // Enhanced volunteer profile fields
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
    val disabilities: String?,
)

// Enumerations are defined in DomainEnums.kt to avoid leaking DTO types into the domain layer.

// Duplicate enums removed; DomainUserType, DomainSex, and DomainInstitutionName
// are now typealiases defined in DomainAliases.kt
