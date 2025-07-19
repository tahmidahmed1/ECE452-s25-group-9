package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.data.remote.EventApiService
import com.example.gooddeedfeed.data.remote.dto.EventDto
import com.example.gooddeedfeed.domain.model.DateFilter
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.OpportunityFilters
import com.example.gooddeedfeed.domain.model.VolunteerApplicationForVolunteer
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.model.toApiValue
import com.example.gooddeedfeed.domain.repository.OpportunitiesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

private fun EventDto.toOpportunity(): VolunteerOpportunity = VolunteerOpportunity(
    id = id ?: 0,
    title = title,
    organizationName = organizer_name ?: "",
    location = location ?: "",
    date = date ?: "",
    description = description ?: "",
    requiredVolunteers = max_volunteers ?: 0,
    currentVolunteers = current_volunteers ?: 0,
    category = try {
        OpportunityCategory.valueOf(category.uppercase())
    } catch (e: Exception) {
        OpportunityCategory.OTHER
    },
    latitude = latitude ?: 0.0,
    longitude = longitude ?: 0.0,
    imageUrl = image_url,
)

@Singleton
class OpportunitiesRepositoryImpl @Inject constructor(
    private val apiService: EventApiService,
) : OpportunitiesRepository {

    private suspend fun fetchAll(): List<VolunteerOpportunity> {
        return try {
            apiService.getAllEvents().map { it.toOpportunity() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getOpportunities(): Flow<List<VolunteerOpportunity>> = flow {
        emit(fetchAll())
    }

    override suspend fun getOpportunitiesByCategory(category: OpportunityCategory): Flow<List<VolunteerOpportunity>> = flow {
        emit(fetchAll().filter { it.category == category })
    }

    override suspend fun searchOpportunities(query: String): Flow<List<VolunteerOpportunity>> = flow {
        val list = fetchAll().filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.organizationName.contains(query, ignoreCase = true)
        }
        emit(list)
    }

    override suspend fun getOpportunitiesNearLocation(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
    ): Flow<List<VolunteerOpportunity>> = flow {
        val list = fetchAll().filter {
            val dist = android.location.Location("event").apply {
                this.latitude = it.latitude
                this.longitude = it.longitude
            }.distanceTo(
                android.location.Location("user").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                },
            ) / 1000f
            dist <= radiusKm
        }
        emit(list)
    }

    override suspend fun applyForOpportunity(opportunityId: Int, message: String?): Result<Unit> {
        return Result.failure(Exception("Not implemented"))
    }

    override suspend fun getMyApplications(): Flow<List<VolunteerApplicationForVolunteer>> = flow {
        emit(emptyList())
    }

    override suspend fun cancelApplication(opportunityId: Int): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getOpportunityById(opportunityId: Int): Result<VolunteerOpportunity> {
        return runCatching {
            apiService.getEvent(opportunityId).toOpportunity()
        }
    }

    override suspend fun getCategories(): List<OpportunityCategory> {
        return OpportunityCategory.values().toList()
    }

    override suspend fun getOpportunitiesWithFilters(
        lat: Double?,
        lon: Double?,
        radiusKm: Float,
        filters: OpportunityFilters,
    ): Flow<List<VolunteerOpportunity>> = flow {
        val opportunities = try {
            // Convert filters to API parameters
            val categoryParam = if (filters.selectedCategories.isNotEmpty()) {
                filters.selectedCategories.first().toApiValue()
            } else {
                null
            }

            val dateFilterParam = when (filters.dateFilter) {
                DateFilter.ALL -> null
                DateFilter.TODAY -> "today"
                DateFilter.THIS_WEEK -> "this_week"
                DateFilter.THIS_MONTH -> "this_month"
            }

            apiService.getAllEvents(
                lat = lat,
                lon = lon,
                radiusKm = radiusKm,
                category = categoryParam,
                onlyAvailable = filters.onlyAvailable,
                almostFull = filters.almostFull,
                minKarmaPoints = filters.minKarmaPoints,
                maxKarmaPoints = filters.maxKarmaPoints,
                dateFilter = dateFilterParam,
            ).map { it.toOpportunity() }
        } catch (e: Exception) {
            emptyList()
        }
        emit(opportunities)
    }
} 
