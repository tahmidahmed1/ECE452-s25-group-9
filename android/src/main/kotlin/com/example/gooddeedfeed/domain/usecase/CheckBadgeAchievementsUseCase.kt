package com.example.gooddeedfeed.domain.usecase

import com.example.gooddeedfeed.domain.model.DomainBadgeCheckResponse
import com.example.gooddeedfeed.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckBadgeAchievementsUseCase @Inject constructor(
    private val badgeRepository: BadgeRepository,
) {
    suspend operator fun invoke(): Flow<Result<DomainBadgeCheckResponse>> {
        return badgeRepository.checkBadgeAchievements()
    }
} 