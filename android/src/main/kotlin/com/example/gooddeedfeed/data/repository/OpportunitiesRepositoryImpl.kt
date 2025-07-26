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
import com.example.gooddeedfeed.domain.model.hasPassed
import com.example.gooddeedfeed.domain.model.toApiValue
import com.example.gooddeedfeed.domain.repository.OpportunitiesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

private fun EventDto.toOpportunity(isJoined: Boolean = false): VolunteerOpportunity {
    // Debug logging for organization name mapping
    Log.d("OpportunitiesRepo", "🏢 DTO MAPPING - Event ID: ${id ?: 0}")
    Log.d("OpportunitiesRepo", "🏢 DTO MAPPING - Event Title: '$title'")
    Log.d("OpportunitiesRepo", "🏢 DTO MAPPING - Raw organizer_name: '$organizer_name'")
    Log.d("OpportunitiesRepo", "🏢 DTO MAPPING - organizer_name is null: ${organizer_name == null}")
    Log.d("OpportunitiesRepo", "🏢 DTO MAPPING - Final organizationName: '${organizer_name ?: ""}'")

    return VolunteerOpportunity(
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
            // Convert from backend format (e.g., "community_service") to enum
            when (category) {
                "community_service" -> OpportunityCategory.COMMUNITY_SERVICE
                "education" -> OpportunityCategory.EDUCATION
                "environmental" -> OpportunityCategory.ENVIRONMENTAL
                "healthcare" -> OpportunityCategory.HEALTHCARE
                "social_services" -> OpportunityCategory.SOCIAL_SERVICES
                "disaster_relief" -> OpportunityCategory.DISASTER_RELIEF
                "food_security" -> OpportunityCategory.FOOD_SECURITY
                "animal_welfare" -> OpportunityCategory.ANIMAL_WELFARE
                "arts_culture" -> OpportunityCategory.ARTS_CULTURE
                "youth_mentoring" -> OpportunityCategory.YOUTH_MENTORING
                "elderly_care" -> OpportunityCategory.ELDERLY_CARE
                "technology" -> OpportunityCategory.TECHNOLOGY
                else -> OpportunityCategory.OTHER
            }
        } catch (e: Exception) {
            OpportunityCategory.OTHER
        },
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        imageUrl = image_url?.toEmulatorAccessibleUrl(),
        karmaPoints = karma_points,
        isJoined = isJoined,
        images = images.map { it.copy(image_url = it.image_url.toEmulatorAccessibleUrl()) },
    )
}

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

            // Log all opportunities with their date/time data before filtering
            opportunities.forEach { opp ->
                Log.d("OpportunitiesRepo", "  📊 Event: '${opp.title}' | Date: ${opp.date} | Start: ${opp.startTime} | End: ${opp.endTime}")
            }

            val futureOpportunities = opportunities.filterNot { it.hasPassed() }
            Log.d("OpportunitiesRepo", "📞 Filtered to ${futureOpportunities.size} future opportunities (removed ${opportunities.size - futureOpportunities.size} past events)")

            // Log remaining opportunities after filtering
            futureOpportunities.forEach { opp ->
                Log.d("OpportunitiesRepo", "  ✅ Remaining: '${opp.title}' | Date: ${opp.date} | Start: ${opp.startTime} | End: ${opp.endTime}")
            }
            futureOpportunities
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
            Log.i("OpportunitiesRepo", "🎯 JOIN EVENT - Attempting to join event $eventId")

            // Join the event first
            apiService.joinEvent(eventId)
            Log.i("OpportunitiesRepo", "✅ JOIN EVENT - Successfully joined event $eventId")

            // Send notification to organizer about volunteer joining
            try {
                apiService.notifyOrganizerOfVolunteerJoin(eventId)
                Log.i("OpportunitiesRepo", "📢 JOIN EVENT - Notified organizer for event $eventId")
            } catch (e: Exception) {
                // Log but don't fail the join if notification fails
                Log.w("OpportunitiesRepo", "⚠️ JOIN EVENT - Failed to send notification to organizer for event $eventId", e)
            }

            // Trigger refresh of joined events
            val refreshTime = System.currentTimeMillis()
            _joinedEventsRefresh.value = refreshTime
            Log.i("OpportunitiesRepo", "🔄 JOIN EVENT - Triggered joined events refresh at $refreshTime")
            Unit
        }.onFailure { e ->
            Log.e("OpportunitiesRepo", "❌ JOIN EVENT - Failed to join event $eventId", e)
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
        _joinedEventsRefresh.flatMapLatest { refreshTime ->
            flow {
                try {
                    Log.i("OpportunitiesRepo", "📅 GET JOINED EVENTS - Fetching joined events (refresh time: $refreshTime)")
                    val joinedEventsDto = apiService.getMyJoinedEvents()
                    Log.i("OpportunitiesRepo", "📅 GET JOINED EVENTS - API returned ${joinedEventsDto.size} joined events")

                    joinedEventsDto.forEach { eventDto ->
                        Log.i("OpportunitiesRepo", "  📋 Joined Event: '${eventDto.title}' (ID: ${eventDto.id}) on ${eventDto.date} from ${eventDto.start_time} to ${eventDto.end_time}")
                    }

                    val joinedEvents = joinedEventsDto.map { it.toOpportunity(isJoined = true) }
                    Log.i("OpportunitiesRepo", "📅 GET JOINED EVENTS - Mapped to ${joinedEvents.size} volunteer opportunities")

                    joinedEvents.forEach { opportunity ->
                        Log.i("OpportunitiesRepo", "  ✅ Mapped Opportunity: '${opportunity.title}' (ID: ${opportunity.id}) - isJoined: ${opportunity.isJoined}")
                    }

                    emit(joinedEvents)
                } catch (e: Exception) {
                    Log.e("OpportunitiesRepo", "❌ GET JOINED EVENTS - Error getting joined events", e)
                    Log.e("OpportunitiesRepo", "❌ GET JOINED EVENTS - Exception details: ${e.message}")
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
            Log.d("OpportunitiesRepo", "  - Distance filtering enabled: ${filters.useDistanceFilter}")
            if (filters.useDistanceFilter) {
                Log.d("OpportunitiesRepo", "  - Location params: lat=$lat, lon=$lon, radiusKm=${radiusKm ?: 50f}")
            } else {
                Log.d("OpportunitiesRepo", "  - Location params: DISABLED (no lat/lon sent to API)")
            }

            val allOpportunities = apiService.getAllEvents(
                lat = if (filters.useDistanceFilter) lat else null,
                lon = if (filters.useDistanceFilter) lon else null,
                radiusKm = if (filters.useDistanceFilter) (radiusKm ?: 50f) else 50f,
                category = categoryParam,
                onlyAvailable = filters.onlyAvailable,
                almostFull = filters.almostFull,
                minKarmaPoints = filters.minKarmaPoints,
                maxKarmaPoints = filters.maxKarmaPoints,
                dateFilter = dateFilterParam,
            ).map { dto ->
                dto.toOpportunity().copy(isJoined = false)
            }

            // Log all opportunities with their date/time data before filtering
            allOpportunities.forEach { opp ->
                Log.d("OpportunitiesRepo", "  🎯 Event: '${opp.title}' | Date: ${opp.date} | Start: ${opp.startTime} | End: ${opp.endTime}")
            }

            // Filter out past events
            val result = allOpportunities.filterNot { it.hasPassed() }

            Log.d("OpportunitiesRepo", "🎯 API returned ${allOpportunities.size} opportunities")
            Log.d("OpportunitiesRepo", "🎯 Filtered to ${result.size} future opportunities (removed ${allOpportunities.size - result.size} past events)")

            // Log remaining opportunities after filtering
            result.forEach { opp ->
                Log.d("OpportunitiesRepo", "  🎯✅ Remaining: '${opp.title}' | Date: ${opp.date} | Start: ${opp.startTime} | End: ${opp.endTime}")
            }
            result
        } catch (e: Exception) {
            Log.e("OpportunitiesRepo", "❌ Error getting opportunities with filters", e)
            emptyList()
        }
        emit(opportunities)
    }

    override suspend fun generateOpportunityIdeas(): Result<List<String>> = runCatching {
        apiService.generateOpportunityIdeas()
    }
} 
