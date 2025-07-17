package com.example.gooddeedfeed.domain.usecase

import com.example.gooddeedfeed.domain.model.DomainBadge
import com.example.gooddeedfeed.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBadgesUseCase @Inject constructor(
    private val badgeRepository: BadgeRepository,
) {
    suspend operator fun invoke(): Flow<Result<List<DomainBadge>>> {
        return badgeRepository.getAllBadges()
    }
} 
