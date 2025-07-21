package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    // TODO: Inject LostFoundRepository when created
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<LostFoundItem>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<LostFoundItem>>> = _uiState.asStateFlow()

    private val _createItemState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createItemState: StateFlow<UiState<Unit>> = _createItemState.asStateFlow()

    fun loadItems(filterType: LostFoundType? = null) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                // TODO: Replace with real API call
                // For now, return empty list to remove mock data
                _uiState.value = UiState.Success(emptyList())
            } catch (e: Exception) {
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
        images: List<String>
    ) {
        viewModelScope.launch {
            _createItemState.value = UiState.Loading

            try {
                // TODO: Replace with real API call
                _createItemState.value = UiState.Success(Unit)
                
                // Refresh items list
                loadItems()
            } catch (e: Exception) {
                _createItemState.value = UiState.Error(e.message ?: "Failed to create item")
            }
        }
    }

    fun clearCreateItemState() {
        _createItemState.value = UiState.Idle
    }
} 