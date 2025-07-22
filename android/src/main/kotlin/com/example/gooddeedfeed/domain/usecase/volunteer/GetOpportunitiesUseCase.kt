package com.example.gooddeedfeed.domain.usecase.volunteer

import android.util.Log
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.OpportunityFilters
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.repository.OpportunitiesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for getting volunteer opportunities
 */
class GetOpportunitiesUseCase @Inject constructor(
    private val repository: OpportunitiesRepository,
) {
    suspend operator fun invoke(): Flow<List<VolunteerOpportunity>> {
        Log.d("GetOpportunitiesUseCase", "📥 invoke() - Getting all opportunities...")
        return repository.getOpportunities().map { opportunities ->
            Log.d("GetOpportunitiesUseCase", "📥 invoke() - Repository returned ${opportunities.size} opportunities")
            opportunities
        }
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
        radiusKm: Float?,
        filters: OpportunityFilters,
    ): Flow<List<VolunteerOpportunity>> {
        Log.d("GetOpportunitiesUseCase", "🎯 getOpportunitiesWithFilters() called")
        Log.d("GetOpportunitiesUseCase", "  - lat: $lat, lon: $lon, radiusKm: $radiusKm")
        Log.d("GetOpportunitiesUseCase", "  - filters: $filters")
        return repository.getOpportunitiesWithFilters(lat, lon, radiusKm, filters).map { opportunities ->
            Log.d("GetOpportunitiesUseCase", "🎯 getOpportunitiesWithFilters() - Repository returned ${opportunities.size} opportunities")
            opportunities.forEach { opp ->
                Log.d("GetOpportunitiesUseCase", "  - Opportunity: ${opp.title} (karmaPoints: ${opp.karmaPoints})")
            }
            opportunities
        }
    }
} 
