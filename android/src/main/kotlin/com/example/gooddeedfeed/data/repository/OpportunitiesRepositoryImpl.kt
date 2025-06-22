package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerApplicationForVolunteer
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.repository.OpportunitiesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpportunitiesRepositoryImpl @Inject constructor(
    // TODO: Inject API service when available
) : OpportunitiesRepository {
    
    override suspend fun getOpportunities(): Flow<List<VolunteerOpportunity>> = flow {
        delay(500)
        emit(getMockOpportunities())
    }
    
    override suspend fun getOpportunitiesByCategory(category: OpportunityCategory): Flow<List<VolunteerOpportunity>> = flow {
        delay(500)
        emit(getMockOpportunities().filter { it.category == category })
    }
    
    override suspend fun searchOpportunities(query: String): Flow<List<VolunteerOpportunity>> = flow {
        delay(500)
        emit(getMockOpportunities().filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.description.contains(query, ignoreCase = true) ||
            it.organizationName.contains(query, ignoreCase = true)
        })
    }
    
    override suspend fun getOpportunitiesNearLocation(
        latitude: Double, 
        longitude: Double, 
        radiusKm: Double
    ): Flow<List<VolunteerOpportunity>> = flow {
        delay(500)
        // TODO: Implement location-based filtering when location data is available
        emit(getMockOpportunities())
    }
    
    override suspend fun applyForOpportunity(opportunityId: Int, message: String?): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }
    
    override suspend fun getMyApplications(): Flow<List<VolunteerApplicationForVolunteer>> = flow {
        delay(500)
        emit(emptyList()) // TODO: Implement when API is available
    }
    
    override suspend fun cancelApplication(opportunityId: Int): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }
    
    override suspend fun getOpportunityById(opportunityId: Int): Result<VolunteerOpportunity> {
        delay(500)
        val opportunity = getMockOpportunities().find { it.id == opportunityId }
        return if (opportunity != null) {
            Result.success(opportunity)
        } else {
            Result.failure(Exception("Opportunity not found"))
        }
    }
    
    override suspend fun getCategories(): List<OpportunityCategory> {
        return OpportunityCategory.values().toList()
    }
    
    private fun getMockOpportunities(): List<VolunteerOpportunity> {
        return listOf(
            VolunteerOpportunity(
                id = 1,
                title = "Beach Cleanup Drive",
                organizationName = "Ocean Conservation Society",
                location = "Sunset Beach",
                date = "2024-02-18",
                description = "Join us for a morning beach cleanup to protect marine life",
                requiredVolunteers = 30,
                currentVolunteers = 12,
                category = OpportunityCategory.ENVIRONMENTAL
            ),
            VolunteerOpportunity(
                id = 2,
                title = "Reading Program for Kids",
                organizationName = "Learning Together Foundation",
                location = "Central Library",
                date = "2024-02-22",
                description = "Help children improve their reading skills through one-on-one sessions",
                requiredVolunteers = 10,
                currentVolunteers = 6,
                category = OpportunityCategory.EDUCATION
            ),
            VolunteerOpportunity(
                id = 3,
                title = "Senior Care Assistance",
                organizationName = "Golden Years Care",
                location = "Riverside Senior Center",
                date = "2024-02-25",
                description = "Provide companionship and assistance to elderly residents",
                requiredVolunteers = 15,
                currentVolunteers = 3,
                category = OpportunityCategory.HEALTHCARE
            ),
            VolunteerOpportunity(
                id = 4,
                title = "Homeless Shelter Meal Service",
                organizationName = "Hope Kitchen",
                location = "Downtown Shelter",
                date = "2024-02-28",
                description = "Help prepare and serve meals to those in need",
                requiredVolunteers = 20,
                currentVolunteers = 14,
                category = OpportunityCategory.SOCIAL_SERVICES
            )
        )
    }
} 