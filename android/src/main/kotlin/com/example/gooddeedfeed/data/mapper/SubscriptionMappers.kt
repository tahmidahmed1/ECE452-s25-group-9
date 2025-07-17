package com.example.gooddeedfeed.data.mapper

import com.example.gooddeedfeed.data.remote.dto.SubscriptionResponseDto
import com.example.gooddeedfeed.data.remote.dto.SubscriptionStatusDto
import com.example.gooddeedfeed.data.remote.dto.OrganizerWithSubscriptionStatusDto
import com.example.gooddeedfeed.data.remote.dto.SocialMediaLinkDto
import com.example.gooddeedfeed.domain.model.DomainSubscriptionResponse
import com.example.gooddeedfeed.domain.model.DomainSubscriptionStatus
import com.example.gooddeedfeed.domain.model.DomainOrganizerWithSubscriptionStatus
import com.example.gooddeedfeed.domain.model.DomainSocialMediaLink
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainSex

fun SubscriptionResponseDto.toDomainSubscriptionResponse(): DomainSubscriptionResponse {
    return DomainSubscriptionResponse(
        success = success,
        message = message,
        isSubscribed = is_subscribed
    )
}

fun SubscriptionStatusDto.toDomainSubscriptionStatus(): DomainSubscriptionStatus {
    return DomainSubscriptionStatus(
        organizerId = organizer_id,
        isSubscribed = is_subscribed,
        subscribedAt = subscribed_at
    )
}

fun OrganizerWithSubscriptionStatusDto.toDomainOrganizerWithSubscriptionStatus(): DomainOrganizerWithSubscriptionStatus {
    return DomainOrganizerWithSubscriptionStatus(
        id = id,
        username = username,
        email = email,
        isActive = is_active,
        userType = user_type?.let { DomainUserType.valueOf(it.uppercase()) },
        onboardingCompleted = onboarding_completed,
        fullName = full_name,
        phone = phone,
        profilePictureUrl = profile_picture_url,
        bannerUrl = banner_url,
        organizationName = organization_name,
        organizationType = organization_type,
        organizationDescription = organization_description,
        organizationWebsite = organization_website,
        organizationSocialMedia = organization_social_media?.map { it.toDomainSocialMediaLink() },
        organizationImages = organization_images,
        organizationCustomType = organization_custom_type,
        sex = sex?.let { DomainSex.valueOf(it.uppercase()) },
        description = description,
        skills = skills,
        age = age,
        emergencyContactName = emergency_contact_name,
        emergencyContactPhone = emergency_contact_phone,
        locationArea = location_area,
        hasDriversLicense = has_drivers_license,
        disabilities = disabilities,
        karmaPoints = karma_points,
        isSubscribed = is_subscribed,
        subscriberCount = subscriber_count
    )
}

fun SocialMediaLinkDto.toDomainSocialMediaLink(): DomainSocialMediaLink {
    return DomainSocialMediaLink(
        platform = platform.name.lowercase(),
        url = url
    )
} 