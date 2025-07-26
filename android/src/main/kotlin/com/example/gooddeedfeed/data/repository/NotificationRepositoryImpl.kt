package com.example.gooddeedfeed.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.remote.NotificationApiService
import com.example.gooddeedfeed.domain.model.DomainInAppNotificationsResponse
import com.example.gooddeedfeed.domain.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val notificationApiService: NotificationApiService,
    private val firebaseMessaging: FirebaseMessaging,
) : NotificationRepository {

    companion object {
        private const val TAG = "NotificationRepository"
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> {
        Log.i(TAG, "🔄 UPDATE FCM TOKEN: Starting FCM token update process")
        Log.i(TAG, "🔄 UPDATE FCM TOKEN: Token: ${token.take(20)}...${token.takeLast(10)}")
        Log.i(TAG, "🔄 UPDATE FCM TOKEN: Token length: ${token.length}")

        return try {
            Log.i(TAG, "🔄 UPDATE FCM TOKEN: Saving token to local datastore")
            dataStore.edit { preferences ->
                preferences[FCM_TOKEN_KEY] = token
            }
            Log.i(TAG, "🔄 UPDATE FCM TOKEN: ✅ Token saved to local datastore")

            Log.i(TAG, "🔄 UPDATE FCM TOKEN: Sending token to server")
            notificationApiService.updateFcmToken(token)
            Log.i(TAG, "🔄 UPDATE FCM TOKEN: ✅ Token sent to server successfully")

            Log.i(TAG, "🔄 UPDATE FCM TOKEN: ✅ FCM token updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "🔄 UPDATE FCM TOKEN: ❌ Failed to update FCM token", e)
            Log.e(TAG, "🔄 UPDATE FCM TOKEN: Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "🔄 UPDATE FCM TOKEN: Exception message: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getFcmToken(): Result<String?> {
        return try {
            val localToken = dataStore.data.map { preferences ->
                preferences[FCM_TOKEN_KEY]
            }.first()

            if (localToken != null) {
                Result.success(localToken)
            } else {
                val newToken = firebaseMessaging.token.await()
                updateFcmToken(newToken)
                Result.success(newToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            Result.failure(e)
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
            }

            notificationApiService.setNotificationPreferences(enabled)

            Log.d(TAG, "Notification preferences updated: $enabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification preferences", e)
            Result.failure(e)
        }
    }

    override suspend fun areNotificationsEnabled(): Result<Boolean> {
        return try {
            val enabled = dataStore.data.map { preferences ->
                preferences[NOTIFICATIONS_ENABLED_KEY] ?: true // Default to enabled
            }.first()

            Result.success(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get notification preferences", e)
            Result.failure(e)
        }
    }

    override suspend fun subscribeToOrganizer(organizerId: Int): Result<Unit> {
        return try {
            notificationApiService.subscribeToOrganizer(organizerId)
            Log.d(TAG, "Subscribed to organizer: $organizerId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to organizer: $organizerId", e)
            Result.failure(e)
        }
    }

    override suspend fun unsubscribeFromOrganizer(organizerId: Int): Result<Unit> {
        return try {
            notificationApiService.unsubscribeFromOrganizer(organizerId)
            Log.d(TAG, "Unsubscribed from organizer: $organizerId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from organizer: $organizerId", e)
            Result.failure(e)
        }
    }

    override suspend fun getInAppNotifications(limit: Int): Result<DomainInAppNotificationsResponse> {
        return try {
            val response = notificationApiService.getInAppNotifications(limit)
            if (response != null) {
                Log.d(TAG, "Retrieved ${response.notifications.size} in-app notifications")
                Result.success(response.toDomain())
            } else {
                Log.e(TAG, "Failed to retrieve in-app notifications: null response")
                Result.failure(Exception("Failed to retrieve notifications"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get in-app notifications", e)
            Result.failure(e)
        }
    }

    override suspend fun updateNotification(notificationId: Int, isRead: Boolean): Result<Unit> {
        return try {
            val success = notificationApiService.updateNotification(notificationId, isRead)
            if (success) {
                Log.d(TAG, "Notification $notificationId marked as ${if (isRead) "read" else "unread"}")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to update notification $notificationId")
                Result.failure(Exception("Failed to update notification"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification: $notificationId", e)
            Result.failure(e)
        }
    }

    override suspend fun markAllNotificationsRead(): Result<Unit> {
        return try {
            val success = notificationApiService.markAllNotificationsRead()
            if (success) {
                Log.d(TAG, "All notifications marked as read")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to mark all notifications as read")
                Result.failure(Exception("Failed to mark all notifications as read"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark all notifications as read", e)
            Result.failure(e)
        }
    }

    override suspend fun clearAllNotifications(): Result<Unit> {
        return try {
            val success = notificationApiService.clearAllNotifications()
            if (success) {
                Log.d(TAG, "All notifications cleared")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to clear all notifications")
                Result.failure(Exception("Failed to clear all notifications"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all notifications", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: Int): Result<Unit> {
        return try {
            val success = notificationApiService.deleteNotification(notificationId)
            if (success) {
                Log.d(TAG, "Notification $notificationId deleted")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to delete notification $notificationId")
                Result.failure(Exception("Failed to delete notification"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification: $notificationId", e)
            Result.failure(e)
        }
    }
}
