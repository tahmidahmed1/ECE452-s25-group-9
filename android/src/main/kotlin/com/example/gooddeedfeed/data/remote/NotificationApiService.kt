package com.example.gooddeedfeed.data.remote

import com.example.gooddeedfeed.data.remote.dto.InAppNotificationUpdateDto
import com.example.gooddeedfeed.data.remote.dto.InAppNotificationsResponseDto
import com.example.gooddeedfeed.data.remote.dto.NotificationPreferencesDto
import com.example.gooddeedfeed.data.remote.dto.NotificationTokenDto
import com.example.gooddeedfeed.data.remote.dto.SubscriptionRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationApiService @Inject constructor(
    client: HttpClient,
) : BaseApiService(client) {

    /**
     * Update FCM token for the current user
     */
    suspend fun updateFcmToken(token: String): Boolean {
        return try {
            val response = withFallbackUrls { baseUrl ->
                client.put("$baseUrl/notifications/token") {
                    contentType(ContentType.Application.Json)
                    setBody(NotificationTokenDto(fcmToken = token))
                }
            }
            response.body<Map<String, Any>>()["success"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update notification preferences for the current user
     */
    suspend fun setNotificationPreferences(enabled: Boolean): Boolean {
        return try {
            val response = withFallbackUrls { baseUrl ->
                client.put("$baseUrl/notifications/preferences") {
                    contentType(ContentType.Application.Json)
                    setBody(NotificationPreferencesDto(notificationsEnabled = enabled))
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
            val response = withFallbackUrls { baseUrl ->
                client.post("$baseUrl/notifications/subscribe") {
                    contentType(ContentType.Application.Json)
                    setBody(SubscriptionRequestDto(organizerId = organizerId))
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
            val response = withFallbackUrls { baseUrl ->
                client.post("$baseUrl/notifications/unsubscribe") {
                    contentType(ContentType.Application.Json)
                    setBody(SubscriptionRequestDto(organizerId = organizerId))
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
            withFallbackUrls { baseUrl ->
                client.get("$baseUrl/in-app-notifications?limit=$limit") {
                    contentType(ContentType.Application.Json)
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
            withFallbackUrls { baseUrl ->
                client.put("$baseUrl/in-app-notifications/$notificationId") {
                    contentType(ContentType.Application.Json)
                    setBody(InAppNotificationUpdateDto(isRead = isRead))
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
            val response = withFallbackUrls { baseUrl ->
                client.put("$baseUrl/in-app-notifications/mark-all-read") {
                    contentType(ContentType.Application.Json)
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
            val response = withFallbackUrls { baseUrl ->
                client.delete("$baseUrl/in-app-notifications") {
                    contentType(ContentType.Application.Json)
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
            val response = withFallbackUrls { baseUrl ->
                client.delete("$baseUrl/in-app-notifications/$notificationId") {
                    contentType(ContentType.Application.Json)
                }
            }
            response.body<Map<String, Any>>()["success"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
}
