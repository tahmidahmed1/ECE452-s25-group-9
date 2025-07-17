package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BadgeDto(
    val id: Int,
    val name: String,
    val description: String?,
    @SerialName("required_karma_points")
    val requiredKarmaPoints: Int,
    @SerialName("icon_name")
    val iconName: String,
    val color: String?,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class UserBadgeDto(
    val badge: BadgeDto,
    @SerialName("earned_at")
    val earnedAt: String,
)

@Serializable
data class BadgeAchievementDto(
    @SerialName("badge_id")
    val badgeId: Int,
    @SerialName("badge_name")
    val badgeName: String,
    val description: String,
    @SerialName("icon_name")
    val iconName: String,
    val color: String?,
    @SerialName("earned_at")
    val earnedAt: String,
)

@Serializable
data class BadgeCheckResponseDto(
    @SerialName("newly_earned_badges")
    val newlyEarnedBadges: List<BadgeAchievementDto>,
    @SerialName("total_badges_earned")
    val totalBadgesEarned: Int,
    @SerialName("next_badge")
    val nextBadge: BadgeDto?,
) 
