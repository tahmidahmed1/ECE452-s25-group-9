package com.example.gooddeedfeed.data.mapper

import com.example.gooddeedfeed.data.remote.dto.*
import com.example.gooddeedfeed.domain.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// DTO to Domain mappers
fun MessageReactionDto.toDomain(): MessageReaction = MessageReaction(
    id = id,
    messageId = message_id,
    userId = user_id,
    username = username,
    emoji = emoji,
    createdAt = LocalDateTime.parse(created_at, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
)

fun MessageDto.toDomain(currentUserId: Int): DomainMessage = DomainMessage(
    id = id,
    senderId = sender_id,
    receiverId = receiver_id,
    content = content,
    senderUsername = sender_username,
    receiverUsername = receiver_username,
    isRead = is_read,
    isImportantSender = is_important_sender,
    isImportantReceiver = is_important_receiver,
    isDeletedSender = is_deleted_sender,
    isDeletedReceiver = is_deleted_receiver,
    reactions = reactions.map { it.toDomain() },
    createdAt = LocalDateTime.parse(created_at, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    isFromCurrentUser = sender_id == currentUserId
)

fun ChatSummaryDto.toDomain(): ChatSummary = ChatSummary(
    otherUserId = other_user_id,
    otherUserUsername = other_user_username,
    otherUserFullName = other_user_full_name,
    otherUserProfilePicture = other_user_profile_picture,
    latestMessage = latest_message,
    latestMessageTime = LocalDateTime.parse(latest_message_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    unreadCount = unread_count,
    isImportant = is_important
)

// Factory methods for DTOs
fun MessageCreateDto.Companion.create(receiverId: Int, content: String): MessageCreateDto = 
    MessageCreateDto(
        receiver_id = receiverId,
        content = content
    )

fun MessageReactionCreateDto.Companion.create(emoji: String): MessageReactionCreateDto = 
    MessageReactionCreateDto(emoji = emoji)

fun MessageUpdateImportantDto.Companion.create(isImportant: Boolean): MessageUpdateImportantDto = 
    MessageUpdateImportantDto(is_important = isImportant)

fun MessageUpdateDeletedDto.Companion.create(isDeleted: Boolean): MessageUpdateDeletedDto = 
    MessageUpdateDeletedDto(is_deleted = isDeleted)