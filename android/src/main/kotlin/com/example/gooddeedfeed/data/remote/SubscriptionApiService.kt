package com.example.gooddeedfeed.data.remote

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.remote.dto.OrganizerWithSubscriptionStatusDto
import com.example.gooddeedfeed.data.remote.dto.SubscriptionCreateDto
import com.example.gooddeedfeed.data.remote.dto.SubscriptionResponseDto
import com.example.gooddeedfeed.data.remote.dto.SubscriptionStatusDto
import com.example.gooddeedfeed.data.remote.dto.UserSubscriptionsResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first

class SubscriptionApiService(
    client: HttpClient,
    private val dataStore: DataStore<Preferences>,
) : BaseApiService(client) {

    companion object {
        private const val TAG = "SubscriptionApiService"
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
    }

    private suspend fun getSessionIdFromDataStore(): String? {
        return try {
            dataStore.data.first()[SESSION_ID_KEY]
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get session ID from DataStore", e)
            null
        }
    }

    suspend fun subscribeToOrganizer(organizerId: Int): SubscriptionResponseDto {
        Log.d(TAG, "🚀 Starting subscribeToOrganizer request for organizer: $organizerId")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for subscribeToOrganizer")
            throw Exception("No authentication session found")
        }

        val request = SubscriptionCreateDto(organizerId)

        return try {
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying subscribeToOrganizer URL: $baseUrl/subscriptions")
                client.post("$baseUrl/subscriptions") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $sessionId")
                    setBody(request)
                }
            }.body<SubscriptionResponseDto>()
        } catch (e: Exception) {
            Log.e(TAG, "❌ subscribeToOrganizer failed", e)
            throw e
        }
    }

    suspend fun unsubscribeFromOrganizer(organizerId: Int): SubscriptionResponseDto {
        Log.d(TAG, "🚀 Starting unsubscribeFromOrganizer request for organizer: $organizerId")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for unsubscribeFromOrganizer")
            throw Exception("No authentication session found")
        }

        return try {
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying unsubscribeFromOrganizer URL: $baseUrl/subscriptions/$organizerId")
                client.delete("$baseUrl/subscriptions/$organizerId") {
                    header("Authorization", "Bearer $sessionId")
                }
            }.body<SubscriptionResponseDto>()
        } catch (e: Exception) {
            Log.e(TAG, "❌ unsubscribeFromOrganizer failed", e)
            throw e
        }
    }

    suspend fun getUserSubscriptions(): UserSubscriptionsResponseDto {
        Log.d(TAG, "🚀 Starting getUserSubscriptions request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for getUserSubscriptions")
            throw Exception("No authentication session found")
        }

        return try {
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying getUserSubscriptions URL: $baseUrl/subscriptions")
                client.get("$baseUrl/subscriptions") {
                    header("Authorization", "Bearer $sessionId")
                }
            }.body<UserSubscriptionsResponseDto>()
        } catch (e: Exception) {
            Log.e(TAG, "❌ getUserSubscriptions failed", e)
            throw e
        }
    }

    suspend fun getSubscriptionStatus(organizerId: Int): SubscriptionStatusDto {
        Log.d(TAG, "🚀 Starting getSubscriptionStatus request for organizer: $organizerId")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for getSubscriptionStatus")
            throw Exception("No authentication session found")
        }

        return try {
            withFallbackUrls { baseUrl ->
                Log.d(TAG, "🌐 Trying getSubscriptionStatus URL: $baseUrl/subscriptions/status/$organizerId")
                client.get("$baseUrl/subscriptions/status/$organizerId") {
                    header("Authorization", "Bearer $sessionId")
                }
            }.body<SubscriptionStatusDto>()
        } catch (e: Exception) {
            Log.e(TAG, "❌ getSubscriptionStatus failed", e)
            throw e
        }
    }

    suspend fun getOrganizersWithSubscriptionStatus(query: String? = null): List<OrganizerWithSubscriptionStatusDto> {
        Log.d(TAG, "🚀 Starting getOrganizersWithSubscriptionStatus request")

        val sessionId = getSessionIdFromDataStore()
        if (sessionId == null) {
            Log.e(TAG, "❌ No session ID found in DataStore for getOrganizersWithSubscriptionStatus")
            throw Exception("No authentication session found")
        }

        return try {
            withFallbackUrls { baseUrl ->
                val url = if (query != null) {
                    "$baseUrl/organizers/with-subscription-status?q=$query"
                } else {
                    "$baseUrl/organizers/with-subscription-status"
                }
                Log.d(TAG, "🌐 Trying getOrganizersWithSubscriptionStatus URL: $url")
                client.get(url) {
                    header("Authorization", "Bearer $sessionId")
                }
            }.body<List<OrganizerWithSubscriptionStatusDto>>()
        } catch (e: Exception) {
            Log.e(TAG, "❌ getOrganizersWithSubscriptionStatus failed", e)
            throw e
        }
    }
} 
