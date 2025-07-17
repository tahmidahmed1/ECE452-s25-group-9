package com.example.gooddeedfeed.data.remote

import com.example.gooddeedfeed.data.remote.dto.BadgeCheckResponseDto
import com.example.gooddeedfeed.data.remote.dto.BadgeDto
import com.example.gooddeedfeed.data.remote.dto.UserBadgeDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BadgeApiService(client: HttpClient) : BaseApiService(client) {

    suspend fun getAllBadges(): Flow<Result<List<BadgeDto>>> = flow {
        try {
            val response = withFallbackUrls { baseUrl ->
                client.get("$baseUrl/badges")
            }

            if (response.status.value in 200..299) {
                val badges: List<BadgeDto> = response.body()
                emit(Result.success(badges))
            } else {
                emit(Result.failure(Exception("Failed to fetch badges: ${response.status}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun getUserBadges(token: String): Flow<Result<List<UserBadgeDto>>> = flow {
        try {
            val response = withFallbackUrls { baseUrl ->
                client.get("$baseUrl/users/me/badges") {
                    bearerAuth(token)
                }
            }

            if (response.status.value in 200..299) {
                val userBadges: List<UserBadgeDto> = response.body()
                emit(Result.success(userBadges))
            } else {
                emit(Result.failure(Exception("Failed to fetch user badges: ${response.status}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun checkBadgeAchievements(token: String): Flow<Result<BadgeCheckResponseDto>> = flow {
        try {
            val response = withFallbackUrls { baseUrl ->
                client.post("$baseUrl/users/me/check-badges") {
                    bearerAuth(token)
                }
            }

            if (response.status.value in 200..299) {
                val badgeCheckResponse: BadgeCheckResponseDto = response.body()
                emit(Result.success(badgeCheckResponse))
            } else {
                emit(Result.failure(Exception("Failed to check badge achievements: ${response.status}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
} 
