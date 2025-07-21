package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.data.remote.EventApiService
import com.example.gooddeedfeed.data.remote.dto.toDomain
import com.example.gooddeedfeed.domain.model.CreateEventData
import com.example.gooddeedfeed.domain.model.VolunteerApplicationForOrganizer
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.repository.AuthRepository
import com.example.gooddeedfeed.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val apiService: EventApiService,
    private val authRepository: AuthRepository,
) : EventRepository {

    override suspend fun getMyEvents(): Flow<List<VolunteerEvent>> = flow {
        val currentUserResult = authRepository.getCurrentUser()
        val userId = currentUserResult.getOrNull()?.id
        val dtos = if (userId != null) {
            apiService.getOrganizerEvents(userId)
        } else {
            emptyList()
        }
        emit(dtos.map { it.toDomain() })
    }

    override suspend fun createEvent(eventData: CreateEventData): Result<VolunteerEvent> = runCatching {
        apiService.createEvent(eventData).toDomain()
    }

    override suspend fun updateEvent(eventId: Int, eventData: CreateEventData): Result<VolunteerEvent> = runCatching {
        apiService.updateEvent(eventId, eventData).toDomain()
    }

    override suspend fun deleteEvent(eventId: Int): Result<Unit> = runCatching {
        apiService.deleteEvent(eventId)
    }

    override suspend fun getEventById(eventId: Int): Result<VolunteerEvent> = runCatching {
        apiService.getEvent(eventId).toDomain()
    }

    override suspend fun toggleEventStatus(eventId: Int, isPublished: Boolean): Result<Unit> {
        // For simplicity, call updateEvent with status change
        // This assumes backend will accept status field but CreateEventData lacks status; skipping.
        return Result.failure(Exception("Not implemented"))
    }

    override suspend fun getEventApplications(eventId: Int): Flow<List<VolunteerApplicationForOrganizer>> = flow {
        emit(emptyList()) // Not yet implemented
    }

    override suspend fun uploadEventImage(eventId: Int, file: java.io.File): Result<Unit> = runCatching {
        apiService.uploadEventImage(eventId, file)
    }

    override suspend fun uploadEventImageToCarousel(eventId: Int, file: java.io.File, isMain: Boolean): Result<Unit> = runCatching {
        apiService.uploadEventImageToCarousel(eventId, file, isMain)
    }

    override suspend fun setMainEventImage(eventId: Int, imageId: Int): Result<Unit> = runCatching {
        apiService.setMainEventImage(eventId, imageId)
    }
} 
