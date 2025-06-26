package com.example.gooddeedfeed.data.mapper

import com.example.gooddeedfeed.data.remote.dto.*
import com.example.gooddeedfeed.data.remote.dto.AuthResponse
import com.example.gooddeedfeed.domain.model.DomainAuthResponse
import com.example.gooddeedfeed.domain.model.DomainInstitutionName
import com.example.gooddeedfeed.domain.model.DomainProfilePictureUploadResponse
import com.example.gooddeedfeed.domain.model.DomainSex
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType

/**
 * Mappers to convert between data layer DTOs and domain models
 */

// User mappers
fun User.toDomain(): DomainUser = DomainUser(
    id = id,
    username = username,
    email = email,
    isActive = is_active,
    userType = user_type?.toDomain(),
    onboardingCompleted = onboarding_completed,
    fullName = full_name,
    phone = phone,
    profilePictureUrl = profile_picture_url,
    organizationName = organization_name,
    institutionName = institution_name?.toDomain(),
    createdAt = created_at,
    sex = sex?.toDomain(),
    description = description,
    skills = skills,
    age = age,
    emergencyContactName = emergency_contact_name,
    emergencyContactPhone = emergency_contact_phone,
    locationArea = location_area,
    hasDriversLicense = has_drivers_license,
    disabilities = disabilities,
)

// UserType mappers
fun UserType.toDomain(): DomainUserType = when (this) {
    UserType.VOLUNTEER -> DomainUserType.VOLUNTEER
    UserType.ORGANIZER -> DomainUserType.ORGANIZER
    UserType.INSTITUTION -> DomainUserType.INSTITUTION
}

fun DomainUserType.toData(): UserType = when (this) {
    DomainUserType.VOLUNTEER -> UserType.VOLUNTEER
    DomainUserType.ORGANIZER -> UserType.ORGANIZER
    DomainUserType.INSTITUTION -> UserType.INSTITUTION
}

// Sex mappers
fun Sex.toDomain(): DomainSex = when (this) {
    Sex.MALE -> DomainSex.MALE
    Sex.FEMALE -> DomainSex.FEMALE
    Sex.NON_BINARY -> DomainSex.NON_BINARY
    Sex.PREFER_NOT_TO_SAY -> DomainSex.PREFER_NOT_TO_SAY
}

fun DomainSex.toData(): Sex = when (this) {
    DomainSex.MALE -> Sex.MALE
    DomainSex.FEMALE -> Sex.FEMALE
    DomainSex.NON_BINARY -> Sex.NON_BINARY
    DomainSex.PREFER_NOT_TO_SAY -> Sex.PREFER_NOT_TO_SAY
}

// InstitutionName mappers
fun InstitutionName.toDomain(): DomainInstitutionName = when (this) {
    InstitutionName.INSTITUTION_1 -> DomainInstitutionName.INSTITUTION_1
    InstitutionName.INSTITUTION_2 -> DomainInstitutionName.INSTITUTION_2
    InstitutionName.INSTITUTION_3 -> DomainInstitutionName.INSTITUTION_3
}

fun DomainInstitutionName.toData(): InstitutionName = when (this) {
    DomainInstitutionName.INSTITUTION_1 -> InstitutionName.INSTITUTION_1
    DomainInstitutionName.INSTITUTION_2 -> InstitutionName.INSTITUTION_2
    DomainInstitutionName.INSTITUTION_3 -> InstitutionName.INSTITUTION_3
}

// AuthResponse mappers
fun AuthResponse.toDomain(): DomainAuthResponse = DomainAuthResponse(
    success = success,
    token = token,
    message = message,
)

// ProfilePictureUploadResponse mappers
fun ProfilePictureUploadResponse.toDomain(): DomainProfilePictureUploadResponse =
    DomainProfilePictureUploadResponse(
        profilePictureUrl = profile_picture_url,
        message = message,
    ) 
