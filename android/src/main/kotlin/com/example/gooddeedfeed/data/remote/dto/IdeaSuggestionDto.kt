package com.example.gooddeedfeed.data.remote.dto

import kotlinx.serialization.Serializable
 
@Serializable
data class IdeaSuggestionDto(
    val ideas: List<String>
) 