package com.example.gooddeedfeed.domain.usecase.volunteer

import com.example.gooddeedfeed.domain.repository.OpportunitiesRepository
import javax.inject.Inject

/**
 * Use case for applying to volunteer opportunities
 */
class ApplyForOpportunityUseCase @Inject constructor(
    private val repository: OpportunitiesRepository,
) {
    suspend operator fun invoke(opportunityId: Int, message: String? = null): Result<Unit> {
        return repository.applyForOpportunity(opportunityId, message)
    }
} 
