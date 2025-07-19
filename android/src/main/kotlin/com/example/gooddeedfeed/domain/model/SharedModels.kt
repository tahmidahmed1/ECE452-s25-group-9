package com.example.gooddeedfeed.domain.model

/**
 * Domain models shared across multiple user types
 */

// Badge domain models
data class DomainBadge(
    val id: Int,
    val name: String,
    val description: String?,
    val requiredKarmaPoints: Int,
    val iconName: String,
    val color: String?,
    val isActive: Boolean,
    val createdAt: String,
)

data class DomainUserBadge(
    val badge: DomainBadge,
    val earnedAt: String,
)

data class DomainBadgeAchievement(
    val badgeId: Int,
    val badgeName: String,
    val description: String,
    val iconName: String,
    val color: String?,
    val earnedAt: String,
)

data class DomainBadgeCheckResponse(
    val newlyEarnedBadges: List<DomainBadgeAchievement>,
    val totalBadgesEarned: Int,
    val nextBadge: DomainBadge?,
)

enum class ApplicationStatus {
    PENDING, APPROVED, REJECTED, WITHDRAWN
}

data class VolunteerApplicationForOrganizer(
    val id: Int,
    val volunteerId: Int,
    val volunteerName: String,
    val volunteerEmail: String,
    val applicationDate: String,
    val status: ApplicationStatus,
    val message: String?,
)

data class VolunteerApplicationForVolunteer(
    val id: Int,
    val opportunityId: Int,
    val opportunityTitle: String,
    val organizationName: String,
    val applicationDate: String,
    val status: ApplicationStatus,
    val message: String?,
)

enum class ApprovalStatus(val displayName: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    ;

    companion object {
        fun fromString(value: String): ApprovalStatus? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

enum class SocialMediaPlatform(val displayName: String, val iconName: String) {
    INSTAGRAM("Instagram", "instagram"),
    FACEBOOK("Facebook", "facebook"),
    TWITTER("Twitter", "twitter"),
    LINKEDIN("LinkedIn", "linkedin"),
    ;

    companion object {
        fun fromString(value: String): SocialMediaPlatform? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

data class SocialMediaLink(
    val platform: SocialMediaPlatform,
    val url: String,
)

data class OrganizationProfile(
    val name: String,
    val description: String? = null,
    val website: String? = null,
    val socialMediaLinks: List<SocialMediaLink> = emptyList(),
    val images: List<String> = emptyList(),
) 
