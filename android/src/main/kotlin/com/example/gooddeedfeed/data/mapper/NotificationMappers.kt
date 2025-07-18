package com.example.gooddeedfeed.data.mapper

import com.example.gooddeedfeed.data.remote.dto.InAppNotificationDto
import com.example.gooddeedfeed.data.remote.dto.InAppNotificationsResponseDto
import com.example.gooddeedfeed.domain.model.DomainInAppNotification
import com.example.gooddeedfeed.domain.model.DomainInAppNotificationsResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun InAppNotificationDto.toDomain(): DomainInAppNotification {
    return DomainInAppNotification(
        id = id,
        userId = userId,
        title = title,
        message = message,
        data = data,
        isRead = isRead,
        createdAt = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_DATE_TIME),
        readAt = readAt?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
    )
}

fun InAppNotificationsResponseDto.toDomain(): DomainInAppNotificationsResponse {
    return DomainInAppNotificationsResponse(
        notifications = notifications.map { it.toDomain() },
        unreadCount = unreadCount,
    )
}
