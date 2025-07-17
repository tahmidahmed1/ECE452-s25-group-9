package com.example.gooddeedfeed.domain.model

data class DomainLeaderboardEntry(
    val id: Int,
    val username: String,
    val fullName: String? = null,
    val karmaPoints: Int,
    val profilePictureUrl: String? = null,
    val userType: DomainUserType? = null,
    val rank: Int
)

data class DomainLeaderboardResponse(
    val entries: List<DomainLeaderboardEntry>,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalEntries: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) 