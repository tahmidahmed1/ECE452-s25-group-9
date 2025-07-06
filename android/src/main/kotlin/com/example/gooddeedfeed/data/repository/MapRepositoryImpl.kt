package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.data.remote.EventApiService
import com.example.gooddeedfeed.data.remote.dto.toDomain
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.repository.MapRepository
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(
    private val apiService: EventApiService,
) : MapRepository {
    override suspend fun getMapEvents(lat: Double?, lon: Double?, radiusKm: Float): List<VolunteerEvent> {
        return apiService.getAllEvents(lat, lon, radiusKm).map { it.toDomain() }
    }
} 
