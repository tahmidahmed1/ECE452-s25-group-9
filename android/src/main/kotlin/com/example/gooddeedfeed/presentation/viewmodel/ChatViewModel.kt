package com.example.gooddeedfeed.presentation.viewmodel

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.remote.AuthApiService
import com.example.gooddeedfeed.data.remote.ChatApiService
import com.example.gooddeedfeed.data.remote.dto.MessageDto
import com.example.gooddeedfeed.data.repository.ConversationPreferencesRepository
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.util.MessageNotificationEvent
import com.example.gooddeedfeed.domain.util.NotificationEventBus
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import javax.inject.Inject

@Serializable
data class ChatMessage(
    val id: String,
    val content: String,
    val senderName: String,
    val senderType: String,
    val timestamp: String,
    val isFromCurrentUser: Boolean = false,
    val senderId: Int,
    val receiverId: Int,
)

@Serializable
data class SendMessageRequest(
    val receiver_id: Int,
    val content: String,
)

@Serializable
data class ChatConversation(
    val id: String,
    val title: String,
    val subtitle: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isStarred: Boolean = false,
    val participantCount: Int = 0,
    val otherUserId: Int,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val httpClient: HttpClient,
    private val chatApiService: ChatApiService,
    private val authApiService: AuthApiService,
    private val dataStore: DataStore<Preferences>,
    private val conversationPreferencesRepository: ConversationPreferencesRepository,
    private val notificationEventBus: NotificationEventBus,
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
    }

    private val _conversationsState = MutableStateFlow<UiState<List<ChatConversation>>>(UiState.Loading)
    val conversationsState: StateFlow<UiState<List<ChatConversation>>> = _conversationsState.asStateFlow()

    private val _messagesState = MutableStateFlow<UiState<List<ChatMessage>>>(UiState.Loading)
    val messagesState: StateFlow<UiState<List<ChatMessage>>> = _messagesState.asStateFlow()

    private val _sendMessageState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val sendMessageState: StateFlow<UiState<String>> = _sendMessageState.asStateFlow()

    private val _otherUserState = MutableStateFlow<UiState<com.example.gooddeedfeed.data.remote.dto.UserDto>>(UiState.Idle)
    val otherUserState: StateFlow<UiState<com.example.gooddeedfeed.data.remote.dto.UserDto>> = _otherUserState.asStateFlow()

    private val _messageChannel = Channel<ChatMessage>()
    val messageFlow = _messageChannel.receiveAsFlow()

    private val json = Json { ignoreUnknownKeys = true }

    // Current chat state for real-time updates
    private var currentChatUserId: Int? = null
    private var currentUser: DomainUser? = null
    private var isPeriodicRefreshActive = false
    private var isWebSocketConnected = false
    private var connectedUserId: Int? = null // Track which user ID the WebSocket is connected for

    init {
        // Listen for message notifications to refresh chat in real-time
        Log.d(TAG, "🔔 CHAT INIT: Setting up notification event listener")
        viewModelScope.launch {
            notificationEventBus.messageNotificationEvents.collect { event ->
                Log.d(TAG, "🔔 CHAT EVENT: Received notification event: $event")
                when (event) {
                    is MessageNotificationEvent.NewMessage -> {
                        handleNewMessageNotification(event)
                    }
                }
            }
        }
        
        // Listen for WebSocket messages
        viewModelScope.launch {
            Log.d(TAG, "🔔 CHAT INIT: Starting WebSocket message flow listener")
            try {
                messageFlow.collect { message ->
                    Log.d(TAG, "🔔 CHAT INIT: ✅ Received message from flow: ${message.content}")
                    handleWebSocketMessage(message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "🔔 CHAT INIT: ❌ Error in message flow: $e")
            }
        }
    }

    private fun handleWebSocketMessage(message: ChatMessage) {
        Log.d(TAG, "📱 WEBSOCKET HANDLER: Processing real-time message")
        Log.d(TAG, "📱 WEBSOCKET HANDLER: Message details - ID: ${message.id}, Content: '${message.content}', Sender: ${message.senderId}, Receiver: ${message.receiverId}")
        
        currentUser?.let { user ->
            Log.d(TAG, "📱 WEBSOCKET HANDLER: Current user ID: ${user.id}, Current chat user ID: $currentChatUserId")
            
            // Update messages if this message is for the current chat
            val isForCurrentChat = (message.senderId == currentChatUserId && message.receiverId == user.id) ||
                (message.senderId == user.id && message.receiverId == currentChatUserId)
            
            Log.d(TAG, "📱 WEBSOCKET HANDLER: Is message for current chat? $isForCurrentChat")
            
            if (isForCurrentChat) {
                val currentMessages = (_messagesState.value as? UiState.Success)?.data ?: emptyList()
                Log.d(TAG, "📱 WEBSOCKET HANDLER: Current message count before adding: ${currentMessages.size}")
                
                val newMessage = message.copy(
                    isFromCurrentUser = message.senderId == user.id
                )
                
                Log.d(TAG, "📱 WEBSOCKET HANDLER: Updated message isFromCurrentUser: ${newMessage.isFromCurrentUser}")
                
                _messagesState.value = UiState.Success(currentMessages + newMessage)
                Log.d(TAG, "📱 WEBSOCKET HANDLER: ✅ Added real-time message to current chat. New count: ${currentMessages.size + 1}")
            } else {
                Log.d(TAG, "📱 WEBSOCKET HANDLER: Message not for current chat, skipping UI update")
            }
            
            // Always refresh conversations to update unread counts, but only for WebSocket messages
            // to avoid duplicate calls when both WebSocket and Firebase notification arrive
            Log.d(TAG, "📱 WEBSOCKET HANDLER: Refreshing conversations for unread count update")
            loadConversations(user)
        } ?: run {
            Log.e(TAG, "📱 WEBSOCKET HANDLER: ❌ Current user is null, cannot process message")
        }
    }

    private fun handleNewMessageNotification(event: MessageNotificationEvent.NewMessage) {
        currentUser?.let { user ->
            Log.d(TAG, "📱 NOTIFICATION: Processing Firebase notification from ${event.senderName}")
            
            val isForCurrentChat = (event.senderId == currentChatUserId && event.receiverId == user.id) ||
                (event.senderId == user.id && event.receiverId == currentChatUserId)
            
            if (isForCurrentChat) {
                // For current chat, don't refresh messages (WebSocket handles that)
                // but do refresh conversations after a delay to update unread counts correctly
                Log.d(TAG, "📱 NOTIFICATION: Message for current chat - WebSocket handles message display")
            } else {
                Log.d(TAG, "📱 NOTIFICATION: Message not for current chat")
            }
            
            // Note: TabNavigation will handle conversation refresh with delay to avoid conflicts
        }
    }

    fun loadConversations(currentUser: DomainUser) {
        viewModelScope.launch {
            try {
                _conversationsState.value = UiState.Loading

                val sessionId = getSessionId()
                if (sessionId.isNullOrEmpty()) {
                    _conversationsState.value = UiState.Error("No authentication session found")
                    return@launch
                }

                chatApiService.withFallbackUrls { baseUrl ->
                    httpClient.get("$baseUrl/conversations") {
                        header("Authorization", "Bearer $sessionId")
                    }
                }.let { response ->
                    if (response.status.value == 401) {
                        Log.w(TAG, "Session expired or invalid - authentication required")
                        _conversationsState.value = UiState.Error("Session expired. Please log in again.")
                        return@launch
                    }
                    val conversations = response.body<List<ChatConversation>>()

                    // Apply user preferences for starring conversations (previously deleted conversations will now
                    // re-appear in the list if new messages arrive, addressing the issue where volunteers sometimes
                    // could not see ongoing chats).
                    val visibleConversations = mutableListOf<ChatConversation>()

                    for (conversation in conversations) {
                        val isDeleted = conversationPreferencesRepository.isConversationDeleted(
                            currentUser.id,
                            conversation.id,
                            conversation.lastMessage,
                        )

                        if (!isDeleted) {
                            val isStarred = conversationPreferencesRepository.isConversationStarred(currentUser.id, conversation.id)
                            visibleConversations.add(conversation.copy(isStarred = isStarred))
                        }
                    }

                    _conversationsState.value = UiState.Success(visibleConversations)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversations", e)
                val errorMessage = when {
                    e.message?.contains("401") == true -> "Session expired. Please log in again."
                    e.message?.contains("Invalid or expired session") == true -> "Session expired. Please log in again."
                    else -> "Failed to load conversations: ${e.message}"
                }
                _conversationsState.value = UiState.Error(errorMessage)
            }
        }
    }

    fun loadMessages(otherUserId: Int, currentUser: DomainUser) {
        // Update current chat state for real-time updates
        this.currentChatUserId = otherUserId
        this.currentUser = currentUser
        refreshMessages(otherUserId, currentUser, showLoading = true)
        
        // Global messaging service handles WebSocket connections
        // Reduce polling frequency since we have real-time updates from global service
        startPeriodicRefresh()
    }

    fun clearCurrentChat() {
        Log.d(TAG, "📱 CHAT CLEAR: Clearing current chat state")
        
        // Refresh conversations one final time to ensure unread counts are accurate
        // This helps fix nav bar indicator not updating when leaving chat
        currentUser?.let { user ->
            Log.d(TAG, "📱 CHAT CLEAR: Final conversation refresh to update nav bar indicators")
            loadConversations(user)
        }
        
        currentChatUserId = null
        currentUser = null
        // Don't disconnect WebSocket here as user might still be in chat list
        stopPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        if (isPeriodicRefreshActive) return

        isPeriodicRefreshActive = true
        viewModelScope.launch {
            while (isPeriodicRefreshActive && currentChatUserId != null && currentUser != null) {
                delay(120_000) // Refresh every 2 minutes as fallback (reduced from 30s since we have WebSocket)
                currentChatUserId?.let { otherUserId ->
                    currentUser?.let { user ->
                        Log.d(TAG, "🔄 PERIODIC REFRESH: Fallback refresh for messages")
                        refreshMessages(otherUserId, user, showLoading = false)
                    }
                }
            }
        }
    }

    private fun stopPeriodicRefresh() {
        isPeriodicRefreshActive = false
    }

    fun refreshMessages(otherUserId: Int, currentUser: DomainUser, showLoading: Boolean = false) {
        viewModelScope.launch {
            try {
                if (showLoading) {
                    _messagesState.value = UiState.Loading
                }

                val sessionId = getSessionId()
                if (sessionId.isNullOrEmpty()) {
                    _messagesState.value = UiState.Error("No authentication session found")
                    return@launch
                }

                chatApiService.withFallbackUrls { baseUrl ->
                    httpClient.get("$baseUrl/messages/$otherUserId") {
                        header("Authorization", "Bearer $sessionId")
                    }
                }.let { response ->
                    val messages = response.body<List<MessageDto>>()
                    val chatMessages = messages.map { msg ->
                        ChatMessage(
                            id = msg.id.toString(),
                            content = msg.content,
                            senderName = if (msg.sender_id == currentUser.id) {
                                currentUser.fullName ?: currentUser.username
                            } else {
                                msg.sender_username.ifBlank { msg.receiver_username }
                            },
                            senderType = if (msg.sender_id == currentUser.id) currentUser.userType?.name?.lowercase() ?: "volunteer" else "organizer",
                            timestamp = formatTimestamp(msg.created_at),
                            isFromCurrentUser = msg.sender_id == currentUser.id,
                            senderId = msg.sender_id,
                            receiverId = msg.receiver_id,
                        )
                    }
                    _messagesState.value = UiState.Success(chatMessages)

                    // Refresh conversations to update unread counts
                    loadConversations(currentUser)
                    
                    // Also do a delayed refresh to catch server-side read status updates
                    viewModelScope.launch {
                        delay(1000L) // Wait 1 second for server to process read status
                        Log.d(TAG, "📱 DELAYED REFRESH: Updating conversations after server processing")
                        loadConversations(currentUser)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load messages", e)
                if (showLoading) {
                    _messagesState.value = UiState.Error("Failed to load messages: ${e.message}")
                }
            }
        }
    }

    fun sendMessage(content: String, receiverId: Int, currentUser: DomainUser) {
        viewModelScope.launch {
            try {
                _sendMessageState.value = UiState.Loading

                val sessionId = getSessionId()
                if (sessionId.isNullOrEmpty()) {
                    _sendMessageState.value = UiState.Error("No authentication session found")
                    return@launch
                }

                val request = SendMessageRequest(
                    receiver_id = receiverId,
                    content = content,
                )

                chatApiService.withFallbackUrls { baseUrl ->
                    httpClient.post("$baseUrl/messages") {
                        contentType(ContentType.Application.Json)
                        header("Authorization", "Bearer $sessionId")
                        setBody(request)
                    }
                }.let { response ->
                    val sentMessage = response.body<MessageDto>()
                    _sendMessageState.value = UiState.Success("Message sent successfully")

                    val currentMessages = (_messagesState.value as? UiState.Success)?.data ?: emptyList()
                    val newMessage = ChatMessage(
                        id = sentMessage.id.toString(),
                        content = sentMessage.content,
                        senderName = currentUser.fullName ?: currentUser.username,
                        senderType = currentUser.userType?.name?.lowercase() ?: "volunteer",
                        timestamp = formatTimestamp(sentMessage.created_at),
                        isFromCurrentUser = true,
                        senderId = currentUser.id,
                        receiverId = receiverId,
                    )

                    _messagesState.value = UiState.Success(currentMessages + newMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                _sendMessageState.value = UiState.Error("Failed to send message: ${e.message}")
            }
        }
    }

    fun connectToWebSocket(userId: Int, currentUser: DomainUser) {
        Log.d(TAG, "📱 WEBSOCKET INIT: Starting WebSocket connection for user $userId")
        
        if (isWebSocketConnected && connectedUserId == userId) {
            Log.d(TAG, "📱 WEBSOCKET INIT: WebSocket already connected for user $userId, skipping")
            return
        }
        
        if (isWebSocketConnected && connectedUserId != userId) {
            Log.d(TAG, "📱 WEBSOCKET INIT: WebSocket connected for different user ($connectedUserId), disconnecting first")
            isWebSocketConnected = false
            connectedUserId = null
        }
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "📱 WEBSOCKET INIT: Getting session ID for authentication")
                val sessionId = getSessionId()
                if (sessionId.isNullOrEmpty()) {
                    Log.e(TAG, "📱 WEBSOCKET INIT: ❌ No authentication token for WebSocket connection")
                    return@launch
                }
                Log.d(TAG, "📱 WEBSOCKET INIT: ✅ Session ID obtained: ${sessionId.take(10)}...")

                chatApiService.withFallbackUrls { baseUrl ->
                    val wsUrl = baseUrl.replace("http", "ws").replace("https", "wss")
                    val fullWsUrl = "$wsUrl/ws/chat/$userId"
                    Log.d(TAG, "📱 WEBSOCKET INIT: Attempting connection to: $fullWsUrl")
                    
                    try {
                        httpClient.webSocket(fullWsUrl) {
                            isWebSocketConnected = true
                            connectedUserId = userId
                            Log.d(TAG, "📱 WEBSOCKET CONNECT: ✅ Connected successfully for user $userId")
                            
                            // Send ping periodically to keep connection alive
                            val pingJob = launch {
                                var pingCount = 0
                                while (isWebSocketConnected) {
                                    try {
                                        val pingMessage = """{"type":"ping"}"""
                                        send(Frame.Text(pingMessage))
                                        pingCount++
                                        Log.d(TAG, "📱 WEBSOCKET PING: Sent ping #$pingCount to user $userId")
                                        delay(30000) // ping every 30 seconds
                                    } catch (e: Exception) {
                                        Log.e(TAG, "📱 WEBSOCKET PING: ❌ Failed to send ping: $e")
                                        break
                                    }
                                }
                                Log.d(TAG, "📱 WEBSOCKET PING: Ping loop ended for user $userId")
                            }
                            
                            // Listen for incoming messages
                            Log.d(TAG, "📱 WEBSOCKET LISTEN: Starting to listen for incoming messages")
                            try {
                                for (frame in incoming) {
                                    Log.d(TAG, "📱 WEBSOCKET LISTEN: Received frame type: ${frame.javaClass.simpleName}")
                                    
                                    if (frame is Frame.Text) {
                                        val messageText = frame.readText()
                                        Log.d(TAG, "📱 WEBSOCKET LISTEN: Received text message: $messageText")
                                        
                                        try {
                                            val jsonElement = json.parseToJsonElement(messageText)
                                            val data = jsonElement.jsonObject
                                            Log.d(TAG, "📱 WEBSOCKET LISTEN: Parsed message data: $data")
                                            
                                            val messageType = data["type"]?.jsonPrimitive?.content
                                            when (messageType) {
                                                "connection_established" -> {
                                                    Log.d(TAG, "📱 WEBSOCKET LISTEN: ✅ Connection confirmation received")
                                                }
                                                "pong" -> {
                                                    Log.d(TAG, "📱 WEBSOCKET LISTEN: Received pong response")
                                                }
                                                "new_message", "test_message" -> {
                                                    Log.d(TAG, "📱 WEBSOCKET LISTEN: 📨 $messageType received!")
                                                    val messageData = data["message"]?.jsonObject
                                                    if (messageData != null) {
                                                        Log.d(TAG, "📱 WEBSOCKET LISTEN: Processing message data: $messageData")
                                                        
                                                        val senderId = try { messageData["sender_id"]?.jsonPrimitive?.int ?: 0 } catch (e: Exception) { 0 }
                                                        val receiverId = try { messageData["receiver_id"]?.jsonPrimitive?.int ?: 0 } catch (e: Exception) { 0 }
                                                        Log.d(TAG, "📱 WEBSOCKET LISTEN: Parsed IDs - sender: $senderId, receiver: $receiverId")
                                                        
                                                        val message = ChatMessage(
                                                            id = messageData["id"]?.jsonPrimitive?.content ?: "",
                                                            content = messageData["content"]?.jsonPrimitive?.content ?: "",
                                                            senderName = messageData["sender_name"]?.jsonPrimitive?.content ?: "",
                                                            senderType = messageData["sender_type"]?.jsonPrimitive?.content ?: "",
                                                            timestamp = formatTimestamp(messageData["timestamp"]?.jsonPrimitive?.content ?: ""),
                                                            senderId = senderId,
                                                            receiverId = receiverId,
                                                            isFromCurrentUser = false
                                                        )
                                                        Log.d(TAG, "📱 WEBSOCKET LISTEN: Created ChatMessage: $message")
                                                        _messageChannel.send(message)
                                                        Log.d(TAG, "📱 WEBSOCKET LISTEN: ✅ Message sent to channel")
                                                    } else {
                                                        Log.e(TAG, "📱 WEBSOCKET LISTEN: ❌ Message data is null")
                                                    }
                                                }
                                                else -> {
                                                    Log.d(TAG, "📱 WEBSOCKET LISTEN: Unknown message type: $messageType")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "📱 WEBSOCKET LISTEN: ❌ Failed to parse message: $messageText", e)
                                        }
                                    } else {
                                        Log.d(TAG, "📱 WEBSOCKET LISTEN: Received non-text frame: $frame")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "📱 WEBSOCKET LISTEN: ❌ Error in message listening loop: $e")
                            }
                            
                            // Cancel ping job when connection ends
                            pingJob.cancel()
                            Log.d(TAG, "📱 WEBSOCKET LISTEN: Message listening ended for user $userId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "📱 WEBSOCKET CONNECT: ❌ Failed to establish WebSocket connection: $e")
                        throw e
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "📱 WEBSOCKET INIT: ❌ WebSocket connection failed completely: $e")
                isWebSocketConnected = false
            } finally {
                isWebSocketConnected = false
                connectedUserId = null
                Log.d(TAG, "📱 WEBSOCKET INIT: Connection cleanup completed for user $userId")
            }
        }
    }

    fun loadOtherUser(otherId: Int) {
        viewModelScope.launch {
            try {
                _otherUserState.value = UiState.Loading
                val dto = authApiService.getUserById(otherId)
                _otherUserState.value = UiState.Success(dto)
            } catch (e: Exception) {
                _otherUserState.value = UiState.Error("Failed to load user: ${e.message}")
            }
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

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val time = timestamp.substringAfter("T").substringBefore(".")
            val parts = time.split(":")
            "${parts[0]}:${parts[1]}"
        } catch (e: Exception) {
            getCurrentTimestamp()
        }
    }

    private fun getCurrentTimestamp(): String {
        val currentTime = System.currentTimeMillis()
        val hours = (currentTime / (1000 * 60 * 60)) % 24
        val minutes = (currentTime / (1000 * 60)) % 60
        return "$hours:${minutes.toString().padStart(2, '0')}"
    }

    fun clearSendMessageState() {
        _sendMessageState.value = UiState.Idle
    }

    fun deleteConversation(conversation: ChatConversation, currentUser: DomainUser) {
        viewModelScope.launch {
            try {
                // Store the deletion preference along with snapshot of last message
                conversationPreferencesRepository.deleteConversation(
                    currentUser.id,
                    conversation.id,
                    conversation.lastMessage,
                )

                // Update the UI state by filtering out the deleted conversation
                val currentConversations = (_conversationsState.value as? UiState.Success)?.data ?: return@launch
                val updatedConversations = currentConversations.filter { it.id != conversation.id }
                _conversationsState.value = UiState.Success(updatedConversations)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete conversation", e)
            }
        }
    }

    fun toggleConversationStar(conversationId: String, currentUser: DomainUser) {
        viewModelScope.launch {
            try {
                // Toggle the star preference and get the new state
                val isStarred = conversationPreferencesRepository.toggleConversationStar(currentUser.id, conversationId)

                // Update the UI state
                val currentConversations = (_conversationsState.value as? UiState.Success)?.data ?: return@launch
                val updatedConversations = currentConversations.map { conversation ->
                    if (conversation.id == conversationId) {
                        conversation.copy(isStarred = isStarred)
                    } else {
                        conversation
                    }
                }
                _conversationsState.value = UiState.Success(updatedConversations)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle conversation star", e)
            }
        }
    }
}

// Removed obsolete MessageResponse; now using MessageDto from data.remote.dto
