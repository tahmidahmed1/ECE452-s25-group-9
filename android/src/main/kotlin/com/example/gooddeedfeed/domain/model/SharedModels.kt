package com.example.gooddeedfeed.domain.model

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

data class VolunteerApplicationForOrganizer(
    val id: Int,
    val volunteerId: Int,
    val volunteerName: String,
    val volunteerEmail: String,
    val applicationDate: String,
    val message: String?,
)

data class VolunteerApplicationForVolunteer(
    val id: Int,
    val opportunityId: Int,
    val opportunityTitle: String,
    val organizationName: String,
    val applicationDate: String,
    val message: String?,
)

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

