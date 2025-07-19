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
