package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntryDto(
    val id: Int,
    val username: String,
    val full_name: String? = null,
    val karma_points: Int,
    val profile_picture_url: String? = null,
    val user_type: String? = null,
    val rank: Int,
)

@Serializable
data class LeaderboardResponseDto(
    val entries: List<LeaderboardEntryDto>,
    val page: Int,
    val page_size: Int,
    val total_pages: Int,
    val total_entries: Int,
    val has_next: Boolean,
    val has_previous: Boolean,
) 
