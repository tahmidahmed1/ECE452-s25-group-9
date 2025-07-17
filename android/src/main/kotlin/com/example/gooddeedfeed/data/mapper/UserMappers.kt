package com.example.gooddeedfeed.data.mapper

import com.example.gooddeedfeed.data.remote.dto.Sex
import com.example.gooddeedfeed.data.remote.dto.UserDto
import com.example.gooddeedfeed.data.remote.dto.UserType
import com.example.gooddeedfeed.data.remote.dto.UserUpdateDto
import com.example.gooddeedfeed.data.remote.dto.OrganizationTypeDto
import com.example.gooddeedfeed.data.remote.dto.SocialMediaLinkDto
import com.example.gooddeedfeed.data.remote.dto.SocialMediaPlatformDto
import com.example.gooddeedfeed.data.remote.dto.BadgeDto
import com.example.gooddeedfeed.data.remote.dto.UserBadgeDto
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainSex
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.model.OrganizationType
import com.example.gooddeedfeed.domain.model.SocialMediaLink
import com.example.gooddeedfeed.domain.model.SocialMediaPlatform
import com.example.gooddeedfeed.domain.model.DomainBadge
import com.example.gooddeedfeed.domain.model.DomainUserBadge

// DTO → Domain
fun UserDto.toDomain(): DomainUser {
    return DomainUser(
        id = id,
        username = username,
        email = email,
        isActive = is_active,
        userType = user_type?.toDomain(),
        onboardingCompleted = onboarding_completed,
        fullName = full_name,
        phone = phone,
        profilePictureUrl = profile_picture_url,
        bannerUrl = banner_url,
        organizationName = organization_name,
        organizationType = organization_type?.toDomain(),
        organizationDescription = organization_description,
        organizationWebsite = organization_website,
        organizationSocialMedia = organization_social_media?.map { it.toDomain() },
        organizationImages = organization_images,
        organizationCustomType = organization_custom_type,
        sex = sex?.toDomain(),
        description = description,
        skills = skills,
        age = age,
        emergencyContactName = emergency_contact_name,
        emergencyContactPhone = emergency_contact_phone,
        locationArea = location_area,
        hasDriversLicense = has_drivers_license,
        disabilities = disabilities,
        karmaPoints = karma_points,
        createdAt = created_at,
        updatedAt = updated_at,
    )
}

fun DomainUserUpdate.toDto(): UserUpdateDto {
    return UserUpdateDto(
        full_name = fullName,
        phone = phone,
        organization_name = organizationName,
        organization_type = organizationType?.toDto(),
        organization_description = organizationDescription,
        organization_website = organizationWebsite,
        organization_social_media = organizationSocialMedia?.map { it.toDto() },
        organization_images = organizationImages,
        organization_custom_type = organizationCustomType,
        sex = sex?.toDto(),
        description = description,
        skills = skills,
        age = age,
        emergency_contact_name = emergencyContactName,
        emergency_contact_phone = emergencyContactPhone,
        location_area = locationArea,
        has_drivers_license = hasDriversLicense,
        disabilities = disabilities,
    )
}

// UserType mappers
fun UserType.toDomain(): DomainUserType = when (this) {
    UserType.VOLUNTEER -> DomainUserType.VOLUNTEER
    UserType.ORGANIZER -> DomainUserType.ORGANIZER
}

// Sex mappers
fun Sex.toDomain(): DomainSex = when (this) {
    Sex.MALE -> DomainSex.MALE
    Sex.FEMALE -> DomainSex.FEMALE
    Sex.NON_BINARY -> DomainSex.NON_BINARY
    Sex.PREFER_NOT_TO_SAY -> DomainSex.PREFER_NOT_TO_SAY
}

// OrganizationType mappers
fun OrganizationTypeDto.toDomain(): OrganizationType = when (this) {
    OrganizationTypeDto.NON_PROFIT -> OrganizationType.NON_PROFIT
    OrganizationTypeDto.SCHOOL_GROUP -> OrganizationType.SCHOOL_GROUP
    OrganizationTypeDto.CLUB -> OrganizationType.CLUB
    OrganizationTypeDto.CHARITY -> OrganizationType.CHARITY
    OrganizationTypeDto.CUSTOM -> OrganizationType.CUSTOM
}

fun OrganizationType.toDto(): OrganizationTypeDto = when (this) {
    OrganizationType.NON_PROFIT -> OrganizationTypeDto.NON_PROFIT
    OrganizationType.SCHOOL_GROUP -> OrganizationTypeDto.SCHOOL_GROUP
    OrganizationType.CLUB -> OrganizationTypeDto.CLUB
    OrganizationType.CHARITY -> OrganizationTypeDto.CHARITY
    OrganizationType.CUSTOM -> OrganizationTypeDto.CUSTOM
}

// SocialMediaPlatform mappers
fun SocialMediaPlatformDto.toDomain(): SocialMediaPlatform = when (this) {
    SocialMediaPlatformDto.INSTAGRAM -> SocialMediaPlatform.INSTAGRAM
    SocialMediaPlatformDto.FACEBOOK -> SocialMediaPlatform.FACEBOOK
    SocialMediaPlatformDto.TWITTER -> SocialMediaPlatform.TWITTER
    SocialMediaPlatformDto.LINKEDIN -> SocialMediaPlatform.LINKEDIN
}

// SocialMediaLink mappers
fun SocialMediaLinkDto.toDomain(): SocialMediaLink = SocialMediaLink(
    platform = platform.toDomain(),
    url = url,
)

// Badge mappers
fun BadgeDto.toDomain(): DomainBadge = DomainBadge(
        id = id,
        name = name,
        description = description,
        requiredKarmaPoints = requiredKarmaPoints,
        iconName = iconName,
        color = color,
        isActive = isActive,
        createdAt = createdAt,
    )

fun UserBadgeDto.toDomain(): DomainUserBadge = DomainUserBadge(
        badge = badge.toDomain(),
        earnedAt = earnedAt,
    )

