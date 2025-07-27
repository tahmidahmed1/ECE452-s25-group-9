package com.example.gooddeedfeed.presentation.viewmodel.common

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.DomainInAppNotification
import com.example.gooddeedfeed.domain.repository.NotificationRepository
import com.example.gooddeedfeed.domain.util.NotificationEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationEventBus: NotificationEventBus,
) : ViewModel() {

    // Expose event bus for composables that need real-time notifications
    val eventBus: NotificationEventBus get() = notificationEventBus

    companion object {
        private const val TAG = "NotificationViewModel"
        private const val REFRESH_INTERVAL_MS = 30000L // 30 seconds
    }

    private val _notifications = MutableStateFlow<List<DomainInAppNotification>>(emptyList())
    val notifications: StateFlow<List<DomainInAppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Flag to prevent immediate refresh after clearing notifications
    private var _recentlyClearedAll = false
    
    // Expose the recently cleared state for UI components
    val recentlyClearedAll: Boolean get() = _recentlyClearedAll

    init {
        loadNotifications()
        startPeriodicRefresh()
        listenForNotificationEvents()
    }
    
    /**
     * Start periodic refresh of notifications to ensure real-time updates
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                Log.d(TAG, "🔄 REFRESH: Periodic refresh of notifications")
                loadNotifications()
            }
        }
    }

    /**
     * Listen for notification refresh events from Firebase notifications
     */
    private fun listenForNotificationEvents() {
        viewModelScope.launch {
            notificationEventBus.notificationRefreshEvents.collect { event ->
                Log.d(TAG, "🔔 EVENT: Received notification refresh event: $event")
                when (event) {
                    is com.example.gooddeedfeed.domain.util.NotificationRefreshEvent.NewNotificationReceived -> {
                        Log.d(TAG, "🔔 EVENT: New notification received, refreshing...")
                        _recentlyClearedAll = false // Reset flag to allow refresh
                        loadNotifications()
                    }
                    is com.example.gooddeedfeed.domain.util.NotificationRefreshEvent.NotificationCleared -> {
                        Log.d(TAG, "🔔 EVENT: Notification cleared, refreshing...")
                        loadNotifications()
                    }
                }
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            // Skip loading if we recently cleared all notifications to prevent flashing
            if (_recentlyClearedAll) {
                Log.d(TAG, "🔄 LOAD: Skipping load due to recent clear all operation")
                return@launch
            }
            
            _isLoading.value = true
            _error.value = null

            notificationRepository.getInAppNotifications()
                .onSuccess { response ->
                    _notifications.value = response.notifications
                    _unreadCount.value = response.unreadCount
                    _error.value = null // Clear error on success
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load notifications"
                }

            _isLoading.value = false
        }
    }

    fun markNotificationAsRead(notificationId: Int) {
        viewModelScope.launch {
            // Store current state for potential rollback
            val previousNotifications = _notifications.value
            val previousUnreadCount = _unreadCount.value
            
            // Immediately update UI for responsive feedback
            val updatedNotifications = _notifications.value.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
            _notifications.value = updatedNotifications
            _unreadCount.value = updatedNotifications.count { !it.isRead }
            _error.value = null
            
            notificationRepository.updateNotification(notificationId, true)
                .onSuccess {
                    // UI already updated, no need to change anything
                }
                .onFailure { exception ->
                    // Rollback to previous state on failure
                    _notifications.value = previousNotifications
                    _unreadCount.value = previousUnreadCount
                    _error.value = exception.message ?: "Failed to mark notification as read"
                }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            // Store current state for potential rollback
            val previousNotifications = _notifications.value
            val previousUnreadCount = _unreadCount.value
            
            // Immediately update UI for responsive feedback
            _notifications.value = _notifications.value.map { notification ->
                notification.copy(isRead = true)
            }
            _unreadCount.value = 0
            _error.value = null
            
            notificationRepository.markAllNotificationsRead()
                .onSuccess {
                    // UI already updated, no need to change anything
                }
                .onFailure { exception ->
                    // Rollback to previous state on failure
                    _notifications.value = previousNotifications
                    _unreadCount.value = previousUnreadCount
                    _error.value = exception.message ?: "Failed to mark all notifications as read"
                }
        }
    }

    fun clearAllNotifications() {
        Log.d(TAG, "🔄 CLEAR ALL: Starting clearAllNotifications")
        Log.d(TAG, "🔄 CLEAR ALL: Current unread count: ${_unreadCount.value}")
        
        viewModelScope.launch {
            // Store current state for potential rollback
            val previousNotifications = _notifications.value
            val previousUnreadCount = _unreadCount.value
            
            Log.d(TAG, "🔄 CLEAR ALL: Previous state - notifications: ${previousNotifications.size}, unread: $previousUnreadCount")
            
            // Set flag to prevent immediate refresh
            _recentlyClearedAll = true
            
            // Immediately update UI for responsive feedback
            _notifications.value = emptyList()
            _unreadCount.value = 0
            _error.value = null
            
            Log.d(TAG, "🔄 CLEAR ALL: ✅ UI state updated - notifications: ${_notifications.value.size}, unread: ${_unreadCount.value}")
            
            notificationRepository.clearAllNotifications()
                .onSuccess {
                    Log.d(TAG, "🔄 CLEAR ALL: ✅ API call succeeded")
                    // Reset flag after successful clear and add delay to prevent race conditions
                    viewModelScope.launch {
                        delay(5000L) // Wait 5 seconds before allowing refreshes again
                        _recentlyClearedAll = false
                        Log.d(TAG, "🔄 CLEAR ALL: Reset recently cleared flag, refreshes now allowed")
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "🔄 CLEAR ALL: ❌ API call failed, rolling back: ${exception.message}")
                    // Reset flag and rollback to previous state on failure
                    _recentlyClearedAll = false
                    _notifications.value = previousNotifications
                    _unreadCount.value = previousUnreadCount
                    _error.value = exception.message ?: "Failed to clear all notifications"
                }
        }
    }

    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            // Store current state for potential rollback
            val previousNotifications = _notifications.value
            val previousUnreadCount = _unreadCount.value
            
            // Immediately update UI for responsive feedback
            val updatedNotifications = _notifications.value.filterNot { it.id == notificationId }
            _notifications.value = updatedNotifications
            _unreadCount.value = updatedNotifications.count { !it.isRead }
            _error.value = null
            
            notificationRepository.deleteNotification(notificationId)
                .onSuccess {
                    // UI already updated, no need to change anything
                }
                .onFailure { exception ->
                    // Rollback to previous state on failure
                    _notifications.value = previousNotifications
                    _unreadCount.value = previousUnreadCount
                    _error.value = exception.message ?: "Failed to delete notification"
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
    
    /**
     * Force refresh notifications - useful when receiving Firebase notifications
     */
    fun forceRefresh() {
        Log.d(TAG, "🔄 FORCE REFRESH: Manually triggered notification refresh")
        // Reset the recently cleared flag for force refreshes
        _recentlyClearedAll = false
        loadNotifications()
    }
    
    /**
     * Start aggressive polling for accurate notification count after clear all
     */
    fun startAggressivePollingAfterClear() {
        Log.d(TAG, "🔄 AGGRESSIVE POLLING: Starting polling cycle after clear all")
        viewModelScope.launch {
            // Poll every 1 second for 5 seconds (5 polls total)
            repeat(5) { pollCount ->
                delay(1000L)
                Log.d(TAG, "🔄 AGGRESSIVE POLLING: Poll #${pollCount + 1}/5 after clear all")
                loadNotifications()
                
                // Stop early if we recently cleared all (no point in continuing)
                if (_recentlyClearedAll && pollCount >= 1) {
                    Log.d(TAG, "🔄 AGGRESSIVE POLLING: Stopping early due to recent clear all")
                    return@launch
                }
            }
            Log.d(TAG, "🔄 AGGRESSIVE POLLING: Completed polling cycle after clear all")
        }
    }
}
