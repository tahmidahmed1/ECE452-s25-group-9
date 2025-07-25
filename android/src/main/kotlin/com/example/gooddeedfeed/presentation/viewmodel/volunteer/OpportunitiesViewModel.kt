package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.repository.LocationSettingsRepository
import com.example.gooddeedfeed.data.services.LocationService
import com.example.gooddeedfeed.domain.model.DateFilter
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.OpportunityFilters
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.repository.OpportunitiesRepository
import com.example.gooddeedfeed.domain.usecase.volunteer.ApplyForOpportunityUseCase
import com.example.gooddeedfeed.domain.usecase.volunteer.GetOpportunitiesUseCase
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class OpportunitiesData(
    val opportunities: List<VolunteerOpportunity>,
    val categories: List<OpportunityCategory>,
    val selectedCategory: OpportunityCategory?,
    val isMapView: Boolean,
    val radiusKm: Float = 10f,
    val currentLocation: Location? = null,
    val isLocationPermissionGranted: Boolean = false,
    val useDistanceFilter: Boolean = false,
)

@HiltViewModel
class OpportunitiesViewModel @Inject constructor(
    private val getOpportunitiesUseCase: GetOpportunitiesUseCase,
    private val applyForOpportunityUseCase: ApplyForOpportunityUseCase,
    private val opportunitiesRepository: OpportunitiesRepository,
    private val locationService: LocationService,
    val locationSettingsRepository: LocationSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<OpportunitiesData>>(UiState.Loading)
    val uiState: StateFlow<UiState<OpportunitiesData>> = _uiState.asStateFlow()

    private var allOpportunities: List<VolunteerOpportunity> = emptyList()

    private var joinedIds: Set<Int> = emptySet()
    
    // Track optimistic updates to prevent race conditions
    private var optimisticJoinedIds: Set<Int> = emptySet()
    private var optimisticLeftIds: Set<Int> = emptySet()

    init {
        Log.d("OpportunitiesViewModel", "🚀 ViewModel initialized, loading opportunities...")
        loadOpportunities()
        // collect joined events continuously
        viewModelScope.launch {
            opportunitiesRepository.getJoinedEvents().collect { joinedList ->
                val serverJoinedIds = joinedList.map { it.id }.toSet()
                
                // Clear optimistic updates that are now confirmed by server
                optimisticJoinedIds = optimisticJoinedIds.filter { id ->
                    !serverJoinedIds.contains(id)
                }.toSet()
                
                optimisticLeftIds = optimisticLeftIds.filter { id ->
                    serverJoinedIds.contains(id)
                }.toSet()
                
                joinedIds = serverJoinedIds
                refreshOpportunitiesState()
            }
        }
    }

    fun loadOpportunities() {
        viewModelScope.launch {
            Log.d("OpportunitiesViewModel", "📥 Loading opportunities...")
            _uiState.value = UiState.Loading
            getOpportunitiesUseCase()
                .catch { e ->
                    Log.e("OpportunitiesViewModel", "❌ Failed to load opportunities: ${e.message}", e)
                    _uiState.value = UiState.Error("Failed to load opportunities: ${e.message}")
                }
                .collect { opportunities ->
                    Log.d("OpportunitiesViewModel", "✅ Loaded ${opportunities.size} opportunities")
                    val categories = OpportunityCategory.values().toList()
                    // Merge joined flag into opportunities
                    val mergedOps = mergeJoinedFlag(opportunities)
                    allOpportunities = mergedOps
                    _uiState.value = UiState.Success(
                        OpportunitiesData(
                            opportunities = mergedOps,
                            categories = categories,
                            selectedCategory = null,
                            isMapView = false,
                            radiusKm = 10f,
                            currentLocation = null,
                            isLocationPermissionGranted = false,
                            useDistanceFilter = false,
                        ),
                    )
                }
        }
    }

    fun filterByCategory(category: OpportunityCategory?) {
        viewModelScope.launch {
            if (category == null) {
                loadOpportunities()
            } else {
                getOpportunitiesUseCase.getByCategory(category)
                    .catch { e ->
                        _uiState.value = UiState.Error("Failed to filter opportunities: ${e.message}")
                    }
                    .collect { opportunities ->
                        val currentState = _uiState.value
                        if (currentState is UiState.Success) {
                            // Merge joined flag
                            val mergedOps = mergeJoinedFlag(opportunities)
                            allOpportunities = mergedOps
                            _uiState.value = currentState.copy(
                                data = currentState.data.copy(
                                    opportunities = filterByRadius(mergedOps, currentState.data.currentLocation, currentState.data.radiusKm, currentState.data.useDistanceFilter),
                                    selectedCategory = category,
                                ),
                            )
                        }
                    }
            }
        }
    }

    fun searchOpportunities(query: String) {
        viewModelScope.launch {
            getOpportunitiesUseCase.search(query)
                .catch { e ->
                    _uiState.value = UiState.Error("Failed to search opportunities: ${e.message}")
                }
                .collect { opportunities ->
                    val merged = mergeJoinedFlag(opportunities)
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        allOpportunities = merged
                        _uiState.value = currentState.copy(
                            data = currentState.data.copy(
                                opportunities = filterByRadius(merged, currentState.data.currentLocation, currentState.data.radiusKm, currentState.data.useDistanceFilter),
                                selectedCategory = null,
                            ),
                        )
                    }
                }
        }
    }

    fun joinOpportunity(opportunityId: Int) {
        viewModelScope.launch {
            // Immediately update optimistic state
            optimisticJoinedIds = optimisticJoinedIds + opportunityId
            optimisticLeftIds = optimisticLeftIds - opportunityId
            refreshOpportunitiesState()
            
            opportunitiesRepository.joinEvent(opportunityId)
                .onSuccess {
                    com.example.gooddeedfeed.presentation.ui.components.ToastManager.showSuccess("Joined event successfully!")
                    // Update volunteer count optimistically
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        val updated = currentState.data.opportunities.map { opp ->
                            if (opp.id == opportunityId) opp.copy(currentVolunteers = opp.currentVolunteers + 1)
                            else opp
                        }
                        allOpportunities = updated
                        _uiState.value = currentState.copy(
                            data = currentState.data.copy(opportunities = updated)
                        )
                    }
                }
                .onFailure { e ->
                    // Revert optimistic update on failure
                    optimisticJoinedIds = optimisticJoinedIds - opportunityId
                    refreshOpportunitiesState()
                    _uiState.value = UiState.Error("Failed to join opportunity: ${e.message}")
                }
        }
    }

    fun leaveOpportunity(opportunityId: Int) {
        viewModelScope.launch {
            // Immediately update optimistic state
            optimisticLeftIds = optimisticLeftIds + opportunityId
            optimisticJoinedIds = optimisticJoinedIds - opportunityId
            refreshOpportunitiesState()
            
            opportunitiesRepository.leaveEvent(opportunityId)
                .onSuccess {
                    com.example.gooddeedfeed.presentation.ui.components.ToastManager.showSuccess("Left event")
                    // Update volunteer count optimistically
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        val updated = currentState.data.opportunities.map { opp ->
                            if (opp.id == opportunityId) opp.copy(currentVolunteers = opp.currentVolunteers - 1)
                            else opp
                        }
                        allOpportunities = updated
                        _uiState.value = currentState.copy(
                            data = currentState.data.copy(opportunities = updated)
                        )
                    }
                }
                .onFailure { e ->
                    // Revert optimistic update on failure
                    optimisticLeftIds = optimisticLeftIds - opportunityId
                    refreshOpportunitiesState()
                    _uiState.value = UiState.Error("Failed to leave opportunity: ${e.message}")
                }
        }
    }

    fun toggleMapView() {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(isMapView = !currentState.data.isMapView),
            )
        }
    }

    private fun filterByRadius(opportunities: List<VolunteerOpportunity>, location: Location?, radiusKm: Float, useDistanceFilter: Boolean = true): List<VolunteerOpportunity> {
        if (location == null || !useDistanceFilter) return opportunities

        return opportunities.filter { opp ->
            if (opp.latitude == 0.0 && opp.longitude == 0.0) {
                true
            } else {
                val distance = locationService.calculateDistance(location.latitude, location.longitude, opp.latitude, opp.longitude)
                distance <= radiusKm
            }
        }
    }

    fun updateRadius(newRadius: Float) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            val newOpportunities = if (currentState.data.useDistanceFilter) {
                filterByRadius(allOpportunities, currentState.data.currentLocation, newRadius, true)
            } else {
                // Keep current opportunities unchanged when distance filtering is disabled
                currentState.data.opportunities
            }
            _uiState.value = currentState.copy(
                data = currentState.data.copy(
                    radiusKm = newRadius,
                    opportunities = newOpportunities,
                ),
            )
        }
    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            val locationEnabled = locationSettingsRepository.isLocationEnabled.first()
            val currentState = _uiState.value
            if (currentState is UiState.Success) {
                if (locationEnabled) {
                    _uiState.value = currentState.copy(
                        data = currentState.data.copy(isLocationPermissionGranted = true),
                    )
                    startLocationUpdates()
                } else {
                    _uiState.value = currentState.copy(
                        data = currentState.data.copy(isLocationPermissionGranted = false),
                    )
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(isLocationPermissionGranted = false),
            )
        }
    }

    fun applyFilters(filters: OpportunityFilters) {
        viewModelScope.launch {
            Log.d("OpportunitiesViewModel", "🎯 Applying filters: $filters")
            val currentState = _uiState.value
            Log.d("OpportunitiesViewModel", "Current state: ${currentState.javaClass.simpleName}")

            if (currentState is UiState.Success) {
                Log.d("OpportunitiesViewModel", "Setting state to Loading...")
                _uiState.value = UiState.Loading
                val location = currentState.data.currentLocation
                val lat = location?.latitude
                val lon = location?.longitude
                val radiusKm = currentState.data.radiusKm

                Log.d("OpportunitiesViewModel", "Location: lat=$lat, lon=$lon, radiusKm=$radiusKm")
                try {
                    val effectiveRadiusKm = if (filters.useDistanceFilter) radiusKm else null
                    Log.d("OpportunitiesViewModel", "Effective radius: $effectiveRadiusKm")
                    getOpportunitiesUseCase.getOpportunitiesWithFilters(lat, lon, effectiveRadiusKm, filters)
                        .catch { e ->
                            Log.e("OpportunitiesViewModel", "❌ Filter error: ${e.message}", e)
                            _uiState.value = UiState.Error("Failed to apply filters: ${e.message}")
                        }
                        .collect { opportunities ->
                            Log.d("OpportunitiesViewModel", "✅ Filtered to ${opportunities.size} opportunities")
                            // Merge joined flag into filtered opportunities
                            val mergedOps = mergeJoinedFlag(opportunities)
                            _uiState.value = UiState.Success(
                                currentState.data.copy(
                                    opportunities = mergedOps,
                                ),
                            )
                        }
                } catch (e: Exception) {
                    Log.e("OpportunitiesViewModel", "❌ Filter exception: ${e.message}", e)
                    _uiState.value = UiState.Error("Failed to apply filters: ${e.message}")
                }
            } else {
                Log.w("OpportunitiesViewModel", "⚠️ Cannot apply filters - current state is not Success: $currentState")
            }
        }
    }

    private fun applyOpportunityFilters(opportunities: List<VolunteerOpportunity>, filters: OpportunityFilters): List<VolunteerOpportunity> {
        return opportunities.filter { opportunity ->
            val categoryMatch = filters.selectedCategories.isEmpty() || filters.selectedCategories.contains(opportunity.category)

            val availabilityMatch = when {
                filters.onlyAvailable -> opportunity.requiredVolunteers == 0 || opportunity.currentVolunteers < opportunity.requiredVolunteers
                filters.almostFull -> {
                    opportunity.requiredVolunteers != 0 && run {
                        val percentage = opportunity.currentVolunteers.toFloat() / opportunity.requiredVolunteers.toFloat()
                        percentage >= 0.8f && percentage < 1.0f
                    }
                }
                else -> true
            }

            val karmaMatch = opportunity.karmaPoints >= filters.minKarmaPoints && opportunity.karmaPoints <= filters.maxKarmaPoints

            val dateMatch = when (filters.dateFilter) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> isToday(opportunity.date)
                DateFilter.THIS_WEEK -> isThisWeek(opportunity.date)
                DateFilter.THIS_MONTH -> isThisMonth(opportunity.date)
            }

            categoryMatch && availabilityMatch && karmaMatch && dateMatch
        }
    }

    private fun isToday(dateString: String): Boolean {
        return try {
            val opportunityDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            opportunityDate.isEqual(LocalDate.now())
        } catch (e: Exception) {
            false
        }
    }

    private fun isThisWeek(dateString: String): Boolean {
        return try {
            val opportunityDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            val startOfWeek = today.minusDays(today.dayOfWeek.value - 1L)
            val endOfWeek = startOfWeek.plusDays(6)
            opportunityDate.isAfter(startOfWeek.minusDays(1)) && opportunityDate.isBefore(endOfWeek.plusDays(1))
        } catch (e: Exception) {
            false
        }
    }

    private fun isThisMonth(dateString: String): Boolean {
        return try {
            val opportunityDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            opportunityDate.month == today.month && opportunityDate.year == today.year
        } catch (e: Exception) {
            false
        }
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            Log.d("OpportunitiesViewModel", "🌍 Starting location updates...")
            locationService.getLocationUpdates()
                .catch { /* ignore */ }
                .collect { loc ->
                    Log.d("OpportunitiesViewModel", "🌍 Location update received: lat=${loc?.latitude}, lon=${loc?.longitude}")
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        val useDistanceFilter = currentState.data.useDistanceFilter
                        Log.d("OpportunitiesViewModel", "🌍 Distance filtering enabled: $useDistanceFilter")

                        // Only update opportunities if distance filtering is enabled
                        val newOpportunities = if (useDistanceFilter) {
                            Log.d("OpportunitiesViewModel", "🌍 Filtering ${allOpportunities.size} opportunities by radius ${currentState.data.radiusKm}km")
                            val result = filterByRadius(allOpportunities, loc, currentState.data.radiusKm, true)
                            Log.d("OpportunitiesViewModel", "🌍 Location filtering resulted in ${result.size} opportunities")
                            result
                        } else {
                            Log.d("OpportunitiesViewModel", "🌍 Distance filtering disabled, keeping current opportunities unchanged")
                            // Keep the current opportunities list unchanged when distance filtering is disabled
                            currentState.data.opportunities
                        }

                        // Only update the state if something actually changed
                        val newLocation = loc
                        val currentLocation = currentState.data.currentLocation
                        val opportunitiesChanged = newOpportunities != currentState.data.opportunities
                        val locationChanged = newLocation != currentLocation

                        if (opportunitiesChanged || locationChanged) {
                            _uiState.value = currentState.copy(
                                data = currentState.data.copy(
                                    currentLocation = newLocation,
                                    opportunities = newOpportunities,
                                ),
                            )
                        } else {
                            Log.d("OpportunitiesViewModel", "🌍 No changes needed, skipping UI update")
                        }
                    }
                }
        }
    }

    private fun mergeJoinedFlag(list: List<VolunteerOpportunity>): List<VolunteerOpportunity> {
        return list.map { opportunity ->
            val isJoined = when {
                // Optimistic updates take priority
                optimisticJoinedIds.contains(opportunity.id) -> true
                optimisticLeftIds.contains(opportunity.id) -> false
                // Fall back to server state
                else -> joinedIds.contains(opportunity.id)
            }
            opportunity.copy(isJoined = isJoined)
        }
    }

    private fun refreshOpportunitiesState() {
        val current = _uiState.value
        if (current is UiState.Success) {
            _uiState.value = current.copy(
                data = current.data.copy(
                    opportunities = mergeJoinedFlag(current.data.opportunities),
                ),
            )
        }
    }
} 
