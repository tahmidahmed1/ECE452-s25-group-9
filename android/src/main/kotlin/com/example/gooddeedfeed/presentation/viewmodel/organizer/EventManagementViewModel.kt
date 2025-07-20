package com.example.gooddeedfeed.presentation.viewmodel.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.CreateEventData
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.usecase.organizer.ManageEventsUseCase
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventManagementData(
    val events: List<VolunteerEvent>,
    val selectedEvent: VolunteerEvent? = null,
)

@HiltViewModel
class EventManagementViewModel @Inject constructor(
    private val manageEventsUseCase: ManageEventsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<EventManagementData>>(UiState.Loading)
    val uiState: StateFlow<UiState<EventManagementData>> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            manageEventsUseCase.getMyEvents()
                .catch { e ->
                    _uiState.value = UiState.Error("Failed to load events: ${e.message}")
                }
                .collect { events ->
                    _uiState.value = UiState.Success(
                        EventManagementData(events = events),
                    )
                }
        }
    }

    fun selectEvent(event: VolunteerEvent) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(selectedEvent = event),
            )
        }
    }

    fun deleteEvent(eventId: Int) {
        viewModelScope.launch {
            manageEventsUseCase.deleteEvent(eventId)
                .onSuccess {
                    loadEvents() // Refresh the list
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error("Failed to delete event: ${e.message}")
                }
        }
    }

    suspend fun createEvent(eventData: CreateEventData): VolunteerEvent {
        return manageEventsUseCase.createEvent(eventData)
            .onSuccess {
                loadEvents()
            }
            .onFailure { e ->
                _uiState.value = UiState.Error("Failed to create event: ${e.message}")
            }
            .getOrThrow()
    }

    fun updateEvent(eventId: Int, eventData: CreateEventData) {
        viewModelScope.launch {
            manageEventsUseCase.updateEvent(eventId, eventData)
                .onSuccess {
                    loadEvents() // Refresh the list
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error("Failed to update event: ${e.message}")
                }
        }
    }

    fun toggleEventStatus(eventId: Int, isPublished: Boolean) {
        viewModelScope.launch {
            manageEventsUseCase.toggleEventStatus(eventId, isPublished)
                .onSuccess {
                    loadEvents() // Refresh the list
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error("Failed to update event status: ${e.message}")
                }
        }
    }

    suspend fun uploadEventImage(eventId: Int, file: java.io.File) {
        manageEventsUseCase.uploadEventImage(eventId, file)
            .onSuccess { loadEvents() }
            .onFailure { e -> _uiState.value = UiState.Error("Failed to upload image: ${e.message}") }
    }

    suspend fun uploadEventImageToCarousel(eventId: Int, file: java.io.File, isMain: Boolean) {
        manageEventsUseCase.uploadEventImageToCarousel(eventId, file, isMain)
            .onSuccess { loadEvents() }
            .onFailure { e -> _uiState.value = UiState.Error("Failed to upload image: ${e.message}") }
    }

    fun setMainEventImage(eventId: Int, imageId: Int) {
        viewModelScope.launch {
            manageEventsUseCase.setMainEventImage(eventId, imageId)
                .onSuccess { loadEvents() }
                .onFailure { e -> _uiState.value = UiState.Error("Failed to set main image: ${e.message}") }
        }
    }
} 
