package com.example.gooddeedfeed.data.repository

import android.util.Log
import com.example.gooddeedfeed.data.mapper.toApiString
import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.remote.LostFoundApiService
import com.example.gooddeedfeed.data.remote.dto.CreateLostFoundItemDto
import com.example.gooddeedfeed.data.remote.dto.UpdateLostFoundItemDto
import com.example.gooddeedfeed.domain.model.DomainLostFoundItem
import com.example.gooddeedfeed.domain.model.DomainLostFoundType
import com.example.gooddeedfeed.domain.repository.LostFoundRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LostFoundRepositoryImpl @Inject constructor(
    private val apiService: LostFoundApiService,
) : LostFoundRepository {

    companion object {
        private const val TAG = "LostFoundRepository"
    }

    override suspend fun getLostFoundItems(
        filterType: DomainLostFoundType?,
        limit: Int,
        offset: Int,
    ): Flow<List<DomainLostFoundItem>> = flow {
        Log.d(TAG, "🔄 Repository: Getting lost and found items")
        Log.d(TAG, "📝 Repository params - filterType: $filterType, limit: $limit, offset: $offset")
        try {
            val response = apiService.getLostFoundItems(
                itemType = filterType?.toApiString(),
                limit = limit,
                offset = offset,
            )
            Log.d(TAG, "✅ Repository: API call successful, got ${response.items.size} items, total: ${response.totalCount}")
            val domainItems = response.items.map { it.toDomain() }
            Log.d(TAG, "🔄 Repository: Converted to ${domainItems.size} domain items")
            emit(domainItems)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository: Failed to get lost and found items", e)
            emit(emptyList())
        }
    }

    override suspend fun getLostFoundItem(itemId: String): Result<DomainLostFoundItem> {
        return try {
            val itemDto = apiService.getLostFoundItem(itemId.toInt())
            Result.success(itemDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createLostFoundItem(
        title: String,
        description: String,
        location: String,
        itemType: DomainLostFoundType,
        reward: String?,
        tags: List<String>,
        expiryDays: Int,
        imageFiles: List<File>,
    ): Result<DomainLostFoundItem> {
        Log.d(TAG, "🚀 Repository: Creating lost and found item")
        Log.d(TAG, "📝 Repository data - title: $title, type: $itemType, location: $location")
        Log.d(TAG, "📝 Repository data - description: $description, reward: $reward, expiryDays: $expiryDays")
        Log.d(TAG, "📝 Repository data - tags: $tags, imageFiles: ${imageFiles.size} files")

        return try {
            val createDto = CreateLostFoundItemDto(
                title = title,
                description = description,
                location = location,
                itemType = itemType.toApiString(),
                reward = reward,
                tags = tags,
                expiryDays = expiryDays,
            )

            Log.d(TAG, "🔄 Repository: Created DTO with itemType: ${createDto.itemType}")

            val itemDto = apiService.createLostFoundItem(createDto)
            Log.d(TAG, "✅ Repository: API call successful, created item with ID: ${itemDto.id}")

            // Upload images if any were provided
            if (imageFiles.isNotEmpty()) {
                Log.d(TAG, "📸 Repository: Uploading ${imageFiles.size} images for item ${itemDto.id}")
                var successfulUploads = 0

                for ((index, imageFile) in imageFiles.withIndex()) {
                    try {
                        val uploadResult = uploadImage(itemDto.id.toString(), imageFile)
                        uploadResult.fold(
                            onSuccess = { imageUrl ->
                                Log.d(TAG, "✅ Repository: Successfully uploaded image ${index + 1}/${imageFiles.size}: $imageUrl")
                                successfulUploads++
                            },
                            onFailure = { error ->
                                Log.e(TAG, "❌ Repository: Failed to upload image ${index + 1}/${imageFiles.size}", error)
                            },
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Repository: Exception uploading image ${index + 1}/${imageFiles.size}", e)
                    }
                }

                Log.d(TAG, "📸 Repository: Uploaded $successfulUploads/${imageFiles.size} images successfully")

                // Fetch the updated item to get the image URLs
                try {
                    val updatedItemResult = getLostFoundItem(itemDto.id.toString())
                    updatedItemResult.fold(
                        onSuccess = { updatedItem ->
                            Log.d(TAG, "✅ Repository: Retrieved updated item with ${updatedItem.images.size} images")
                            return Result.success(updatedItem)
                        },
                        onFailure = { error ->
                            Log.w(TAG, "⚠️ Repository: Failed to fetch updated item, returning original", error)
                        },
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Repository: Exception fetching updated item, returning original", e)
                }
            }

            val domainItem = itemDto.toDomain()
            Log.d(TAG, "✅ Repository: Converted to domain item, returning success")
            Result.success(domainItem)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository: Failed to create lost and found item", e)
            Log.e(TAG, "🔍 Repository exception details: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateLostFoundItem(
        itemId: String,
        title: String?,
        description: String?,
        location: String?,
        reward: String?,
        tags: List<String>?,
        isResolved: Boolean?,
    ): Result<DomainLostFoundItem> {
        return try {
            val updateDto = UpdateLostFoundItemDto(
                title = title,
                description = description,
                location = location,
                reward = reward,
                tags = tags,
                isResolved = isResolved,
            )

            val itemDto = apiService.updateLostFoundItem(itemId.toInt(), updateDto)
            Result.success(itemDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLostFoundItem(itemId: String): Result<Unit> {
        return try {
            apiService.deleteLostFoundItem(itemId.toInt())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(itemId: String, imageFile: File): Result<String> {
        return try {
            val uploadResponse = apiService.uploadLostFoundImage(itemId.toInt(), imageFile)
            Result.success(uploadResponse.imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteImage(itemId: String, imageId: String): Result<Unit> {
        return try {
            apiService.deleteLostFoundImage(itemId.toInt(), imageId.toInt())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
