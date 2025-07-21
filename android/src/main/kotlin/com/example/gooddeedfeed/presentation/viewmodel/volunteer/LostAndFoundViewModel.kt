package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.mapper.toPresentationModel
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LostFoundItem(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val type: LostFoundType,
    val images: List<String>,
    val contactName: String,
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
    private val lostFoundRepository: com.example.gooddeedfeed.domain.repository.LostFoundRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LostAndFoundViewModel"
    }

    private val _uiState = MutableStateFlow<UiState<List<LostFoundItem>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<LostFoundItem>>> = _uiState.asStateFlow()

    private val _createItemState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createItemState: StateFlow<UiState<Unit>> = _createItemState.asStateFlow()

    fun loadItems(filterType: LostFoundType? = null) {
        Log.d(TAG, "🔄 ViewModel: Loading lost and found items")
        Log.d(TAG, "📝 ViewModel filter: $filterType")
        
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                lostFoundRepository.getLostFoundItems(filterType?.toDomain()).collect { domainItems ->
                    Log.d(TAG, "✅ ViewModel: Received ${domainItems.size} domain items from repository")
                    val presentationItems = domainItems.map { it.toPresentationModel() }
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
                val result = lostFoundRepository.createLostFoundItem(
                    title = title,
                    description = description,
                    location = location,
                    itemType = itemType.toDomain(),
                    reward = reward,
                    tags = tags,
                    expiryDays = expiryDays
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
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ ViewModel: Exception during create item", e)
                _createItemState.value = UiState.Error(e.message ?: "Failed to create item")
            }
        }
    }

    fun clearCreateItemState() {
        _createItemState.value = UiState.Idle
    }
} 
