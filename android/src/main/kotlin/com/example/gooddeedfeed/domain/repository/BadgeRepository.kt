package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.DomainBadge
import com.example.gooddeedfeed.domain.model.DomainBadgeCheckResponse
import com.example.gooddeedfeed.domain.model.DomainUserBadge
import kotlinx.coroutines.flow.Flow

interface BadgeRepository {
    suspend fun getAllBadges(): Flow<Result<List<DomainBadge>>>
    suspend fun getUserBadges(): Flow<Result<List<DomainUserBadge>>>
    suspend fun checkBadgeAchievements(): Flow<Result<DomainBadgeCheckResponse>>
} 
