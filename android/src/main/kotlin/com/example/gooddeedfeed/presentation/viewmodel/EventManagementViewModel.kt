package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.model.EventStatus
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EventManagementUiState {
    object Loading : EventManagementUiState()
    data class Success(
        val events: List<VolunteerEvent>,
        val selectedEvent: VolunteerEvent? = null
    ) : EventManagementUiState()
    data class Error(val message: String) : EventManagementUiState()
}

@HiltViewModel
class EventManagementViewModel @Inject constructor(
    // TODO: Inject EventRepository when created
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<EventManagementUiState>(EventManagementUiState.Loading)
    val uiState: StateFlow<EventManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadEvents()
    }
    
    fun loadEvents() {
        viewModelScope.launch {
            try {
                _uiState.value = EventManagementUiState.Loading
                
                // TODO: Replace with actual repository call
                val mockEvents = getMockEvents()
                
                _uiState.value = EventManagementUiState.Success(
                    events = mockEvents
                )
            } catch (e: Exception) {
                _uiState.value = EventManagementUiState.Error("Failed to load events")
            }
        }
    }
    
    fun createEvent(eventData: CreateEventData) {
        viewModelScope.launch {
            try {
                // TODO: Implement with repository
                // eventRepository.createEvent(eventData)
                loadEvents() // Refresh after creation
            } catch (e: Exception) {
                _uiState.value = EventManagementUiState.Error("Failed to create event")
            }
        }
    }
    
    fun updateEvent(eventId: Int, eventData: CreateEventData) {
        viewModelScope.launch {
            try {
                // TODO: Implement with repository
                // eventRepository.updateEvent(eventId, eventData)
                loadEvents() // Refresh after update
            } catch (e: Exception) {
                _uiState.value = EventManagementUiState.Error("Failed to update event")
            }
        }
    }
    
    fun deleteEvent(eventId: Int) {
        viewModelScope.launch {
            try {
                // TODO: Implement with repository
                // eventRepository.deleteEvent(eventId)
                
                // Remove from current state immediately for better UX
                val currentState = _uiState.value
                if (currentState is EventManagementUiState.Success) {
                    val updatedEvents = currentState.events.filter { it.id != eventId }
                    _uiState.value = currentState.copy(events = updatedEvents)
                }
            } catch (e: Exception) {
                _uiState.value = EventManagementUiState.Error("Failed to delete event")
                loadEvents() // Reload to restore state on error
            }
        }
    }
    
    fun selectEvent(event: VolunteerEvent?) {
        val currentState = _uiState.value
        if (currentState is EventManagementUiState.Success) {
            _uiState.value = currentState.copy(selectedEvent = event)
        }
    }
    
    // TODO: Replace with actual repository data
    private fun getMockEvents(): List<VolunteerEvent> {
        return listOf(
            VolunteerEvent(
                id = 1,
                title = "Community Cleanup Drive",
                description = "Help clean up the local park and surrounding areas",
                organizationId = 1,
                organizationName = "Green City Initiative",
                location = "Central Park",
                date = "March 20, 2024",
                startTime = "09:00",
                endTime = "15:00",
                maxVolunteers = 20,
                currentVolunteers = 12,
                category = OpportunityCategory.ENVIRONMENTAL,
                requirements = listOf("Comfortable clothing", "Water bottle"),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-03-01T10:00:00Z",
                updatedAt = "2024-03-01T10:00:00Z"
            ),
            VolunteerEvent(
                id = 2,
                title = "Food Distribution",
                description = "Distribute food packages to families in need",
                organizationId = 1,
                organizationName = "City Food Bank",
                location = "Community Center",
                date = "March 25, 2024",
                startTime = "10:00",
                endTime = "14:00",
                maxVolunteers = 15,
                currentVolunteers = 8,
                category = OpportunityCategory.SOCIAL_SERVICES,
                requirements = listOf("Background check required"),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-03-02T10:00:00Z",
                updatedAt = "2024-03-02T10:00:00Z"
            )
        )
    }
}

data class CreateEventData(
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val maxVolunteers: Int,
    val category: OpportunityCategory,
    val requirements: List<String>
) 