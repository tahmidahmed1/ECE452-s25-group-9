package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.DomainLeaderboardResponse
import kotlinx.coroutines.flow.Flow
import kotlin.Result

interface LeaderboardRepository {
    suspend fun getLeaderboard(page: Int, pageSize: Int): Flow<Result<DomainLeaderboardResponse>>
} 