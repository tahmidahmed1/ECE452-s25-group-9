package com.example.gooddeedfeed.presentation.viewmodel.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.DomainInAppNotification
import com.example.gooddeedfeed.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<DomainInAppNotification>>(emptyList())
    val notifications: StateFlow<List<DomainInAppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            notificationRepository.getInAppNotifications()
                .onSuccess { response ->
                    _notifications.value = response.notifications
                    _unreadCount.value = response.unreadCount
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load notifications"
                }

            _isLoading.value = false
        }
    }

    fun markNotificationAsRead(notificationId: Int) {
        viewModelScope.launch {
            notificationRepository.updateNotification(notificationId, true)
                .onSuccess {
                    // Update local state
                    _notifications.value = _notifications.value.map { notification ->
                        if (notification.id == notificationId) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }
                    // Update unread count
                    _unreadCount.value = _notifications.value.count { !it.isRead }
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to mark notification as read"
                }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            notificationRepository.markAllNotificationsRead()
                .onSuccess {
                    // Update local state
                    _notifications.value = _notifications.value.map { notification ->
                        notification.copy(isRead = true)
                    }
                    _unreadCount.value = 0
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to mark all notifications as read"
                }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.clearAllNotifications()
                .onSuccess {
                    _notifications.value = emptyList()
                    _unreadCount.value = 0
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to clear all notifications"
                }
        }
    }

    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
                .onSuccess {
                    // Update local state
                    val updatedNotifications = _notifications.value.filterNot { it.id == notificationId }
                    _notifications.value = updatedNotifications
                    _unreadCount.value = updatedNotifications.count { !it.isRead }
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to delete notification"
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
