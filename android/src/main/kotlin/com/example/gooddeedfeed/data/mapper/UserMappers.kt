package com.example.gooddeedfeed.data.mapper

import com.example.gooddeedfeed.data.remote.dto.BadgeDto
import com.example.gooddeedfeed.data.remote.dto.Sex
import com.example.gooddeedfeed.data.remote.dto.SocialMediaLinkDto
import com.example.gooddeedfeed.data.remote.dto.SocialMediaPlatformDto
import com.example.gooddeedfeed.data.remote.dto.UserBadgeDto
import com.example.gooddeedfeed.data.remote.dto.UserDto
import com.example.gooddeedfeed.data.remote.dto.UserType
import com.example.gooddeedfeed.data.remote.dto.UserUpdateDto
import com.example.gooddeedfeed.data.remote.dto.VolunteerHistoryEntryDto
import com.example.gooddeedfeed.domain.model.DomainBadge
import com.example.gooddeedfeed.domain.model.DomainSex
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserBadge
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.model.DomainVolunteerHistoryEntry
import com.example.gooddeedfeed.domain.model.SocialMediaLink
import com.example.gooddeedfeed.domain.model.SocialMediaPlatform

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
        profilePictureUrl = profile_picture_url?.toEmulatorAccessibleUrl(),
        bannerUrl = banner_url?.toEmulatorAccessibleUrl(),
        organizationName = organization_name,
        organizationDescription = organization_description,
        organizationWebsite = organization_website,
        organizationSocialMedia = organization_social_media?.map { it.toDomain() },
        organizationImages = organization_images?.map { it.toEmulatorAccessibleUrl() }.also { mapped ->
            android.util.Log.d("UserMappers", "📸 User Organization Images Mapping:")
            android.util.Log.d("UserMappers", "📸 Raw organization_images: ${organization_images?.size ?: 0} items")
            android.util.Log.d("UserMappers", "📸 Raw URLs: $organization_images")
            android.util.Log.d("UserMappers", "📸 Mapped URLs: $mapped")
        },
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
        organization_description = organizationDescription,
        organization_website = organizationWebsite,
        organization_social_media = organizationSocialMedia?.map { it.toDto() },
        organization_images = organizationImages,
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

fun UserType.toDomain(): DomainUserType = when (this) {
    UserType.VOLUNTEER -> DomainUserType.VOLUNTEER
    UserType.ORGANIZER -> DomainUserType.ORGANIZER
}

fun Sex.toDomain(): DomainSex = when (this) {
    Sex.MALE -> DomainSex.MALE
    Sex.FEMALE -> DomainSex.FEMALE
    Sex.NON_BINARY -> DomainSex.NON_BINARY
    Sex.PREFER_NOT_TO_SAY -> DomainSex.PREFER_NOT_TO_SAY
}

fun SocialMediaPlatformDto.toDomain(): SocialMediaPlatform = when (this) {
    SocialMediaPlatformDto.INSTAGRAM -> SocialMediaPlatform.INSTAGRAM
    SocialMediaPlatformDto.FACEBOOK -> SocialMediaPlatform.FACEBOOK
    SocialMediaPlatformDto.TWITTER -> SocialMediaPlatform.TWITTER
    SocialMediaPlatformDto.LINKEDIN -> SocialMediaPlatform.LINKEDIN
}

fun SocialMediaLinkDto.toDomain(): SocialMediaLink = SocialMediaLink(
    platform = platform.toDomain(),
    url = url,
)

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

fun VolunteerHistoryEntryDto.toDomain(): DomainVolunteerHistoryEntry = DomainVolunteerHistoryEntry(
    eventId = event_id,
    eventTitle = event_title,
    eventDate = event_date,
    eventDescription = event_description,
    eventLocation = event_location,
    eventStartTime = event_start_time,
    eventEndTime = event_end_time,
    eventImageUrls = event_image_urls,
    organizerName = organizer_name,
    hoursWorked = hours_worked,
    isApproved = is_approved,
    rejectionReason = rejection_reason,
    karmaPointsEarned = karma_points_earned,
    status = status,
)
