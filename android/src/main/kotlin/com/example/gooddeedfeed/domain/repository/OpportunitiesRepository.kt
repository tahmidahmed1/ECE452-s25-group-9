package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerApplicationForVolunteer
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.model.OpportunityFilters
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for volunteer opportunities
 * Used primarily by volunteer user type
 */
interface OpportunitiesRepository {

    /**
     * Get all available volunteer opportunities
     */
    suspend fun getOpportunities(): Flow<List<VolunteerOpportunity>>

    /**
     * Get opportunities filtered by category
     */
    suspend fun getOpportunitiesByCategory(category: OpportunityCategory): Flow<List<VolunteerOpportunity>>

    /**
     * Search opportunities by keyword
     */
    suspend fun searchOpportunities(query: String): Flow<List<VolunteerOpportunity>>

    /**
     * Get opportunities near a location
     */
    suspend fun getOpportunitiesNearLocation(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
    ): Flow<List<VolunteerOpportunity>>

    /**
     * Get opportunities with filters applied
     */
    suspend fun getOpportunitiesWithFilters(
        lat: Double? = null,
        lon: Double? = null,
        radiusKm: Float = 50f,
        filters: OpportunityFilters
    ): Flow<List<VolunteerOpportunity>>

    /**
     * Apply for an opportunity
     */
    suspend fun applyForOpportunity(opportunityId: Int, message: String?): Result<Unit>

    /**
     * Get user's applied opportunities
     */
    suspend fun getMyApplications(): Flow<List<VolunteerApplicationForVolunteer>>

    /**
     * Cancel application for an opportunity
     */
    suspend fun cancelApplication(opportunityId: Int): Result<Unit>

    /**
     * Get opportunity details by ID
     */
    suspend fun getOpportunityById(opportunityId: Int): Result<VolunteerOpportunity>

    /**
     * Get all available categories
     */
    suspend fun getCategories(): List<OpportunityCategory>
} 
