package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.repository.LocationSettingsRepository
import com.example.gooddeedfeed.data.services.LocationService
import com.example.gooddeedfeed.domain.model.DateFilter
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.OpportunityFilters
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
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
)

@HiltViewModel
class OpportunitiesViewModel @Inject constructor(
    private val getOpportunitiesUseCase: GetOpportunitiesUseCase,
    private val applyForOpportunityUseCase: ApplyForOpportunityUseCase,
    private val locationService: LocationService,
    val locationSettingsRepository: LocationSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<OpportunitiesData>>(UiState.Loading)
    val uiState: StateFlow<UiState<OpportunitiesData>> = _uiState.asStateFlow()

    private var allOpportunities: List<VolunteerOpportunity> = emptyList()

    init {
        loadOpportunities()
    }

    fun loadOpportunities() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            getOpportunitiesUseCase()
                .catch { e ->
                    _uiState.value = UiState.Error("Failed to load opportunities: ${e.message}")
                }
                .collect { opportunities ->
                    val categories = OpportunityCategory.values().toList()
                    allOpportunities = opportunities
                    _uiState.value = UiState.Success(
                        OpportunitiesData(
                            opportunities = opportunities,
                            categories = categories,
                            selectedCategory = null,
                            isMapView = false,
                            radiusKm = 10f,
                            currentLocation = null,
                            isLocationPermissionGranted = false,
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
                            allOpportunities = opportunities
                            _uiState.value = currentState.copy(
                                data = currentState.data.copy(
                                    opportunities = filterByRadius(opportunities, currentState.data.currentLocation, currentState.data.radiusKm),
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
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        allOpportunities = opportunities
                        _uiState.value = currentState.copy(
                            data = currentState.data.copy(
                                opportunities = filterByRadius(opportunities, currentState.data.currentLocation, currentState.data.radiusKm),
                                selectedCategory = null,
                            ),
                        )
                    }
                }
        }
    }

    fun joinOpportunity(opportunityId: Int) {
        viewModelScope.launch {
            applyForOpportunityUseCase(opportunityId)
                .onSuccess {
                    // Opportunity joined successfully - could show a success message
                    loadOpportunities() // Refresh to update status
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error("Failed to join opportunity: ${e.message}")
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

    private fun filterByRadius(opportunities: List<VolunteerOpportunity>, location: Location?, radiusKm: Float): List<VolunteerOpportunity> {
        if (location == null) return opportunities

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
            _uiState.value = currentState.copy(
                data = currentState.data.copy(
                    radiusKm = newRadius,
                    opportunities = filterByRadius(allOpportunities, currentState.data.currentLocation, newRadius),
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
            _uiState.value = UiState.Loading
            val currentState = _uiState.value
            if (currentState is UiState.Success) {
                val location = currentState.data.currentLocation
                val lat = location?.latitude
                val lon = location?.longitude
                val radiusKm = currentState.data.radiusKm

                try {
                    getOpportunitiesUseCase.getOpportunitiesWithFilters(lat, lon, radiusKm, filters)
                        .catch { e ->
                            _uiState.value = UiState.Error("Failed to apply filters: ${e.message}")
                        }
                        .collect { opportunities ->
                            _uiState.value = UiState.Success(
                                currentState.data.copy(
                                    opportunities = opportunities,
                                ),
                            )
                        }
                } catch (e: Exception) {
                    _uiState.value = UiState.Error("Failed to apply filters: ${e.message}")
                }
            }
        }
    }

    private fun applyOpportunityFilters(opportunities: List<VolunteerOpportunity>, filters: OpportunityFilters): List<VolunteerOpportunity> {
        return opportunities.filter { opportunity ->
            // Category filter
            val categoryMatch = filters.selectedCategories.isEmpty() || filters.selectedCategories.contains(opportunity.category)

            // Availability filter
            val availabilityMatch = when {
                filters.onlyAvailable -> opportunity.currentVolunteers < opportunity.requiredVolunteers
                filters.almostFull -> {
                    val percentage = opportunity.currentVolunteers.toFloat() / opportunity.requiredVolunteers.toFloat()
                    percentage >= 0.8f && percentage < 1.0f
                }
                else -> true
            }

            // Karma points filter
            val karmaMatch = opportunity.karmaPoints >= filters.minKarmaPoints && opportunity.karmaPoints <= filters.maxKarmaPoints

            // Date filter
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
            locationService.getLocationUpdates()
                .catch { /* ignore */ }
                .collect { loc ->
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        val filtered = filterByRadius(allOpportunities, loc, currentState.data.radiusKm)
                        _uiState.value = currentState.copy(
                            data = currentState.data.copy(
                                currentLocation = loc,
                                opportunities = filtered,
                            ),
                        )
                    }
                }
        }
    }
} 
