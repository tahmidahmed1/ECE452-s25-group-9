package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.data.remote.EventApiService
import com.example.gooddeedfeed.domain.model.EventStatus
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.repository.MapRepository
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(
    private val apiService: EventApiService
) : MapRepository {
    override suspend fun getMapEvents(): List<VolunteerEvent> {
        return apiService.getEvents().map { response ->
            VolunteerEvent(
                id = response.id,
                title = response.title,
                description = response.description,
                organizationId = response.organization_id,
                organizationName = response.organization_name,
                location = response.location,
                date = response.date,
                startTime = response.start_time,
                endTime = response.end_time,
                maxVolunteers = response.max_volunteers,
                currentVolunteers = response.current_volunteers,
                category = OpportunityCategory.valueOf(response.category.uppercase()),
                requirements = response.requirements,
                status = EventStatus.valueOf(response.status.uppercase()),
                createdAt = response.created_at,
                updatedAt = response.updated_at,
                latitude = response.latitude,
                longitude = response.longitude
            )
        }
    }
} 