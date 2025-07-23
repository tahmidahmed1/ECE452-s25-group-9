package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LostFoundItemDto(
    @SerialName("id") val id: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("location") val location: String = "",
    @SerialName("item_type") val itemType: String = "lost",
    @SerialName("reward") val reward: String? = null,
    @SerialName("tags") val tags: List<String>? = null,
    @SerialName("expiry_days") val expiryDays: Int = 7,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_resolved") val isResolved: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("images") val images: List<String>? = null,
    @SerialName("days_remaining") val daysRemaining: Int? = null,
)

@Serializable
data class LostFoundItemsResponseDto(
    @SerialName("items") val items: List<LostFoundItemDto>,
    @SerialName("total_count") val totalCount: Int,
)

@Serializable
data class CreateLostFoundItemDto(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("location") val location: String,
    @SerialName("item_type") val itemType: String,
    @SerialName("reward") val reward: String?,
    @SerialName("tags") val tags: List<String>?,
    @SerialName("expiry_days") val expiryDays: Int,
)

@Serializable
data class UpdateLostFoundItemDto(
    @SerialName("title") val title: String?,
    @SerialName("description") val description: String?,
    @SerialName("location") val location: String?,
    @SerialName("reward") val reward: String?,
    @SerialName("tags") val tags: List<String>?,
    @SerialName("is_resolved") val isResolved: Boolean?,
)

@Serializable
data class CreateLostFoundResponseDto(
    @SerialName("id") val id: Int,
    @SerialName("message") val message: String? = null,
)

@Serializable
data class ErrorResponseDto(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String,
)

@Serializable
data class LostFoundImageUploadResponseDto(
    @SerialName("message") val message: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("image_id") val imageId: Int,
)
