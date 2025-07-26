package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class NotificationTokenDto(
    val fcmToken: String,
)

@Serializable
data class NotificationPreferencesDto(
    val notificationsEnabled: Boolean,
)

@Serializable
data class SubscriptionRequestDto(
    val organizerId: Int,
)

@Serializable
data class NotificationResponseDto(
    val success: Boolean,
    val message: String? = null,
)

@Serializable
data class SendNotificationDto(
    val title: String,
    val body: String,
    val data: Map<String, String>? = null,
    val organizerId: Int,
)

@Serializable
data class InAppNotificationDto(
    val id: Int,
    @SerialName("user_id")
    val userId: Int,
    val title: String,
    val message: String,
    val data: Map<String, String>? = null,
    @SerialName("is_read")
    val isRead: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("read_at")
    val readAt: String? = null,
)

@Serializable
data class InAppNotificationsResponseDto(
    val notifications: List<InAppNotificationDto> = emptyList(), // Default to empty list if missing
    @SerialName("unread_count")
    val unreadCount: Int = 0, // Default to 0 if missing from server response
)

@Serializable
data class InAppNotificationUpdateDto(
    val isRead: Boolean? = null,
)
