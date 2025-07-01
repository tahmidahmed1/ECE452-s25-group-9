package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.services.LocationService
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.usecase.GetMapEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Simple contract interface for Map UI state */
interface MapUiContract {
    val currentLocation: Location?
    val filteredEvents: List<VolunteerEvent>
    val radiusKm: Float
}

data class MapUiState(
    override val currentLocation: Location? = null,
    val allEvents: List<VolunteerEvent> = emptyList(),
    override val filteredEvents: List<VolunteerEvent> = emptyList(),
    override val radiusKm: Float = 10f,
    val isLocationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : MapUiContract

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationService: LocationService,
    private val getMapEventsUseCase: GetMapEventsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun startLocationUpdates() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                locationService.getLocationUpdates()
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to get location: ${throwable.message}",
                            )
                        }
                    }
                    .collect { location ->
                        _uiState.update { currentState ->
                            val newState = currentState.copy(
                                currentLocation = location,
                                isLoading = false,
                                isLocationPermissionGranted = location != null,
                                errorMessage = if (location == null) "Location permission denied" else null,
                            )
                            filterEventsByRadius(newState)
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Location service error: ${e.message}",
                    )
                }
            }
        }
    }

    fun updateRadius(radiusKm: Float) {
        _uiState.update { currentState ->
            val newState = currentState.copy(radiusKm = radiusKm)
            filterEventsByRadius(newState)
        }
    }

    fun onLocationPermissionGranted() {
        _uiState.update { it.copy(isLocationPermissionGranted = true, errorMessage = null) }
        startLocationUpdates()
    }

    fun onLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                isLocationPermissionGranted = false,
                errorMessage = "Location permission is required to show nearby events",
            )
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            try {
                val events = getMapEventsUseCase()

                val finalEvents = if (events.isEmpty()) generateMockEvents() else events

                _uiState.update { currentState -> filterEventsByRadius(currentState.copy(allEvents = finalEvents)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to load events: ${e.message}")
                }
            }
        }
    }

    private fun filterEventsByRadius(state: MapUiState): MapUiState {
        // Reference point for distance calculations
        val referenceLocation = state.currentLocation ?: run {
            // Fallback: geometric center of all events (if any) so radius slider still works in demo
            if (state.allEvents.isEmpty()) return state.copy(filteredEvents = emptyList())

            val avgLat = state.allEvents.map { it.latitude }.average()
            val avgLng = state.allEvents.map { it.longitude }.average()
            android.location.Location("avg").apply {
                latitude = avgLat
                longitude = avgLng
            }
        }

        return try {
            var filteredEvents = state.allEvents.filter { event ->
                val distance = locationService.calculateDistance(
                    referenceLocation.latitude,
                    referenceLocation.longitude,
                    event.latitude,
                    event.longitude,
                )
                distance <= state.radiusKm
            }

            // If nothing matched (e.g., user GPS far away), keep showing all events
            if (filteredEvents.isEmpty()) filteredEvents = state.allEvents

            state.copy(filteredEvents = filteredEvents)
        } catch (e: Exception) {
            state.copy(
                filteredEvents = state.allEvents,
                errorMessage = "Error filtering events: ${e.message}",
            )
        }
    }

    /** Creates a small hard-coded set of events near downtown Toronto for demo purposes */
    private fun generateMockEvents(): List<VolunteerEvent> {
        val baseLat = 37.422131
        val baseLng = -122.084801

        return listOf(
            VolunteerEvent(
                id = 1,
                title = "Park Clean-up",
                description = "Help clean litter in the waterfront park.",
                organizationId = 100,
                organizationName = "Green Toronto",
                location = "Harbourfront Park",
                date = "2025-05-18",
                startTime = "09:00",
                endTime = "12:00",
                maxVolunteers = 25,
                currentVolunteers = 10,
                category = com.example.gooddeedfeed.domain.model.OpportunityCategory.ENVIRONMENTAL,
                requirements = listOf("Gloves provided"),
                status = com.example.gooddeedfeed.domain.model.EventStatus.PUBLISHED,
                createdAt = "2025-05-01",
                updatedAt = "2025-05-01",
                latitude = baseLat + 0.01,
                longitude = baseLng - 0.005,
            ),
            VolunteerEvent(
                id = 2,
                title = "Food Bank Sorting",
                description = "Sort and package food donations.",
                organizationId = 101,
                organizationName = "Toronto Food Bank",
                location = "Bathurst Warehouse",
                date = "2025-05-20",
                startTime = "13:00",
                endTime = "16:00",
                maxVolunteers = 15,
                currentVolunteers = 5,
                category = com.example.gooddeedfeed.domain.model.OpportunityCategory.SOCIAL_SERVICES,
                requirements = listOf("Closed-toe shoes"),
                status = com.example.gooddeedfeed.domain.model.EventStatus.PUBLISHED,
                createdAt = "2025-05-02",
                updatedAt = "2025-05-02",
                latitude = baseLat - 0.015,
                longitude = baseLng + 0.008,
            ),
            VolunteerEvent(
                id = 3,
                title = "Community Tutoring",
                description = "Tutor elementary students after school.",
                organizationId = 102,
                organizationName = "Learning Bridge",
                location = "Spadina Community Centre",
                date = "2025-05-22",
                startTime = "16:00",
                endTime = "18:00",
                maxVolunteers = 10,
                currentVolunteers = 2,
                category = com.example.gooddeedfeed.domain.model.OpportunityCategory.EDUCATION,
                requirements = emptyList(),
                status = com.example.gooddeedfeed.domain.model.EventStatus.PUBLISHED,
                createdAt = "2025-05-03",
                updatedAt = "2025-05-03",
                latitude = baseLat + 0.007,
                longitude = baseLng + 0.012,
            ),
        )
    }
} 
