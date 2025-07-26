package com.example.gooddeedfeed.data.remote

import com.example.gooddeedfeed.data.remote.dto.LeaderboardResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class LeaderboardApiService(client: HttpClient) : BaseApiService(client) {

    suspend fun getLeaderboard(
        page: Int = 1,
        pageSize: Int = 20,
    ): LeaderboardResponseDto {
        return withFallbackUrls { baseUrl ->
            client.get("$baseUrl/leaderboard") {
                parameter("page", page)
                parameter("page_size", pageSize)
            }.body()
        }
    }
} 
