package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import android.location.Location
import android.util.Log
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

                            // No longer re-fetch events on location updates
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

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            val locationEnabled = locationSettingsRepository.isLocationEnabled.first()
            if (locationEnabled) {
                _uiState.update { it.copy(isLocationPermissionGranted = true, errorMessage = null) }
                val location = locationService.getCurrentLocation()
                location?.let { loc ->
                    _uiState.update { currentState ->
                        currentState.copy(currentLocation = loc)
                    }
                }
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
                Log.d("MapViewModel", "Loaded events: ${events.size}")
                events.forEach { ev ->
                    Log.d("MapViewModel", "Event ${ev.id}: ${ev.title} (${ev.latitude},${ev.longitude})")
                }
                _uiState.update { currentState ->
                    currentState.copy(allEvents = events, filteredEvents = events)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to load events: ${e.message}")
                }
            }
        }
    }

    // Radius-based filtering removed – always show all events
    private fun filterEventsByRadius(state: MapUiState): MapUiState =
        state.copy(filteredEvents = state.allEvents)
} 
