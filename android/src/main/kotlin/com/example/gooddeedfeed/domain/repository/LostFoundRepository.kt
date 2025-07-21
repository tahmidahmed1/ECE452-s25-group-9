package com.example.gooddeedfeed.domain.repository

import com.example.gooddeedfeed.domain.model.DomainLostFoundItem
import com.example.gooddeedfeed.domain.model.DomainLostFoundType
import kotlinx.coroutines.flow.Flow
import java.io.File

interface LostFoundRepository {
    suspend fun getLostFoundItems(
        filterType: DomainLostFoundType? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): Flow<List<DomainLostFoundItem>>

    suspend fun getLostFoundItem(itemId: String): Result<DomainLostFoundItem>

    suspend fun createLostFoundItem(
        title: String,
        description: String,
        location: String,
        itemType: DomainLostFoundType,
        reward: String?,
        tags: List<String>,
        expiryDays: Int,
    ): Result<DomainLostFoundItem>

    suspend fun updateLostFoundItem(
        itemId: String,
        title: String? = null,
        description: String? = null,
        location: String? = null,
        reward: String? = null,
        tags: List<String>? = null,
        isResolved: Boolean? = null,
    ): Result<DomainLostFoundItem>

    suspend fun deleteLostFoundItem(itemId: String): Result<Unit>

    suspend fun uploadImage(itemId: String, imageFile: File): Result<String>

    suspend fun deleteImage(itemId: String, imageId: String): Result<Unit>
}
