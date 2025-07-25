package com.example.gooddeedfeed.domain.model

import java.time.LocalDateTime

data class MessageReaction(
    val id: Int,
    val messageId: Int,
    val userId: Int,
    val username: String,
    val emoji: String,
    val createdAt: LocalDateTime
)

data class DomainMessage(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val senderUsername: String,
    val receiverUsername: String,
    val isRead: Boolean,
    val isImportantSender: Boolean,
    val isImportantReceiver: Boolean,
    val isDeletedSender: Boolean,
    val isDeletedReceiver: Boolean,
    val reactions: List<MessageReaction>,
    val createdAt: LocalDateTime,
    val isFromCurrentUser: Boolean = false
)

data class ChatSummary(
    val otherUserId: Int,
    val otherUserUsername: String,
    val otherUserFullName: String?,
    val otherUserProfilePicture: String?,
    val latestMessage: String,
    val latestMessageTime: LocalDateTime,
    val unreadCount: Int,
    val isImportant: Boolean
)

// For UI state
data class MessageUiState(
    val message: DomainMessage,
    val showReactionPicker: Boolean = false,
    val isSelected: Boolean = false
)

// Available emoji reactions
object MessageEmojis {
    const val HEART = "❤️"
    const val THUMBS_UP = "👍"
    const val THUMBS_DOWN = "👎"
    const val HAPPY = "😄"
    const val SAD = "😢"
    const val SURPRISED = "😮"
    
    val ALL = listOf(HEART, THUMBS_UP, THUMBS_DOWN, HAPPY, SAD, SURPRISED)
}