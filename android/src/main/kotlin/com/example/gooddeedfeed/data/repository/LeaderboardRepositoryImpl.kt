package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.remote.LeaderboardApiService
import com.example.gooddeedfeed.domain.model.DomainLeaderboardResponse
import com.example.gooddeedfeed.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

@Singleton
class LeaderboardRepositoryImpl @Inject constructor(
    private val leaderboardApiService: LeaderboardApiService,
) : LeaderboardRepository {

    override suspend fun getLeaderboard(page: Int, pageSize: Int): Flow<Result<DomainLeaderboardResponse>> = flow {
        try {
            val response = leaderboardApiService.getLeaderboard(page, pageSize)
            emit(Result.success(response.toDomain()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
} 
