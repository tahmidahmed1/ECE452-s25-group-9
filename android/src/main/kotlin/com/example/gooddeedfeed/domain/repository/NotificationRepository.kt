package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.DomainInAppNotificationsResponse

/**
 * Repository interface for managing FCM tokens and notification preferences
 */
interface NotificationRepository {
    /**
     * Update the FCM token for the current user
     */
    suspend fun updateFcmToken(token: String): Result<Unit>
    
    /**
     * Get the current FCM token
     */
    suspend fun getFcmToken(): Result<String?>
    
    /**
     * Enable/disable notifications for the user
     */
    suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit>
    
    /**
     * Check if notifications are enabled
     */
    suspend fun areNotificationsEnabled(): Result<Boolean>
    
    /**
     * Subscribe to an organizer's notifications
     */
    suspend fun subscribeToOrganizer(organizerId: Int): Result<Unit>
    
    /**
     * Unsubscribe from an organizer's notifications
     */
    suspend fun unsubscribeFromOrganizer(organizerId: Int): Result<Unit>
    
    /**
     * Get in-app notifications for the current user
     */
    suspend fun getInAppNotifications(limit: Int = 50): Result<DomainInAppNotificationsResponse>
    
    /**
     * Mark a notification as read/unread
     */
    suspend fun updateNotification(notificationId: Int, isRead: Boolean): Result<Unit>
    
    /**
     * Mark all notifications as read
     */
    suspend fun markAllNotificationsRead(): Result<Unit>
    
    /**
     * Clear all notifications
     */
    suspend fun clearAllNotifications(): Result<Unit>
    
    /**
     * Delete a specific notification
     */
    suspend fun deleteNotification(notificationId: Int): Result<Unit>
}