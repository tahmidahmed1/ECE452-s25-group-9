package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequestDto(
    val username: String,
    val email: String,
    val password: String,
)

@Serializable
data class SignInRequestDto(
    val username: String,
    val password: String,
)

@Serializable
data class AuthResponseDto(
    val access_token: String,
    val token_type: String,
    val user: UserDto,
)

@Serializable
data class TokenResponseDto(
    val access_token: String,
    val token_type: String,
)

@Serializable
data class OnboardingStepOneDto(
    val user_type: UserType,
)

@Serializable
enum class UserType {
    @SerialName("volunteer")
    VOLUNTEER,

    @SerialName("organizer")
    ORGANIZER,
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

@Serializable
data class UserDto(
    val id: Int,
    val username: String,
    val email: String,
    val is_active: Boolean,
    val user_type: UserType? = null,
    val onboarding_completed: Boolean = false,
    val full_name: String? = null,
    val phone: String? = null,
    val profile_picture_url: String? = null,
    val share_profile_picture: Boolean = true,
    val banner_url: String? = null,
    val organization_name: String? = null,
    val organization_type: OrganizationTypeDto? = null,
    val organization_description: String? = null,
    val organization_website: String? = null,
    val organization_social_media: List<SocialMediaLinkDto>? = null,
    val organization_images: List<String>? = null,
    val organization_custom_type: String? = null,
    val sex: Sex? = null,
    val description: String? = null,
    val skills: List<String>? = null,
    val age: Int? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null,
    val location_area: String? = null,
    val has_drivers_license: Boolean? = null,
    val disabilities: String? = null,
    val karma_points: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
)

@Serializable
data class OnboardingStepTwoOrganizerDto(
    val full_name: String,
    val phone: String,
    val organization_name: String,
    val organization_type: OrganizationTypeDto,
    val organization_description: String? = null,
    val organization_website: String? = null,
    val organization_social_media: List<SocialMediaLinkDto>? = null,
    val organization_images: List<String>? = null,
    val organization_custom_type: String? = null,
)

@Serializable
data class UserUpdateDto(
    val full_name: String? = null,
    val phone: String? = null,
    val share_profile_picture: Boolean? = null,
    val organization_name: String? = null,
    val organization_type: OrganizationTypeDto? = null,
    val organization_description: String? = null,
    val organization_website: String? = null,
    val organization_social_media: List<SocialMediaLinkDto>? = null,
    val organization_images: List<String>? = null,
    val organization_custom_type: String? = null,
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
data class OrganizationImagesResponseDto(
    val organization_images: List<String>,
    val message: String,
)

@Serializable
data class ProfilePictureUploadResponse(
    val profile_picture_url: String,
    val message: String,
)

@Serializable
data class OnboardingResponse(val message: String, val user_type: UserType? = null)

@Serializable
enum class OrganizationTypeDto {
    @SerialName("non_profit")
    NON_PROFIT,

    @SerialName("school_group")
    SCHOOL_GROUP,

    @SerialName("club")
    CLUB,

    @SerialName("charity")
    CHARITY,

    @SerialName("custom")
    CUSTOM,
}

@Serializable
enum class SocialMediaPlatformDto {
    @SerialName("instagram")
    INSTAGRAM,

    @SerialName("facebook")
    FACEBOOK,

    @SerialName("twitter")
    TWITTER,

    @SerialName("linkedin")
    LINKEDIN,
}

@Serializable
data class SocialMediaLinkDto(
    val platform: SocialMediaPlatformDto,
    val url: String,
)

@Serializable
data class OnboardingStepThreeVolunteerDto(
    val full_name: String,
    val phone: String,
    val sex: Sex,
    val description: String,
    val skills: List<String>,
    val age: Int,
    val emergency_contact_name: String,
    val emergency_contact_phone: String,
    val location_area: String,
    val has_drivers_license: Boolean,
    val disabilities: String? = null,
)

@Serializable
data class BannerUploadResponse(
    val banner_url: String,
    val message: String,
)
