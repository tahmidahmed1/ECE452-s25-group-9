package com.example.gooddeedfeed.data.mapper

import com.example.gooddeedfeed.data.remote.dto.*
import com.example.gooddeedfeed.domain.model.*

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

fun LostFoundItemDto.toDomain(): com.example.gooddeedfeed.domain.model.DomainLostFoundItem {
    return com.example.gooddeedfeed.domain.model.DomainLostFoundItem(
        id = id.toString(),
        title = title,
        description = description,
        location = location,
        date = createdAt,
        type = if (itemType == "lost") com.example.gooddeedfeed.domain.model.DomainLostFoundType.LOST 
              else com.example.gooddeedfeed.domain.model.DomainLostFoundType.FOUND,
        images = images,
        contactName = contactName,
        isResolved = isResolved,
        reward = reward,
        tags = tags ?: emptyList(),
        expiryDate = expiresAt,
        daysRemaining = daysRemaining
    )
}

fun com.example.gooddeedfeed.domain.model.DomainLostFoundType.toApiString(): String = when (this) {
    com.example.gooddeedfeed.domain.model.DomainLostFoundType.LOST -> "lost"
    com.example.gooddeedfeed.domain.model.DomainLostFoundType.FOUND -> "found"
}

fun com.example.gooddeedfeed.domain.model.DomainLostFoundItem.toPresentationModel(): com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundItem {
    return com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundItem(
        id = id,
        title = title,
        description = description,
        location = location,
        date = date,
        type = if (type == com.example.gooddeedfeed.domain.model.DomainLostFoundType.LOST) 
               com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.LOST
               else com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.FOUND,
        images = images,
        contactName = contactName,
        isResolved = isResolved,
        reward = reward,
        tags = tags,
        expiryDate = expiryDate,
        daysRemaining = daysRemaining
    )
}

fun com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.toDomain(): com.example.gooddeedfeed.domain.model.DomainLostFoundType = when (this) {
    com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.LOST -> com.example.gooddeedfeed.domain.model.DomainLostFoundType.LOST
    com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType.FOUND -> com.example.gooddeedfeed.domain.model.DomainLostFoundType.FOUND
} 
