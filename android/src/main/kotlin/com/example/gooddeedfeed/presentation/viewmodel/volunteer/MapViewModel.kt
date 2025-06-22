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
    private val locationService: LocationService,
    private val getMapEventsUseCase: GetMapEventsUseCase
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
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Location service error: ${e.message}"
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
                errorMessage = "Location permission is required to show nearby events"
            )
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            try {
                val events = getMapEventsUseCase()
                
                _uiState.update { currentState -> filterEventsByRadius(currentState.copy(allEvents = events)) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to load events: ${e.message}")
                }
            }
        }
    }

    private fun filterEventsByRadius(state: MapUiState): MapUiState {
        val currentLocation = state.currentLocation
        if (currentLocation == null) {
            return state.copy(filteredEvents = state.allEvents)
        }

        return try {
            val filteredEvents = state.allEvents.filter { event ->
                val distance = locationService.calculateDistance(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    event.latitude,
                    event.longitude
                )
                distance <= state.radiusKm
            }
            state.copy(filteredEvents = filteredEvents)
        } catch (e: Exception) {
            state.copy(
                filteredEvents = state.allEvents,
                errorMessage = "Error filtering events: ${e.message}"
            )
        }
    }
} 