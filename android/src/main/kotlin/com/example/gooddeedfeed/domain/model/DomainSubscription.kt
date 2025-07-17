package com.example.gooddeedfeed.domain.model

data class DomainSubscriptionResponse(
    val success: Boolean,
    val message: String,
    val isSubscribed: Boolean
)

data class DomainSubscriptionStatus(
    val organizerId: Int,
    val isSubscribed: Boolean,
    val subscribedAt: String? = null
)

data class DomainOrganizerWithSubscriptionStatus(
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
    val organizationType: String? = null,
    val organizationDescription: String? = null,
    val organizationWebsite: String? = null,
    val organizationSocialMedia: List<DomainSocialMediaLink>? = null,
    val organizationImages: List<String>? = null,
    val organizationCustomType: String? = null,
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
    val isSubscribed: Boolean = false,
    val subscriberCount: Int = 0
)

data class DomainSocialMediaLink(
    val platform: String,
    val url: String
) 