package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.Serializable
import com.example.gooddeedfeed.data.remote.dto.UserDto
import com.example.gooddeedfeed.data.remote.dto.SocialMediaLinkDto

@Serializable
data class SubscriptionCreateDto(
    val organizer_id: Int
)

@Serializable
data class SubscriptionResponseDto(
    val success: Boolean,
    val message: String,
    val is_subscribed: Boolean
)

@Serializable
data class SubscriptionStatusDto(
    val organizer_id: Int,
    val is_subscribed: Boolean,
    val subscribed_at: String? = null
)

@Serializable
data class UserSubscriptionsResponseDto(
    val subscriptions: List<UserDto>
)

@Serializable
data class OrganizerWithSubscriptionStatusDto(
    val id: Int,
    val username: String,
    val email: String,
    val is_active: Boolean,
    val user_type: String? = null,
    val onboarding_completed: Boolean = false,
    val full_name: String? = null,
    val phone: String? = null,
    val profile_picture_url: String? = null,
    val banner_url: String? = null,
    val organization_name: String? = null,
    val organization_type: String? = null,
    val organization_description: String? = null,
    val organization_website: String? = null,
    val organization_social_media: List<SocialMediaLinkDto>? = null,
    val organization_images: List<String>? = null,
    val organization_custom_type: String? = null,
    val sex: String? = null,
    val description: String? = null,
    val skills: List<String>? = null,
    val age: Int? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null,
    val location_area: String? = null,
    val has_drivers_license: Boolean? = null,
    val disabilities: String? = null,
    val karma_points: Int = 0,
    val is_subscribed: Boolean = false,
    val subscriber_count: Int = 0
) 