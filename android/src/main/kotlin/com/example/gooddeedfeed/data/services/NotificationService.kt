package com.example.gooddeedfeed.data.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gooddeedfeed.R
import com.example.gooddeedfeed.domain.repository.NotificationRepository
import com.example.gooddeedfeed.domain.util.MessageNotificationEvent
import com.example.gooddeedfeed.domain.util.NotificationEventBus
import com.example.gooddeedfeed.domain.util.NotificationRefreshEvent
import com.example.gooddeedfeed.presentation.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for handling push notifications
 */
@AndroidEntryPoint
class NotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var notificationEventBus: NotificationEventBus

    companion object {
        private const val TAG = "NotificationService"
        private const val CHANNEL_ID = "good_deed_feed_notifications"
        private const val CHANNEL_NAME = "Good Deed Feed"
        private const val CHANNEL_DESCRIPTION = "Notifications for new volunteer opportunities"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "🔑 FCM TOKEN: New FCM token received")
        Log.i(TAG, "🔑 FCM TOKEN: Token: ${token.take(20)}...${token.takeLast(10)}")
        Log.i(TAG, "🔑 FCM TOKEN: Token length: ${token.length}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "🔑 FCM TOKEN: Attempting to update FCM token on server")
                notificationRepository.updateFcmToken(token)
                Log.i(TAG, "🔑 FCM TOKEN: ✅ FCM token updated successfully on server")
            } catch (e: Exception) {
                Log.e(TAG, "🔑 FCM TOKEN: ❌ Failed to update FCM token on server", e)
                Log.e(TAG, "🔑 FCM TOKEN: Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "🔑 FCM TOKEN: Exception message: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.i(TAG, "📥 FCM MESSAGE: Message received from Firebase")
        Log.i(TAG, "📥 FCM MESSAGE: From: ${remoteMessage.from}")
        Log.i(TAG, "📥 FCM MESSAGE: Message ID: ${remoteMessage.messageId}")
        Log.i(TAG, "📥 FCM MESSAGE: Message type: ${remoteMessage.messageType}")
        Log.i(TAG, "📥 FCM MESSAGE: TTL: ${remoteMessage.ttl}")
        Log.i(TAG, "📥 FCM MESSAGE: Priority: ${remoteMessage.priority}")

        // Check if message has data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.i(TAG, "📥 FCM MESSAGE: Data payload present: ${remoteMessage.data}")
            Log.i(TAG, "📥 FCM MESSAGE: Data keys: ${remoteMessage.data.keys}")
            for ((key, value) in remoteMessage.data) {
                Log.i(TAG, "📥 FCM MESSAGE: Data - $key: $value")
            }
            handleDataMessage(remoteMessage.data)
        } else {
            Log.i(TAG, "📥 FCM MESSAGE: No data payload")
        }

        // Check if message has notification payload
        remoteMessage.notification?.let { notification ->
            Log.i(TAG, "📥 FCM MESSAGE: Notification payload present")
            Log.i(TAG, "📥 FCM MESSAGE: Notification title: '${notification.title}'")
            Log.i(TAG, "📥 FCM MESSAGE: Notification body: '${notification.body}'")
            Log.i(TAG, "📥 FCM MESSAGE: Notification icon: ${notification.icon}")
            Log.i(TAG, "📥 FCM MESSAGE: Notification color: ${notification.color}")
            Log.i(TAG, "📥 FCM MESSAGE: Notification sound: ${notification.sound}")
            Log.i(TAG, "📥 FCM MESSAGE: Notification tag: ${notification.tag}")

            showNotification(
                title = notification.title ?: "Good Deed Feed",
                body = notification.body ?: "You have a new notification",
                data = remoteMessage.data,
            )
        } ?: run {
            Log.i(TAG, "📥 FCM MESSAGE: No notification payload")
        }

        Log.i(TAG, "📥 FCM MESSAGE: Message processing completed")
    }

    private fun handleDataMessage(data: Map<String, String>) {
        Log.i(TAG, "🔄 DATA MESSAGE: Processing data-only message")

        val type = data["type"]
        val eventId = data["eventId"]
        val organizerName = data["organizerName"]
        val eventTitle = data["eventTitle"]

        Log.i(TAG, "🔄 DATA MESSAGE: Type: '$type'")
        Log.i(TAG, "🔄 DATA MESSAGE: Event ID: '$eventId'")
        Log.i(TAG, "🔄 DATA MESSAGE: Organizer: '$organizerName'")
        Log.i(TAG, "🔄 DATA MESSAGE: Event Title: '$eventTitle'")

        when (type) {
            "new_event" -> {
                Log.i(TAG, "🔄 DATA MESSAGE: Handling new_event notification")

                // Emit notification refresh event for real-time UI updates
                CoroutineScope(Dispatchers.IO).launch {
                    notificationEventBus.emitNotificationRefresh(NotificationRefreshEvent.NewNotificationReceived)
                }

                showNotification(
                    title = "New Volunteer Opportunity!",
                    body = "$organizerName posted: $eventTitle",
                    data = data,
                )
            }
            "event_reminder" -> {
                Log.i(TAG, "🔄 DATA MESSAGE: Handling event_reminder notification")

                // Emit notification refresh event for real-time UI updates
                CoroutineScope(Dispatchers.IO).launch {
                    notificationEventBus.emitNotificationRefresh(NotificationRefreshEvent.NewNotificationReceived)
                }

                showNotification(
                    title = "Event Reminder",
                    body = "Don't forget about: $eventTitle",
                    data = data,
                )
            }
            "subscription_update" -> {
                Log.i(TAG, "🔄 DATA MESSAGE: Handling subscription_update notification")

                // Emit notification refresh event for real-time UI updates
                CoroutineScope(Dispatchers.IO).launch {
                    notificationEventBus.emitNotificationRefresh(NotificationRefreshEvent.NewNotificationReceived)
                }

                showNotification(
                    title = "Organization Update",
                    body = "$organizerName has an update for you",
                    data = data,
                )
            }
            "new_message" -> {
                Log.i(TAG, "🔄 DATA MESSAGE: Handling new_message notification")
                val senderName = data["senderName"]
                val messagePreview = data["messagePreview"]
                val senderId = data["senderId"]?.toIntOrNull()
                val receiverId = data["receiverId"]?.toIntOrNull()

                // Emit notification refresh event for real-time UI updates (for app top bar)
                CoroutineScope(Dispatchers.IO).launch {
                    notificationEventBus.emitNotificationRefresh(NotificationRefreshEvent.NewNotificationReceived)
                }

                // Emit notification event for real-time chat updates
                if (senderId != null && receiverId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        notificationEventBus.emitMessageNotification(
                            MessageNotificationEvent.NewMessage(
                                senderId = senderId,
                                receiverId = receiverId,
                                senderName = senderName,
                                messagePreview = messagePreview,
                            ),
                        )
                    }
                }

                showNotification(
                    title = "Message from $senderName",
                    body = messagePreview ?: "You have a new message",
                    data = data,
                )
            }
            else -> {
                Log.w(TAG, "🔄 DATA MESSAGE: ⚠️ Unknown message type: '$type'")
            }
        }

        Log.i(TAG, "🔄 DATA MESSAGE: Data message processing completed")
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        Log.i(TAG, "🔔 SHOW NOTIFICATION: Creating and displaying notification")
        Log.i(TAG, "🔔 SHOW NOTIFICATION: Title: '$title'")
        Log.i(TAG, "🔔 SHOW NOTIFICATION: Body: '$body'")
        Log.i(TAG, "🔔 SHOW NOTIFICATION: Data extras: $data")

        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                data["eventId"]?.let {
                    putExtra("eventId", it)
                    Log.i(TAG, "🔔 SHOW NOTIFICATION: Added eventId extra: $it")
                }
                data["type"]?.let {
                    putExtra("notificationType", it)
                    Log.i(TAG, "🔔 SHOW NOTIFICATION: Added notificationType extra: $it")
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            Log.i(TAG, "🔔 SHOW NOTIFICATION: PendingIntent created successfully")

            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))

            Log.i(TAG, "🔔 SHOW NOTIFICATION: Notification builder configured")
            Log.i(TAG, "🔔 SHOW NOTIFICATION: Channel ID: $CHANNEL_ID")
            Log.i(TAG, "🔔 SHOW NOTIFICATION: Priority: HIGH")

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            Log.i(TAG, "🔔 SHOW NOTIFICATION: NotificationManager obtained")

            val notification = notificationBuilder.build()
            Log.i(TAG, "🔔 SHOW NOTIFICATION: Notification built successfully")

            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.i(TAG, "🔔 SHOW NOTIFICATION: ✅ Notification displayed successfully with ID: $NOTIFICATION_ID")
        } catch (e: Exception) {
            Log.e(TAG, "🔔 SHOW NOTIFICATION: ❌ Failed to show notification", e)
            Log.e(TAG, "🔔 SHOW NOTIFICATION: Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "🔔 SHOW NOTIFICATION: Exception message: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        Log.i(TAG, "📢 CHANNEL: Creating notification channel")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(TAG, "📢 CHANNEL: Android O+ detected, creating notification channel")

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            Log.i(TAG, "📢 CHANNEL: Channel configured - ID: $CHANNEL_ID, Name: $CHANNEL_NAME")
            Log.i(TAG, "📢 CHANNEL: Channel importance: HIGH")
            Log.i(TAG, "📢 CHANNEL: Lights enabled: true, Vibration enabled: true")

            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel)
                Log.i(TAG, "📢 CHANNEL: ✅ Notification channel created successfully")
            } else {
                Log.e(TAG, "📢 CHANNEL: ❌ NotificationManager is null")
            }
        } else {
            Log.i(TAG, "📢 CHANNEL: Android version < O, notification channel not needed")
        }
    }
}
