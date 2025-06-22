package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.VolunteerEvent

interface MapRepository {
    suspend fun getMapEvents(): List<VolunteerEvent>
} 
