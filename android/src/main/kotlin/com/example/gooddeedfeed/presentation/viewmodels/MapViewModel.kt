package com.example.gooddeedfeed.presentation.viewmodels

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.services.LocationService
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.model.EventStatus
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val currentLocation: Location? = null,
    val allEvents: List<VolunteerEvent> = emptyList(),
    val filteredEvents: List<VolunteerEvent> = emptyList(),
    val radiusKm: Float = 10f,
    val isLocationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationService: LocationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadDummyEvents()
    }

    fun startLocationUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            locationService.getLocationUpdates()
                .catch { throwable ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to get location: ${throwable.message}"
                        )
                    }
                }
                .collect { location ->
                    _uiState.update { currentState ->
                        val newState = currentState.copy(
                            currentLocation = location,
                            isLoading = false,
                            isLocationPermissionGranted = location != null,
                            errorMessage = if (location == null) "Location permission denied" else null
                        )
                        filterEventsByRadius(newState)
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
        _uiState.update { it.copy(isLocationPermissionGranted = true) }
        startLocationUpdates()
    }

    fun onLocationPermissionDenied() {
        _uiState.update { 
            it.copy(
                isLocationPermissionGranted = false,
                errorMessage = "Location permission is required to show nearby events"
            )
        }
    }

    private fun filterEventsByRadius(state: MapUiState): MapUiState {
        val currentLocation = state.currentLocation
        if (currentLocation == null) {
            return state.copy(filteredEvents = state.allEvents)
        }

        val filteredEvents = state.allEvents.filter { event ->
            val distance = locationService.calculateDistance(
                currentLocation.latitude,
                currentLocation.longitude,
                event.latitude,
                event.longitude
            )
            distance <= state.radiusKm
        }

        return state.copy(filteredEvents = filteredEvents)
    }

    private fun loadDummyEvents() {
        // Using Toronto area coordinates for demo
        val dummyEvents = listOf(
            VolunteerEvent(
                id = 1,
                title = "Community Garden Cleanup",
                description = "Help clean and maintain our local community garden. Bring gloves and enthusiasm!",
                organizationId = 1,
                organizationName = "Green Toronto Initiative",
                location = "High Park Community Garden",
                date = "2024-12-28",
                startTime = "09:00",
                endTime = "12:00",
                maxVolunteers = 20,
                currentVolunteers = 5,
                category = OpportunityCategory.ENVIRONMENTAL,
                requirements = emptyList(),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-12-01",
                updatedAt = "2024-12-01",
                latitude = 43.6465, // High Park area
                longitude = -79.4636
            ),
            VolunteerEvent(
                id = 2,
                title = "Youth Coding Workshop",
                description = "Teach basic programming concepts to underprivileged youth in our community.",
                organizationId = 2,
                organizationName = "Code for Good",
                location = "Ryerson University",
                date = "2024-12-29",
                startTime = "14:00",
                endTime = "17:00",
                maxVolunteers = 12,
                currentVolunteers = 4,
                category = OpportunityCategory.EDUCATION,
                requirements = emptyList(),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-12-01",
                updatedAt = "2024-12-01",
                latitude = 43.6577, // Ryerson University
                longitude = -79.3788
            ),
            VolunteerEvent(
                id = 3,
                title = "Homeless Shelter Meal Prep",
                description = "Prepare and serve meals for homeless individuals at our downtown shelter.",
                organizationId = 3,
                organizationName = "Toronto Shelter Network",
                location = "Downtown Toronto Shelter",
                date = "2024-12-30",
                startTime = "11:00",
                endTime = "15:00",
                maxVolunteers = 15,
                currentVolunteers = 3,
                category = OpportunityCategory.SOCIAL_SERVICES,
                requirements = emptyList(),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-12-01",
                updatedAt = "2024-12-01",
                latitude = 43.6511, // Downtown Toronto
                longitude = -79.3470
            ),
            VolunteerEvent(
                id = 4,
                title = "Beach Cleanup Drive",
                description = "Join us for a morning beach cleanup to protect our waterfront environment.",
                organizationId = 4,
                organizationName = "Lake Ontario Guardians",
                location = "Woodbine Beach",
                date = "2024-12-31",
                startTime = "08:00",
                endTime = "11:00",
                maxVolunteers = 30,
                currentVolunteers = 5,
                category = OpportunityCategory.ENVIRONMENTAL,
                requirements = emptyList(),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-12-01",
                updatedAt = "2024-12-01",
                latitude = 43.6630, // Woodbine Beach
                longitude = -79.3052
            ),
            VolunteerEvent(
                id = 5,
                title = "Senior Center Activities",
                description = "Spend time with seniors, play games, and provide companionship.",
                organizationId = 5,
                organizationName = "Elder Care Toronto",
                location = "Scarborough Senior Center",
                date = "2025-01-02",
                startTime = "13:00",
                endTime = "16:00",
                maxVolunteers = 10,
                currentVolunteers = 4,
                category = OpportunityCategory.COMMUNITY_SERVICE,
                requirements = emptyList(),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-12-01",
                updatedAt = "2024-12-01",
                latitude = 43.7735, // Scarborough
                longitude = -79.2577
            ),
            VolunteerEvent(
                id = 6,
                title = "Library Reading Program",
                description = "Read stories to children and help with literacy activities at the local library.",
                organizationId = 6,
                organizationName = "Toronto Public Library",
                location = "North York Central Library",
                date = "2025-01-03",
                startTime = "10:00",
                endTime = "12:00",
                maxVolunteers = 8,
                currentVolunteers = 4,
                category = OpportunityCategory.EDUCATION,
                requirements = emptyList(),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-12-01",
                updatedAt = "2024-12-01",
                latitude = 43.7615, // North York
                longitude = -79.4111
            )
        )

        _uiState.update { currentState ->
            val newState = currentState.copy(allEvents = dummyEvents)
            filterEventsByRadius(newState)
        }
    }
} 