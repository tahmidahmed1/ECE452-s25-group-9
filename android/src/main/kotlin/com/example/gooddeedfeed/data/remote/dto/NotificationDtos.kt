package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.Serializable

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
    val userId: Int,
    val title: String,
    val message: String,
    val data: Map<String, String>? = null,
    val isRead: Boolean,
    val createdAt: String,
    val readAt: String? = null,
)

@Serializable
data class InAppNotificationsResponseDto(
    val notifications: List<InAppNotificationDto> = emptyList(),  // Default to empty list if missing
    val unreadCount: Int = 0,  // Default to 0 if missing from server response
)

@Serializable
data class InAppNotificationUpdateDto(
    val isRead: Boolean? = null,
)
