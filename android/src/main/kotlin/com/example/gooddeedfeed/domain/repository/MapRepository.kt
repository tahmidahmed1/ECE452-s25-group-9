package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.VolunteerEvent

interface MapRepository {
    suspend fun getMapEvents(lat: Double? = null, lon: Double? = null, radiusKm: Float = 50f): List<VolunteerEvent>
} 
