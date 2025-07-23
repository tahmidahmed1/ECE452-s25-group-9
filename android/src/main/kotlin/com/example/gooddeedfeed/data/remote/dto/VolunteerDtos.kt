package com.example.gooddeedfeed.data.remote.dto

import com.example.gooddeedfeed.domain.model.JoinedVolunteer
import kotlinx.serialization.Serializable

@Serializable
data class VolunteerDto(
    val id: Int,
    val username: String? = null,
    val full_name: String? = null,
    val profile_picture_url: String? = null,
)

@Serializable
data class EventVolunteersResponseDto(
    val volunteers: List<VolunteerDto>,
    val total_count: Int,
)

fun VolunteerDto.toDomain(): JoinedVolunteer = JoinedVolunteer(
    id = id,
    username = username ?: "",
    fullName = full_name ?: username ?: "",
    profilePictureUrl = profile_picture_url,
) 