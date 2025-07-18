package com.example.gooddeedfeed.domain.model

import java.time.LocalDateTime

data class DomainInAppNotification(
    val id: Int,
    val userId: Int,
    val title: String,
    val message: String,
    val data: Map<String, String>? = null,
    val isRead: Boolean,
    val createdAt: LocalDateTime,
    val readAt: LocalDateTime? = null,
)

data class DomainInAppNotificationsResponse(
    val notifications: List<DomainInAppNotification>,
    val unreadCount: Int,
)
