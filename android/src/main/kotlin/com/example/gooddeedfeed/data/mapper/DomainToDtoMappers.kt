package com.example.gooddeedfeed.data.mapper

import com.example.gooddeedfeed.data.remote.dto.*
import com.example.gooddeedfeed.domain.model.*
import com.example.gooddeedfeed.domain.util.toPrettyDate

fun SocialMediaPlatform.toDto(): SocialMediaPlatformDto = when (this) {
    SocialMediaPlatform.INSTAGRAM -> SocialMediaPlatformDto.INSTAGRAM
    SocialMediaPlatform.FACEBOOK -> SocialMediaPlatformDto.FACEBOOK
    SocialMediaPlatform.TWITTER -> SocialMediaPlatformDto.TWITTER
    SocialMediaPlatform.LINKEDIN -> SocialMediaPlatformDto.LINKEDIN
}

fun SocialMediaLink.toDto(): SocialMediaLinkDto = SocialMediaLinkDto(
    platform = platform.toDto(),
    url = url,
)

fun DomainSex.toDto(): Sex = when (this) {
    DomainSex.MALE -> Sex.MALE
    DomainSex.FEMALE -> Sex.FEMALE
    DomainSex.NON_BINARY -> Sex.NON_BINARY
    DomainSex.PREFER_NOT_TO_SAY -> Sex.PREFER_NOT_TO_SAY
}

fun DomainUserType.toDto(): UserType = when (this) {
    DomainUserType.VOLUNTEER -> UserType.VOLUNTEER
    DomainUserType.ORGANIZER -> UserType.ORGANIZER
}

fun LostFoundItemDto.toDomain(): DomainLostFoundItem {
    try {
        android.util.Log.d("LostFoundMapper", "Mapping DTO id=$id with images=${images?.joinToString()}")
    } catch (_: Throwable) {}

    return DomainLostFoundItem(
        id = id.toString(),
        userId = userId.toString(),
        title = title,
        description = description,
        location = location,
        date = createdAt,
        type = if (itemType == "lost") {
            DomainLostFoundType.LOST
        } else {
            DomainLostFoundType.FOUND
        },
        images = images?.map {
            val mapped = it.toEmulatorAccessibleUrl()
            try { android.util.Log.d("LostFoundMapper", "  image: $it -> $mapped") } catch (_: Throwable) {}
            mapped
        } ?: emptyList(),
        contactName = contactName ?: "Unknown",
        contactPhone = null, // backend may send later
        contactEmail = null,
        isResolved = isResolved,
        reward = reward,
        tags = tags ?: emptyList(),
        expiryDate = expiresAt,
        daysRemaining = daysRemaining,
    )
}

fun DomainLostFoundType.toApiString(): String = when (this) {
    DomainLostFoundType.LOST -> "lost"
    DomainLostFoundType.FOUND -> "found"
}

fun DomainLostFoundItem.toPresentationModel(): com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundItem {
    return com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundItem(
        id = id,
        userId = userId,
        title = title,
        description = description,
        location = location,
        date = date.toPrettyDate(),
        type = if (type == DomainLostFoundType.LOST) {
            com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.LOST
        } else {
            com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.FOUND
        },
        images = images.map { it.toEmulatorAccessibleUrl() },
        contactName = contactName,
        contactPhone = contactPhone,
        contactEmail = contactEmail,
        isResolved = isResolved,
        reward = reward,
        tags = tags,
        expiryDate = expiryDate,
        daysRemaining = daysRemaining,
    )
}

fun com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.toDomain(): DomainLostFoundType = when (this) {
    com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.LOST -> DomainLostFoundType.LOST
    com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.FOUND -> DomainLostFoundType.FOUND
} 
