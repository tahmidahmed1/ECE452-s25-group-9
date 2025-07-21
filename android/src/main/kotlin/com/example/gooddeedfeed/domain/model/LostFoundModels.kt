package com.example.gooddeedfeed.domain.model

data class DomainLostFoundItem(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val type: DomainLostFoundType,
    val images: List<String>,
    val contactName: String,
    val isResolved: Boolean = false,
    val reward: String? = null,
    val tags: List<String> = emptyList(),
    val expiryDate: String? = null,
    val daysRemaining: Int? = null,
)

enum class DomainLostFoundType {
    LOST, FOUND
}