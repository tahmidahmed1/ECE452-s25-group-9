package com.example.gooddeedfeed.data.repository

import android.util.Log
import com.example.gooddeedfeed.data.mapper.toEmulatorAccessibleUrl
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

private fun EventDto.toOpportunity(isJoined: Boolean = false): VolunteerOpportunity = VolunteerOpportunity(
    id = id ?: 0,
    title = title,
    organizationName = organizer_name ?: "",
    location = location ?: "",
    date = date ?: "",
    startTime = start_time,
    endTime = end_time,
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
    imageUrl = image_url?.toEmulatorAccessibleUrl(),
    karmaPoints = karma_points,
    isJoined = isJoined,
)

@Singleton
class OpportunitiesRepositoryImpl @Inject constructor(
    private val apiService: EventApiService,
) : OpportunitiesRepository {

    private val _joinedEventsRefresh = MutableStateFlow(System.currentTimeMillis())

    private suspend fun fetchAll(): List<VolunteerOpportunity> {
        return try {
            Log.d("OpportunitiesRepo", "📞 Calling apiService.getAllEvents()...")
            val events = apiService.getAllEvents()
            Log.d("OpportunitiesRepo", "📞 API returned ${events.size} events")
            val opportunities = events.map { it.toOpportunity() }
            Log.d("OpportunitiesRepo", "📞 Mapped to ${opportunities.size} opportunities")
            opportunities
        } catch (e: Exception) {
            Log.e("OpportunitiesRepo", "❌ Error in fetchAll()", e)
            emptyList()
        }
    }

    override suspend fun getOpportunities(): Flow<List<VolunteerOpportunity>> = flow {
        Log.d("OpportunitiesRepo", "📥 getOpportunities() called")
        val opportunities = fetchAll()
        Log.d("OpportunitiesRepo", "📥 getOpportunities() - fetchAll() returned ${opportunities.size} opportunities")
        opportunities.forEach { opp ->
            Log.d("OpportunitiesRepo", "  - ${opp.title}: karmaPoints=${opp.karmaPoints}")
        }
        emit(opportunities)
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

    override suspend fun joinEvent(eventId: Int): Result<Unit> {
        return runCatching {
            apiService.joinEvent(eventId)
            // Trigger refresh of joined events
            _joinedEventsRefresh.value = System.currentTimeMillis()
            Unit
        }
    }

    override suspend fun leaveEvent(eventId: Int): Result<Unit> {
        return runCatching {
            apiService.leaveEvent(eventId)
            // Trigger refresh of joined events
            _joinedEventsRefresh.value = System.currentTimeMillis()
            Unit
        }
    }

    override suspend fun getJoinedEvents(): Flow<List<VolunteerOpportunity>> = 
        _joinedEventsRefresh.flatMapLatest {
            flow {
                try {
                    val joinedEvents = apiService.getMyJoinedEvents().map { it.toOpportunity(isJoined = true) }
                    emit(joinedEvents)
                } catch (e: Exception) {
                    Log.e("OpportunitiesRepo", "❌ Error getting joined events", e)
                    emit(emptyList())
                }
            }
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
        radiusKm: Float?,
        filters: OpportunityFilters,
    ): Flow<List<VolunteerOpportunity>> = flow {
        Log.d("OpportunitiesRepo", "🎯 getOpportunitiesWithFilters called")
        Log.d("OpportunitiesRepo", "  - lat: $lat, lon: $lon, radiusKm: $radiusKm")
        Log.d("OpportunitiesRepo", "  - filters: $filters")

        val opportunities = try {
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

            Log.d("OpportunitiesRepo", "  - API params: category=$categoryParam, onlyAvailable=${filters.onlyAvailable}, almostFull=${filters.almostFull}")
            Log.d("OpportunitiesRepo", "  - API params: minKarmaPoints=${filters.minKarmaPoints}, maxKarmaPoints=${filters.maxKarmaPoints}, dateFilter=$dateFilterParam")

            val result = apiService.getAllEvents(
                lat = lat,
                lon = lon,
                radiusKm = radiusKm ?: 50f,
                category = categoryParam,
                onlyAvailable = filters.onlyAvailable,
                almostFull = filters.almostFull,
                minKarmaPoints = filters.minKarmaPoints,
                maxKarmaPoints = filters.maxKarmaPoints,
                dateFilter = dateFilterParam,
            ).map { it.toOpportunity() }

            Log.d("OpportunitiesRepo", "🎯 API returned ${result.size} opportunities")
            result.forEach { opp ->
                Log.d("OpportunitiesRepo", "  - ${opp.title}: karmaPoints=${opp.karmaPoints}, available=${opp.requiredVolunteers - opp.currentVolunteers}")
            }
            result
        } catch (e: Exception) {
            Log.e("OpportunitiesRepo", "❌ Error getting opportunities with filters", e)
            emptyList()
        }
        emit(opportunities)
    }
} 
