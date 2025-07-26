package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.mapper.toPresentationModel
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class LostFoundItem(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val type: LostFoundType,
    val images: List<String>,
    val contactName: String,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val isResolved: Boolean = false,
    val reward: String? = null,
    val tags: List<String> = emptyList(),
    val expiryDate: String? = null,
    val daysRemaining: Int? = null,
)

enum class LostFoundType {
    LOST, FOUND
}

@HiltViewModel
class LostAndFoundViewModel @Inject constructor(
    private val lostFoundRepository: com.example.gooddeedfeed.domain.repository.LostFoundRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "LostAndFoundViewModel"
    }

    private val _uiState = MutableStateFlow<UiState<List<LostFoundItem>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<LostFoundItem>>> = _uiState.asStateFlow()

    private val _createItemState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createItemState: StateFlow<UiState<Unit>> = _createItemState.asStateFlow()

    private val _updateItemState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateItemState: StateFlow<UiState<Unit>> = _updateItemState.asStateFlow()

    private val _deleteItemState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteItemState: StateFlow<UiState<Unit>> = _deleteItemState.asStateFlow()

    fun loadItems(filterType: LostFoundType? = null) {
        Log.d(TAG, "🔄 ViewModel: Loading lost and found items")
        Log.d(TAG, "📝 ViewModel filter: $filterType")

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                lostFoundRepository.getLostFoundItems(filterType?.toDomain()).collect { domainItems ->
                    Log.d(TAG, "✅ ViewModel: Received ${domainItems.size} domain items from repository")
                    val presentationItems = domainItems.map { it.toPresentationModel() }
                    presentationItems.forEach { itm ->
                        Log.d(TAG, "Presentation item id=${itm.id} has images=${itm.images}")
                    }
                    Log.d(TAG, "🔄 ViewModel: Converted to ${presentationItems.size} presentation items")
                    _uiState.value = UiState.Success(presentationItems)
                    Log.d(TAG, "✅ ViewModel: Set UI state to Success with ${presentationItems.size} items")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ ViewModel: Failed to load items", e)
                _uiState.value = UiState.Error(e.message ?: "Failed to load items")
            }
        }
    }

    fun createItem(
        title: String,
        description: String,
        location: String,
        itemType: LostFoundType,
        reward: String?,
        tags: List<String>,
        expiryDays: Int,
        images: List<String>,
    ) {
        Log.d(TAG, "🚀 ViewModel: Creating lost and found item")
        Log.d(TAG, "📝 ViewModel data - title: $title, type: $itemType, location: $location")
        Log.d(TAG, "📝 ViewModel data - description: $description, reward: $reward, expiryDays: $expiryDays")
        Log.d(TAG, "📝 ViewModel data - tags: $tags, images: ${images.size} images")

        viewModelScope.launch {
            _createItemState.value = UiState.Loading

            try {
                // Convert image URIs to files using existing ImageUtils
                val imageFiles = mutableListOf<File>()
                for ((index, imageUri) in images.withIndex()) {
                    try {
                        val uri = Uri.parse(imageUri)
                        val file = ImageUtils.saveUriToFile(context, uri)
                        if (file != null) {
                            imageFiles.add(file)
                            Log.d(TAG, "✅ Converted image URI to file: ${file.name}")
                        } else {
                            Log.w(TAG, "⚠️ Failed to convert image URI to file: $imageUri")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error processing image URI: $imageUri", e)
                    }
                }

                Log.d(TAG, "📸 ViewModel: Converted ${imageFiles.size}/${images.size} images to files")

                val result = lostFoundRepository.createLostFoundItem(
                    title = title,
                    description = description,
                    location = location,
                    itemType = itemType.toDomain(),
                    reward = reward,
                    tags = tags,
                    expiryDays = expiryDays,
                    imageFiles = imageFiles,
                )

                result.fold(
                    onSuccess = { createdItem ->
                        Log.d(TAG, "✅ ViewModel: Successfully created item with ID: ${createdItem.id}")
                        _createItemState.value = UiState.Success(Unit)
                        Log.d(TAG, "🔄 ViewModel: Reloading items after creation")
                        loadItems()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ ViewModel: Failed to create item", error)
                        _createItemState.value = UiState.Error(error.message ?: "Failed to create item")
                    },
                )

                // Clean up temporary files
                imageFiles.forEach { file ->
                    try {
                        if (file.exists()) {
                            file.delete()
                            Log.d(TAG, "🧹 Cleaned up temp file: ${file.name}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to clean up temp file: ${file.name}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ ViewModel: Exception during create item", e)
                _createItemState.value = UiState.Error(e.message ?: "Failed to create item")
            }
        }
    }

    fun clearCreateItemState() {
        _createItemState.value = UiState.Idle
    }

    fun updateItem(
        itemId: String,
        title: String,
        description: String,
        location: String,
        reward: String?,
        tags: List<String>,
        isResolved: Boolean,
    ) {
        Log.d(TAG, "📝 ViewModel: Updating lost and found item with ID: $itemId")
        Log.d(TAG, "📝 Update data - title: $title, location: $location, isResolved: $isResolved")

        viewModelScope.launch {
            _updateItemState.value = UiState.Loading

            try {
                val result = lostFoundRepository.updateLostFoundItem(
                    itemId = itemId,
                    title = title,
                    description = description,
                    location = location,
                    reward = reward,
                    tags = tags,
                    isResolved = isResolved,
                )

                result.fold(
                    onSuccess = { updatedItem ->
                        Log.d(TAG, "✅ ViewModel: Successfully updated item with ID: ${updatedItem.id}")
                        _updateItemState.value = UiState.Success(Unit)
                        Log.d(TAG, "🔄 ViewModel: Reloading items after update")
                        loadItems()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ ViewModel: Failed to update item", error)
                        _updateItemState.value = UiState.Error(error.message ?: "Failed to update item")
                    },
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ ViewModel: Exception during update item", e)
                _updateItemState.value = UiState.Error(e.message ?: "Failed to update item")
            }
        }
    }

    fun deleteItem(itemId: String) {
        Log.d(TAG, "🗑️ ViewModel: Deleting lost and found item with ID: $itemId")

        viewModelScope.launch {
            _deleteItemState.value = UiState.Loading

            try {
                val result = lostFoundRepository.deleteLostFoundItem(itemId)

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "✅ ViewModel: Successfully deleted item with ID: $itemId")
                        _deleteItemState.value = UiState.Success(Unit)
                        Log.d(TAG, "🔄 ViewModel: Reloading items after deletion")
                        loadItems()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ ViewModel: Failed to delete item", error)
                        _deleteItemState.value = UiState.Error(error.message ?: "Failed to delete item")
                    },
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ ViewModel: Exception during delete item", e)
                _deleteItemState.value = UiState.Error(e.message ?: "Failed to delete item")
            }
        }
    }

    fun clearUpdateItemState() {
        _updateItemState.value = UiState.Idle
    }

    fun clearDeleteItemState() {
        _deleteItemState.value = UiState.Idle
    }
} 
