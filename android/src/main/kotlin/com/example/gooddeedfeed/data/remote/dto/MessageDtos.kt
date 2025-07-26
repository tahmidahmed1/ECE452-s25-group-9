package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageReactionDto(
    val id: Int,
    val message_id: Int,
    val user_id: Int,
    val username: String,
    val emoji: String,
    val created_at: String,
)

@Serializable
data class MessageDto(
    val id: Int,
    val sender_id: Int,
    val receiver_id: Int,
    val content: String,
    val sender_username: String,
    val receiver_username: String,
    val is_read: Boolean,
    val is_important_sender: Boolean,
    val is_important_receiver: Boolean,
    val is_deleted_sender: Boolean,
    val is_deleted_receiver: Boolean,
    val reactions: List<MessageReactionDto> = emptyList(),
    val created_at: String,
)

@Serializable
data class ChatSummaryDto(
    val other_user_id: Int,
    val other_user_username: String,
    val other_user_full_name: String?,
    val other_user_profile_picture: String?,
    val latest_message: String,
    val latest_message_time: String,
    val unread_count: Int,
    val is_important: Boolean,
)

@Serializable
data class MessageCreateDto(
    val receiver_id: Int,
    val content: String,
) {
    companion object
}

@Serializable
data class MessageReactionCreateDto(
    val emoji: String,
) {
    companion object
}

@Serializable
data class MessageUpdateImportantDto(
    val is_important: Boolean,
) {
    companion object
}

@Serializable
data class MessageUpdateDeletedDto(
    val is_deleted: Boolean,
) {
    companion object
}

@Serializable
data class UnreadCountDto(
    val unread_count: Int,
)
