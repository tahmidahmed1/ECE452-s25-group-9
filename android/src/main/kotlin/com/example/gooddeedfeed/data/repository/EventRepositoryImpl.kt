package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.domain.model.CreateEventData
import com.example.gooddeedfeed.domain.model.EventStatus
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerApplicationForOrganizer
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor() : EventRepository {

    override suspend fun getMyEvents(): Flow<List<VolunteerEvent>> = flow {
        emit(getMockEvents())
    }

    override suspend fun createEvent(eventData: CreateEventData): Result<VolunteerEvent> {
        val newEvent = VolunteerEvent(
            id = (1..1000).random(),
            title = eventData.title,
            description = eventData.description,
            organizationId = 1,
            organizationName = "Mock Organization",
            location = eventData.location,
            date = eventData.date,
            startTime = eventData.startTime,
            endTime = eventData.endTime,
            maxVolunteers = eventData.maxVolunteers,
            currentVolunteers = 0,
            category = eventData.category,
            requirements = eventData.requirements,
            status = EventStatus.DRAFT,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
        )
        return Result.success(newEvent)
    }

    override suspend fun updateEvent(eventId: Int, eventData: CreateEventData): Result<VolunteerEvent> {
        val updatedEvent = VolunteerEvent(
            id = eventId,
            title = eventData.title,
            description = eventData.description,
            organizationId = 1,
            organizationName = "Mock Organization",
            location = eventData.location,
            date = eventData.date,
            startTime = eventData.startTime,
            endTime = eventData.endTime,
            maxVolunteers = eventData.maxVolunteers,
            currentVolunteers = 2,
            category = eventData.category,
            requirements = eventData.requirements,
            status = EventStatus.PUBLISHED,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-02T00:00:00Z",
        )
        return Result.success(updatedEvent)
    }

    override suspend fun deleteEvent(eventId: Int): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getEventById(eventId: Int): Result<VolunteerEvent> {
        val event = getMockEvents().find { it.id == eventId }
        return if (event != null) {
            Result.success(event)
        } else {
            Result.failure(Exception("Event not found"))
        }
    }

    override suspend fun toggleEventStatus(eventId: Int, isPublished: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getEventApplications(eventId: Int): Flow<List<VolunteerApplicationForOrganizer>> = flow {
        emit(emptyList())
    }

    private fun getMockEvents(): List<VolunteerEvent> {
        return listOf(
            VolunteerEvent(
                id = 1,
                title = "Community Garden Cleanup",
                description = "Help clean up and maintain our local community garden",
                organizationId = 1,
                organizationName = "Green Earth Initiative",
                location = "Downtown Community Garden",
                date = "2024-02-15",
                startTime = "09:00",
                endTime = "12:00",
                maxVolunteers = 20,
                currentVolunteers = 5,
                category = OpportunityCategory.ENVIRONMENTAL,
                requirements = listOf("Bring gloves", "Wear comfortable clothes"),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-01-15T10:00:00Z",
                updatedAt = "2024-01-15T10:00:00Z",
            ),
            VolunteerEvent(
                id = 2,
                title = "Food Bank Support",
                description = "Assist with food sorting and distribution",
                organizationId = 2,
                organizationName = "City Food Bank",
                location = "Main Street Food Bank",
                date = "2024-02-20",
                startTime = "14:00",
                endTime = "17:00",
                maxVolunteers = 15,
                currentVolunteers = 8,
                category = OpportunityCategory.SOCIAL_SERVICES,
                requirements = listOf("Must be 16+", "Food safety training provided"),
                status = EventStatus.PUBLISHED,
                createdAt = "2024-01-10T15:00:00Z",
                updatedAt = "2024-01-10T15:00:00Z",
            ),
        )
    }
} 
