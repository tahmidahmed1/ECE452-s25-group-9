package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventImageDto(
    val id: Int,
    val event_id: Int,
    val image_url: String,
    val is_main: Boolean = false,
    val display_order: Int = 0,
    val created_at: String? = null,
) 