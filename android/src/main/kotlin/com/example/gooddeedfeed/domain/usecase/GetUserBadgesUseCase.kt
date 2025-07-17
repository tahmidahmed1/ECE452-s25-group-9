package com.example.gooddeedfeed.domain.usecase

import com.example.gooddeedfeed.domain.model.DomainUserBadge
import com.example.gooddeedfeed.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserBadgesUseCase @Inject constructor(
    private val badgeRepository: BadgeRepository,
) {
    suspend operator fun invoke(): Flow<Result<List<DomainUserBadge>>> {
        return badgeRepository.getUserBadges()
    }
} 