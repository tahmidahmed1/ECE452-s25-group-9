package com.example.gooddeedfeed.domain.util

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationEventBus @Inject constructor() {
    private val _messageNotificationEvents = MutableSharedFlow<MessageNotificationEvent>()
    val messageNotificationEvents: SharedFlow<MessageNotificationEvent> = _messageNotificationEvents.asSharedFlow()

    private val _notificationRefreshEvents = MutableSharedFlow<NotificationRefreshEvent>()
    val notificationRefreshEvents: SharedFlow<NotificationRefreshEvent> = _notificationRefreshEvents.asSharedFlow()

    suspend fun emitMessageNotification(event: MessageNotificationEvent) {
        Log.d("NotificationEventBus", "🔔 EMITTING: Message notification event: $event")
        _messageNotificationEvents.emit(event)
        Log.d("NotificationEventBus", "🔔 EMITTED: Message notification event successfully - Firebase should handle notifications")
    }

    suspend fun emitNotificationRefresh(event: NotificationRefreshEvent) {
        Log.d("NotificationEventBus", "🔔 EMITTING: Notification refresh event: $event")
        _notificationRefreshEvents.emit(event)
        Log.d("NotificationEventBus", "🔔 EMITTED: Notification refresh event successfully")
    }
}

sealed class MessageNotificationEvent {
    data class NewMessage(
        val senderId: Int,
        val receiverId: Int,
        val senderName: String?,
        val messagePreview: String?,
    ) : MessageNotificationEvent()
}

sealed class NotificationRefreshEvent {
    object NewNotificationReceived : NotificationRefreshEvent()
    object NotificationCleared : NotificationRefreshEvent()
}
