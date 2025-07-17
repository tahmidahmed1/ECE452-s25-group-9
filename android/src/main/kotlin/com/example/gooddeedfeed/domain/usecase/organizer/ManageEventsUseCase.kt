package com.example.gooddeedfeed.domain.usecase.organizer

import com.example.gooddeedfeed.domain.model.CreateEventData
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing events (organizer functionality)
 */
class ManageEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend fun getMyEvents(): Flow<List<VolunteerEvent>> {
        return repository.getMyEvents()
    }

    suspend fun createEvent(eventData: CreateEventData): Result<VolunteerEvent> {
        return repository.createEvent(eventData)
    }

    suspend fun updateEvent(eventId: Int, eventData: CreateEventData): Result<VolunteerEvent> {
        return repository.updateEvent(eventId, eventData)
    }

    suspend fun deleteEvent(eventId: Int): Result<Unit> {
        return repository.deleteEvent(eventId)
    }

    suspend fun getEventById(eventId: Int): Result<VolunteerEvent> {
        return repository.getEventById(eventId)
    }

    suspend fun toggleEventStatus(eventId: Int, isPublished: Boolean): Result<Unit> {
        return repository.toggleEventStatus(eventId, isPublished)
    }

    suspend fun uploadEventImage(eventId: Int, file: java.io.File): Result<Unit> {
        return repository.uploadEventImage(eventId, file)
    }
} 
