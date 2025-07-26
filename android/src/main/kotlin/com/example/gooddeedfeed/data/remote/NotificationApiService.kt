package com.example.gooddeedfeed.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.remote.dto.InAppNotificationUpdateDto
import com.example.gooddeedfeed.data.remote.dto.InAppNotificationsResponseDto
import com.example.gooddeedfeed.data.remote.dto.NotificationPreferencesDto
import com.example.gooddeedfeed.data.remote.dto.NotificationTokenDto
import com.example.gooddeedfeed.data.remote.dto.SubscriptionRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationApiService @Inject constructor(
    client: HttpClient,
    private val dataStore: DataStore<Preferences>,
) : BaseApiService(client) {

    companion object {
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
    }

    private suspend fun getSessionId(): String? {
        return try {
            dataStore.data.first()[SESSION_ID_KEY]
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update FCM token for the current user
     */
    suspend fun updateFcmToken(token: String): Boolean {
        return try {
            val sessionId = getSessionId() ?: throw Exception("No authentication session found")
            android.util.Log.i("NotificationApiService", "🔄 API: Updating FCM token on server")
            android.util.Log.i("NotificationApiService", "🔄 API: Token: ${token.take(20)}...${token.takeLast(10)}")
            android.util.Log.i("NotificationApiService", "🔄 API: Session ID: ${sessionId.take(10)}...")
            
            val response = withFallbackUrls { baseUrl ->
                android.util.Log.i("NotificationApiService", "🔄 API: Sending PUT request to: $baseUrl/notifications/token")
                client.put("$baseUrl/notifications/token") {
                    contentType(ContentType.Application.Json)
                    setBody(NotificationTokenDto(fcmToken = token))
                    header("Authorization", "Bearer $sessionId")
                }
            }
            
            val responseText = response.body<String>()
            android.util.Log.i("NotificationApiService", "🔄 API: Server response: $responseText")
            
            // Parse the JSON manually to avoid serialization issues
            val success = responseText.contains("\"success\":true")
            android.util.Log.i("NotificationApiService", "🔄 API: Success result: $success")
            
            success
        } catch (e: Exception) {
            android.util.Log.e("NotificationApiService", "🔄 API: Failed to update FCM token", e)
            android.util.Log.e("NotificationApiService", "🔄 API: Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("NotificationApiService", "🔄 API: Exception message: ${e.message}")
            false
        }
    }

    /**
     * Update notification preferences for the current user
     */
    suspend fun setNotificationPreferences(enabled: Boolean): Boolean {
        return try {
            val sessionId = getSessionId() ?: throw Exception("No authentication session found")
            val response = withFallbackUrls { baseUrl ->
                client.put("$baseUrl/notifications/preferences") {
                    contentType(ContentType.Application.Json)
                    setBody(NotificationPreferencesDto(notificationsEnabled = enabled))
                    header("Authorization", "Bearer $sessionId")
                }
            }
            response.body<Map<String, Any>>()["success"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Subscribe to an organizer's notifications
     */
    suspend fun subscribeToOrganizer(organizerId: Int): Boolean {
        return try {
            val sessionId = getSessionId() ?: throw Exception("No authentication session found")
            val response = withFallbackUrls { baseUrl ->
                client.post("$baseUrl/notifications/subscribe") {
                    contentType(ContentType.Application.Json)
                    setBody(SubscriptionRequestDto(organizerId = organizerId))
                    header("Authorization", "Bearer $sessionId")
                }
            }
            response.body<Map<String, Any>>()["success"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Unsubscribe from an organizer's notifications
     */
    suspend fun unsubscribeFromOrganizer(organizerId: Int): Boolean {
        return try {
            val sessionId = getSessionId() ?: throw Exception("No authentication session found")
            val response = withFallbackUrls { baseUrl ->
                client.post("$baseUrl/notifications/unsubscribe") {
                    contentType(ContentType.Application.Json)
                    setBody(SubscriptionRequestDto(organizerId = organizerId))
                    header("Authorization", "Bearer $sessionId")
                }
            }
            response.body<Map<String, Any>>()["success"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get in-app notifications for the current user
     */
    suspend fun getInAppNotifications(limit: Int = 50): InAppNotificationsResponseDto? {
        return try {
            val sessionId = getSessionId() ?: throw Exception("No authentication session found")
            withFallbackUrls { baseUrl ->
                client.get("$baseUrl/in-app-notifications?limit=$limit") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $sessionId")
                }
            }.body<InAppNotificationsResponseDto>()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Mark a notification as read/unread
     */
    suspend fun updateNotification(notificationId: Int, isRead: Boolean): Boolean {
        return try {
            val sessionId = getSessionId() ?: throw Exception("No authentication session found")
            withFallbackUrls { baseUrl ->
                client.put("$baseUrl/in-app-notifications/$notificationId") {
                    contentType(ContentType.Application.Json)
                    setBody(InAppNotificationUpdateDto(isRead = isRead))
                    header("Authorization", "Bearer $sessionId")
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Mark all notifications as read
     */
    suspend fun markAllNotificationsRead(): Boolean {
        return try {
            val sessionId = getSessionId() ?: throw Exception("No authentication session found")
            val response = withFallbackUrls { baseUrl ->
                client.put("$baseUrl/in-app-notifications/mark-all-read") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $sessionId")
                }
            }
            response.body<Map<String, Any>>()["success"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear all notifications
     */
    suspend fun clearAllNotifications(): Boolean {
        return try {
            val sessionId = getSessionId() ?: throw Exception("No authentication session found")
            val response = withFallbackUrls { baseUrl ->
                client.delete("$baseUrl/in-app-notifications") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $sessionId")
                }
            }
            response.body<Map<String, Any>>()["success"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a specific notification
     */
    suspend fun deleteNotification(notificationId: Int): Boolean {
        return try {
            val sessionId = getSessionId() ?: throw Exception("No authentication session found")
            val response = withFallbackUrls { baseUrl ->
                client.delete("$baseUrl/in-app-notifications/$notificationId") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $sessionId")
                }
            }
            response.body<Map<String, Any>>()["success"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
}
