package com.example.gooddeedfeed.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gooddeedfeed.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatApiService @Inject constructor(
    client: HttpClient,
    private val dataStore: DataStore<Preferences>,
) : BaseApiService(client) {

    companion object {
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
    }

    private suspend fun getSessionId(): String? {
        return try {
            dataStore.data.first()[SESSION_ID_KEY]
        } catch (e: Exception) {
            null
        }
    }

    // Get conversations with enhanced data
    suspend fun getEnhancedConversations(): List<ChatSummaryDto> {
        return withFallbackUrls { baseUrl ->
            val sessionId = getSessionId()
                ?: throw Exception("No authentication session found")

            client.get("$baseUrl/conversations/enhanced") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionId")
            }.body()
        }
    }

    // Get messages with a specific user
    suspend fun getMessagesWithUser(otherUserId: Int): List<MessageDto> {
        return withFallbackUrls { baseUrl ->
            val sessionId = getSessionId()
                ?: throw Exception("No authentication session found")

            client.get("$baseUrl/messages/$otherUserId") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionId")
            }.body()
        }
    }

    // Send a message
    suspend fun sendMessage(messageCreate: MessageCreateDto): MessageDto {
        return withFallbackUrls { baseUrl ->
            val sessionId = getSessionId()
                ?: throw Exception("No authentication session found")

            client.post("$baseUrl/messages") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionId")
                setBody(messageCreate)
            }.body()
        }
    }

    // Add reaction to a message
    suspend fun addMessageReaction(messageId: Int, reaction: MessageReactionCreateDto): MessageReactionDto {
        return withFallbackUrls { baseUrl ->
            val sessionId = getSessionId()
                ?: throw Exception("No authentication session found")

            client.post("$baseUrl/messages/$messageId/reactions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionId")
                setBody(reaction)
            }.body()
        }
    }

    // Remove reaction from a message
    suspend fun removeMessageReaction(messageId: Int, reactionId: Int) {
        withFallbackUrls { baseUrl ->
            val sessionId = getSessionId()
                ?: throw Exception("No authentication session found")

            client.delete("$baseUrl/messages/$messageId/reactions/$reactionId") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionId")
            }
        }
    }

    // Toggle message importance
    suspend fun toggleMessageImportant(messageId: Int, update: MessageUpdateImportantDto) {
        withFallbackUrls { baseUrl ->
            val sessionId = getSessionId()
                ?: throw Exception("No authentication session found")

            client.patch("$baseUrl/messages/$messageId/important") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionId")
                setBody(update)
            }
        }
    }

    // Toggle message deleted (soft delete)
    suspend fun toggleMessageDeleted(messageId: Int, update: MessageUpdateDeletedDto) {
        withFallbackUrls { baseUrl ->
            val sessionId = getSessionId()
                ?: throw Exception("No authentication session found")

            client.patch("$baseUrl/messages/$messageId/delete") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionId")
                setBody(update)
            }
        }
    }

    // Get unread messages count
    suspend fun getUnreadMessagesCount(): UnreadCountDto {
        return withFallbackUrls { baseUrl ->
            val sessionId = getSessionId()
                ?: throw Exception("No authentication session found")

            client.get("$baseUrl/messages/unread/count") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionId")
            }.body()
        }
    }
} 
