package com.example.gooddeedfeed.domain.usecase.volunteer

import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.repository.OpportunitiesRepository
import com.example.gooddeedfeed.domain.model.OpportunityFilters
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting volunteer opportunities
 */
class GetOpportunitiesUseCase @Inject constructor(
    private val repository: OpportunitiesRepository,
) {
    suspend operator fun invoke(): Flow<List<VolunteerOpportunity>> {
        return repository.getOpportunities()
    }

    suspend fun getByCategory(category: OpportunityCategory): Flow<List<VolunteerOpportunity>> {
        return repository.getOpportunitiesByCategory(category)
    }

    suspend fun search(query: String): Flow<List<VolunteerOpportunity>> {
        return repository.searchOpportunities(query)
    }

    suspend fun getNearLocation(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
    ): Flow<List<VolunteerOpportunity>> {
        return repository.getOpportunitiesNearLocation(latitude, longitude, radiusKm)
    }

    suspend fun getOpportunitiesWithFilters(
        lat: Double?,
        lon: Double?,
        radiusKm: Float,
        filters: OpportunityFilters
    ): Flow<List<VolunteerOpportunity>> {
        return repository.getOpportunitiesWithFilters(lat, lon, radiusKm, filters)
    }
} 
