package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.remote.BadgeApiService
import com.example.gooddeedfeed.domain.model.DomainBadge
import com.example.gooddeedfeed.domain.model.DomainBadgeAchievement
import com.example.gooddeedfeed.domain.model.DomainBadgeCheckResponse
import com.example.gooddeedfeed.domain.model.DomainUserBadge
import com.example.gooddeedfeed.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepositoryImpl @Inject constructor(
    private val badgeApiService: BadgeApiService,
) : BadgeRepository {

    override suspend fun getAllBadges(): Flow<Result<List<DomainBadge>>> {
        return badgeApiService.getAllBadges().map { result ->
            result.map { badgeDtos ->
                badgeDtos.map { it.toDomain() }
            }
        }
    }

    override suspend fun getUserBadges(): Flow<Result<List<DomainUserBadge>>> {
        return badgeApiService.getUserBadges().map { result ->
            result.map { userBadgeDtos ->
                userBadgeDtos.map { it.toDomain() }
            }
        }
    }

    override suspend fun checkBadgeAchievements(): Flow<Result<DomainBadgeCheckResponse>> {
        return badgeApiService.checkBadgeAchievements().map { result ->
            result.map { dto ->
                // Convert DTO to Domain model
                DomainBadgeCheckResponse(
                    newlyEarnedBadges = dto.newlyEarnedBadges.map { achievement ->
                        DomainBadgeAchievement(
                            badgeId = achievement.badgeId,
                            badgeName = achievement.badgeName,
                            description = achievement.description,
                            iconName = achievement.iconName,
                            color = achievement.color,
                            earnedAt = achievement.earnedAt,
                        )
                    },
                    totalBadgesEarned = dto.totalBadgesEarned,
                    nextBadge = dto.nextBadge?.let { badge ->
                        DomainBadge(
                            id = badge.id,
                            name = badge.name,
                            description = badge.description,
                            requiredKarmaPoints = badge.requiredKarmaPoints,
                            iconName = badge.iconName,
                            color = badge.color,
                            isActive = badge.isActive,
                            createdAt = badge.createdAt,
                        )
                    },
                )
            }
        }
    }
} 
