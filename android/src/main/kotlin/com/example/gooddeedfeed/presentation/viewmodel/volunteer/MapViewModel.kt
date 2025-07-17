package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.repository.LocationSettingsRepository
import com.example.gooddeedfeed.data.services.LocationService
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.usecase.GetMapEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
    val locationSettingsRepository: LocationSettingsRepository,
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
                            val resState = filterEventsByRadius(newState)

                            // Refresh events from backend with current location
                            location?.let { loc ->
                                loadEvents(loc.latitude, loc.longitude)
                            }
                            resState
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
            val resState = filterEventsByRadius(newState)

            // Re-fetch events with updated radius and location
            val loc = resState.currentLocation
            loadEvents(loc?.latitude, loc?.longitude)

            resState
        }
    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            val locationEnabled = locationSettingsRepository.isLocationEnabled.first()
            if (locationEnabled) {
                _uiState.update { it.copy(isLocationPermissionGranted = true, errorMessage = null) }
                // First try to get last known location for immediate feedback
                getCurrentLocation()
                // Then start continuous location updates
                startLocationUpdates()
            } else {
                _uiState.update {
                    it.copy(
                        isLocationPermissionGranted = false,
                        errorMessage = "Location services are disabled in settings",
                    )
                }
            }
        }
    }

    private fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                val location = locationService.getCurrentLocation()
                location?.let { loc ->
                    _uiState.update { currentState ->
                        val newState = currentState.copy(
                            currentLocation = loc,
                            isLocationPermissionGranted = true,
                            errorMessage = null,
                        )
                        val resState = filterEventsByRadius(newState)

                        // Load events with current location
                        loadEvents(loc.latitude, loc.longitude)
                        resState
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to get current location: ${e.message}")
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                isLocationPermissionGranted = false,
                errorMessage = "Location permission is required to show nearby events",
            )
        }
    }

    private fun loadEvents(lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            try {
                val events = getMapEventsUseCase(lat, lon, _uiState.value.radiusKm)
                _uiState.update { currentState -> filterEventsByRadius(currentState.copy(allEvents = events)) }
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

    // Mock generation removed – relies solely on backend data now.
} 
