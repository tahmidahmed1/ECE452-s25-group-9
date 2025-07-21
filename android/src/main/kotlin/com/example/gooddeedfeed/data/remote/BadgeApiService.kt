package com.example.gooddeedfeed.data.remote

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.remote.dto.BadgeCheckResponseDto
import com.example.gooddeedfeed.data.remote.dto.BadgeDto
import com.example.gooddeedfeed.data.remote.dto.UserBadgeDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BadgeApiService @Inject constructor(
    client: HttpClient,
    private val dataStore: DataStore<Preferences>,
) : BaseApiService(client) {

    companion object {
        private const val TAG = "BadgeApiService"
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
    }

    private suspend fun getSessionIdFromDataStore(): String? {
        return try {
            val sessionId = dataStore.data.first()[SESSION_ID_KEY]
            Log.d(TAG, "🔍 Session ID from DataStore: ${if (sessionId != null) "Found" else "Not found"}")
            sessionId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get session ID from DataStore", e)
            null
        }
    }

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

    suspend fun getUserBadges(): Flow<Result<List<UserBadgeDto>>> = flow {
        Log.d(TAG, "🚀 Starting getUserBadges request")

        // Get session ID from DataStore
        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for getUserBadges")
            emit(Result.failure(Exception("No authentication session found")))
            return@flow
        }

        try {
            Log.d(TAG, "📤 Sending getUserBadges request with session authorization...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying getUserBadges URL: $baseUrl/users/me/badges")
                client.get("$baseUrl/users/me/badges") {
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                }
            }

            Log.d(TAG, "📥 getUserBadges response status: ${response.status}")

            if (response.status.value in 200..299) {
                val userBadges: List<UserBadgeDto> = response.body()
                Log.d(TAG, "✅ getUserBadges successful - Found ${userBadges.size} badges")
                emit(Result.success(userBadges))
            } else {
                Log.e(TAG, "❌ getUserBadges failed with status ${response.status}")
                emit(Result.failure(Exception("Failed to fetch user badges: ${response.status}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ getUserBadges failed with exception", e)
            emit(Result.failure(e))
        }
    }

    suspend fun checkBadgeAchievements(): Flow<Result<BadgeCheckResponseDto>> = flow {
        Log.d(TAG, "🚀 Starting checkBadgeAchievements request")

        // Get session ID from DataStore
        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for checkBadgeAchievements")
            emit(Result.failure(Exception("No authentication session found")))
            return@flow
        }

        try {
            Log.d(TAG, "📤 Sending checkBadgeAchievements request with session authorization...")
            val response = withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying checkBadgeAchievements URL: $baseUrl/users/me/check-badges")
                client.post("$baseUrl/users/me/check-badges") {
                    header(HttpHeaders.Authorization, "Bearer $sessionId")
                }
            }

            Log.d(TAG, "📥 checkBadgeAchievements response status: ${response.status}")

            if (response.status.value in 200..299) {
                val badgeCheckResponse: BadgeCheckResponseDto = response.body()
                Log.d(TAG, "✅ checkBadgeAchievements successful")
                emit(Result.success(badgeCheckResponse))
            } else {
                Log.e(TAG, "❌ checkBadgeAchievements failed with status ${response.status}")
                emit(Result.failure(Exception("Failed to check badge achievements: ${response.status}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ checkBadgeAchievements failed with exception", e)
            emit(Result.failure(e))
        }
    }
} 
