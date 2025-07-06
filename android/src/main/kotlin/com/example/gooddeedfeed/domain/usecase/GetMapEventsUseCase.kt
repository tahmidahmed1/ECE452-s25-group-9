package com.example.gooddeedfeed.domain.usecase

import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.repository.MapRepository

class GetMapEventsUseCase(
    private val repository: MapRepository,
) {
    suspend operator fun invoke(lat: Double? = null, lon: Double? = null, radiusKm: Float = 50f): List<VolunteerEvent> =
        repository.getMapEvents(lat, lon, radiusKm)
} 
