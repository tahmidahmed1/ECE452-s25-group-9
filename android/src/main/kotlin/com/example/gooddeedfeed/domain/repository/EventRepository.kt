package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.AttendanceSubmission
import com.example.gooddeedfeed.domain.model.CreateEventData
import com.example.gooddeedfeed.domain.model.EventVolunteer
import com.example.gooddeedfeed.domain.model.VolunteerApplicationForOrganizer
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for event management operations
 * Used primarily by organizer user type
 */
interface EventRepository {

    /**
     * Get all events for the current organizer
     */
    suspend fun getMyEvents(): Flow<List<VolunteerEvent>>

    /** Search my events by title */
    suspend fun searchMyEvents(query: String): Flow<List<VolunteerEvent>>

    /**
     * Create a new event
     */
    suspend fun createEvent(eventData: CreateEventData): Result<VolunteerEvent>

    /**
     * Update an existing event
     */
    suspend fun updateEvent(eventId: Int, eventData: CreateEventData): Result<VolunteerEvent>

    /**
     * Delete an event
     */
    suspend fun deleteEvent(eventId: Int): Result<Unit>

    /**
     * Get event by ID
     */
    suspend fun getEventById(eventId: Int): Result<VolunteerEvent>

    /**
     * Publish/unpublish an event
     */
    suspend fun toggleEventStatus(eventId: Int, isPublished: Boolean): Result<Unit>

    /**
     * Upload or replace the image for an event.
     */
    suspend fun uploadEventImage(eventId: Int, file: java.io.File): Result<Unit>

    /**
     * Upload an image to event carousel (up to 10 images)
     */
    suspend fun uploadEventImageToCarousel(eventId: Int, file: java.io.File, isMain: Boolean): Result<Unit>

    /**
     * Set an image as the main image for an event
     */
    suspend fun setMainEventImage(eventId: Int, imageId: Int): Result<Unit>

    /**
     * Get volunteer applications for an event
     */
    suspend fun getEventApplications(eventId: Int): Flow<List<VolunteerApplicationForOrganizer>>

    /**
     * Get volunteers joined to this event
     */
    suspend fun getEventVolunteers(eventId: Int): kotlinx.coroutines.flow.Flow<List<com.example.gooddeedfeed.domain.model.JoinedVolunteer>>

    /**
     * Kick volunteer from event
     */
    suspend fun kickVolunteer(eventId: Int, volunteerId: Int): Result<Unit>

    /**
     * Generate a concise event description suggestion using the provided title.
     */
    suspend fun generateDescriptionSuggestion(title: String): Result<String>

    /**
     * Get volunteers for attendance tracking
     */
    suspend fun getEventVolunteersForAttendance(eventId: Int): Result<List<EventVolunteer>>

    /**
     * Submit volunteer attendance
     */
    suspend fun submitAttendance(attendanceData: AttendanceSubmission): Result<Map<String, Int>>
} 
