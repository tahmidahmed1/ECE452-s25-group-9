package com.example.gooddeedfeed.data.services

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.remote.BaseApiService
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.util.MessageNotificationEvent
import com.example.gooddeedfeed.domain.util.NotificationEventBus
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalMessagingService @Inject constructor(
    private val httpClient: HttpClient,
    private val dataStore: DataStore<Preferences>,
    private val notificationEventBus: NotificationEventBus,
    private val coroutineScope: CoroutineScope,
) : BaseApiService(httpClient) {

    companion object {
        private const val TAG = "GlobalMessagingService"
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
    }

    private val json = Json { ignoreUnknownKeys = true }
    private var webSocketJob: Job? = null
    private var isConnected = false
    private var currentUser: DomainUser? = null

    fun startGlobalMessaging(user: DomainUser) {
        Log.d(TAG, "🌐 GLOBAL: Starting global messaging service for user ${user.id}")
        currentUser = user
        
        // Stop any existing connection
        stopGlobalMessaging()
        
        webSocketJob = coroutineScope.launch {
            connectToGlobalWebSocket(user)
        }
    }

    fun stopGlobalMessaging() {
        Log.d(TAG, "🌐 GLOBAL: Stopping global messaging service")
        webSocketJob?.cancel()
        webSocketJob = null
        isConnected = false
        currentUser = null
    }

    fun isMessagingActive(): Boolean = isConnected && currentUser != null

    private suspend fun connectToGlobalWebSocket(user: DomainUser) {
        Log.d(TAG, "🌐 GLOBAL: Connecting to global WebSocket for user ${user.id}")
        
        try {
            val sessionId = getSessionId()
            if (sessionId.isNullOrEmpty()) {
                Log.e(TAG, "🌐 GLOBAL: ❌ No authentication token for WebSocket connection")
                return
            }
            Log.d(TAG, "🌐 GLOBAL: ✅ Session ID obtained: ${sessionId.take(10)}...")

            withFallbackUrls { baseUrl ->
                val wsUrl = baseUrl.replace("http", "ws").replace("https", "wss")
                val fullWsUrl = "$wsUrl/ws/chat/${user.id}"
                Log.d(TAG, "🌐 GLOBAL: Attempting connection to: $fullWsUrl")
                
                try {
                    httpClient.webSocket(fullWsUrl) {
                        isConnected = true
                        Log.d(TAG, "🌐 GLOBAL: ✅ Connected successfully for user ${user.id}")
                        
                        // Send ping periodically to keep connection alive
                        val pingJob = launch {
                            var pingCount = 0
                            while (isConnected) {
                                try {
                                    val pingMessage = """{"type":"ping"}"""
                                    send(Frame.Text(pingMessage))
                                    pingCount++
                                    Log.d(TAG, "🌐 GLOBAL: Sent ping #$pingCount")
                                    delay(30000) // ping every 30 seconds
                                } catch (e: Exception) {
                                    Log.e(TAG, "🌐 GLOBAL: ❌ Failed to send ping: $e")
                                    break
                                }
                            }
                        }
                        
                        // Listen for incoming messages
                        Log.d(TAG, "🌐 GLOBAL: Starting to listen for incoming messages")
                        try {
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val messageText = frame.readText()
                                    Log.d(TAG, "🌐 GLOBAL: Received message: $messageText")
                                    
                                    try {
                                        val jsonElement = json.parseToJsonElement(messageText)
                                        val data = jsonElement.jsonObject
                                        
                                        val messageType = data["type"]?.jsonPrimitive?.content
                                        when (messageType) {
                                            "connection_established" -> {
                                                Log.d(TAG, "🌐 GLOBAL: ✅ Connection confirmation received")
                                            }
                                            "pong" -> {
                                                Log.d(TAG, "🌐 GLOBAL: Received pong response")
                                            }
                                            "new_message", "test_message" -> {
                                                Log.d(TAG, "🌐 GLOBAL: 📨 $messageType received!")
                                                handleNewMessage(data, user)
                                            }
                                            else -> {
                                                Log.d(TAG, "🌐 GLOBAL: Unknown message type: $messageType")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "🌐 GLOBAL: ❌ Failed to parse message: $messageText", e)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "🌐 GLOBAL: ❌ Error in message listening loop: $e")
                        }
                        
                        // Cancel ping job when connection ends
                        pingJob.cancel()
                        Log.d(TAG, "🌐 GLOBAL: Message listening ended")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "🌐 GLOBAL: ❌ Failed to establish WebSocket connection: $e")
                    throw e
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🌐 GLOBAL: ❌ WebSocket connection failed completely: $e")
            isConnected = false
            
            // Retry connection after delay
            Log.d(TAG, "🌐 GLOBAL: Will retry connection in 30 seconds...")
            delay(30_000)
            if (currentUser != null) {
                connectToGlobalWebSocket(currentUser!!)
            }
        } finally {
            isConnected = false
            Log.d(TAG, "🌐 GLOBAL: Connection cleanup completed")
        }
    }

    private suspend fun handleNewMessage(data: kotlinx.serialization.json.JsonObject, currentUser: DomainUser) {
        try {
            val messageData = data["message"]?.jsonObject
            if (messageData != null) {
                val senderId = try { messageData["sender_id"]?.jsonPrimitive?.int ?: 0 } catch (e: Exception) { 0 }
                val receiverId = try { messageData["receiver_id"]?.jsonPrimitive?.int ?: 0 } catch (e: Exception) { 0 }
                val senderName = messageData["sender_name"]?.jsonPrimitive?.content
                val messageContent = messageData["content"]?.jsonPrimitive?.content
                
                Log.d(TAG, "🌐 GLOBAL: Processing message - sender: $senderId, receiver: $receiverId, content: '$messageContent'")
                
                // Only process messages where current user is the receiver
                if (receiverId == currentUser.id && senderId != currentUser.id) {
                    Log.d(TAG, "🌐 GLOBAL: ✅ Message is for current user, emitting notification event")
                    
                    val notificationEvent = MessageNotificationEvent.NewMessage(
                        senderId = senderId,
                        receiverId = receiverId,
                        senderName = senderName,
                        messagePreview = messageContent
                    )
                    
                    notificationEventBus.emitMessageNotification(notificationEvent)
                    Log.d(TAG, "🌐 GLOBAL: ✅ Message notification event emitted successfully")
                    Log.d(TAG, "🌐 GLOBAL: 🔔 Firebase notification should be triggered automatically")
                } else {
                    Log.d(TAG, "🌐 GLOBAL: Message not for current user (current: ${currentUser.id}, receiver: $receiverId, sender: $senderId)")
                }
            } else {
                Log.e(TAG, "🌐 GLOBAL: ❌ Message data is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🌐 GLOBAL: ❌ Error handling new message: $e")
        }
    }

    private suspend fun getSessionId(): String? {
        return try {
            dataStore.data.first()[SESSION_ID_KEY]
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get session ID", e)
            null
        }
    }
}