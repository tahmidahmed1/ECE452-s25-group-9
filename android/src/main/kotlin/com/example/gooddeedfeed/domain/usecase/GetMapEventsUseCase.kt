package com.example.gooddeedfeed.domain.usecase

import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.repository.MapRepository
 
class GetMapEventsUseCase(
    private val repository: MapRepository
) {
    suspend operator fun invoke(): List<VolunteerEvent> = repository.getMapEvents()
} 