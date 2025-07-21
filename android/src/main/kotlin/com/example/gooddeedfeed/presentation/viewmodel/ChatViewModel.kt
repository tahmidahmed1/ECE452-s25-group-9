package com.example.gooddeedfeed.presentation.viewmodel

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.remote.ChatApiService
import com.example.gooddeedfeed.domain.model.DomainUser
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
    private val dataStore: DataStore<Preferences>,
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

    private val _messageChannel = Channel<ChatMessage>()
    val messageFlow = _messageChannel.receiveAsFlow()

    private val json = Json { ignoreUnknownKeys = true }

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
                    _conversationsState.value = UiState.Success(conversations)
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
        viewModelScope.launch {
            try {
                _messagesState.value = UiState.Loading

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
                    val messages = response.body<List<MessageResponse>>()
                    val chatMessages = messages.map { msg ->
                        ChatMessage(
                            id = msg.id.toString(),
                            content = msg.content,
                            senderName = if (msg.sender_id == currentUser.id) currentUser.fullName ?: currentUser.username else "Other User",
                            senderType = if (msg.sender_id == currentUser.id) currentUser.userType?.name?.lowercase() ?: "volunteer" else "organizer",
                            timestamp = formatTimestamp(msg.created_at),
                            isFromCurrentUser = msg.sender_id == currentUser.id,
                            senderId = msg.sender_id,
                            receiverId = msg.receiver_id,
                        )
                    }
                    _messagesState.value = UiState.Success(chatMessages)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load messages", e)
                _messagesState.value = UiState.Error("Failed to load messages: ${e.message}")
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
                    val sentMessage = response.body<MessageResponse>()
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

    fun connectToWebSocket(roomId: Int, currentUser: DomainUser) {
        viewModelScope.launch {
            try {
                val sessionId = getSessionId()
                if (sessionId.isNullOrEmpty()) {
                    Log.e(TAG, "No authentication token for WebSocket connection")
                    return@launch
                }

                chatApiService.withFallbackUrls { baseUrl ->
                    val wsUrl = baseUrl.replace("http", "ws")
                    httpClient.webSocket("$wsUrl/ws/chat/$roomId") {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val messageText = frame.readText()
                                try {
                                    val message = json.decodeFromString<ChatMessage>(messageText)
                                    _messageChannel.send(message)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to parse websocket message", e)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket connection failed", e)
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
}

@Serializable
data class MessageResponse(
    val id: Int,
    val content: String,
    val sender_id: Int,
    val receiver_id: Int,
    val created_at: String,
) 
