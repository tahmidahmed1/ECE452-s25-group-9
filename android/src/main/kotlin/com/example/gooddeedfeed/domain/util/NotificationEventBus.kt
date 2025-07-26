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

    suspend fun emitMessageNotification(event: MessageNotificationEvent) {
        Log.d("NotificationEventBus", "🔔 EMITTING: Message notification event: $event")
        _messageNotificationEvents.emit(event)
        Log.d("NotificationEventBus", "🔔 EMITTED: Message notification event successfully")
    }
}

sealed class MessageNotificationEvent {
    data class NewMessage(
        val senderId: Int,
        val receiverId: Int,
        val senderName: String?,
        val messagePreview: String?
    ) : MessageNotificationEvent()
}