package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.repository.SubscriptionRepository
import com.example.gooddeedfeed.domain.model.DomainOrganizerWithSubscriptionStatus
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SubscriptionViewModel"
    }

    private val _uiState = MutableStateFlow<UiState<List<DomainOrganizerWithSubscriptionStatus>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<DomainOrganizerWithSubscriptionStatus>>> = _uiState.asStateFlow()

    private val _subscriptionActionState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val subscriptionActionState: StateFlow<UiState<String>> = _subscriptionActionState.asStateFlow()

    fun searchOrganizers(query: String) {
        Log.d(TAG, "🔍 Searching organizers with query: $query")
        _uiState.value = UiState.Loading
        
        viewModelScope.launch {
            subscriptionRepository.getOrganizersWithSubscriptionStatus(query)
                .onSuccess { organizers ->
                    Log.d(TAG, "✅ Successfully loaded organizers: ${organizers.size}")
                    _uiState.value = UiState.Success(organizers)
                }
                .onFailure { exception ->
                    Log.e(TAG, "❌ Failed to load organizers", exception)
                    _uiState.value = UiState.Error("Failed to load organizers: ${exception.message}")
                }
        }
    }

    fun subscribeToOrganizer(organizerId: Int) {
        Log.d(TAG, "📝 Subscribing to organizer: $organizerId")
        _subscriptionActionState.value = UiState.Loading
        
        viewModelScope.launch {
            subscriptionRepository.subscribeToOrganizer(organizerId)
                .onSuccess { response ->
                    Log.d(TAG, "✅ Successfully subscribed to organizer: $organizerId")
                    _subscriptionActionState.value = UiState.Success(response.message)
                    
                    // Update the current organizer list to reflect the subscription change
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        val updatedOrganizers = currentState.data.map { organizer ->
                            if (organizer.id == organizerId) {
                                organizer.copy(
                                    isSubscribed = response.isSubscribed,
                                    subscriberCount = if (response.isSubscribed) organizer.subscriberCount + 1 else organizer.subscriberCount
                                )
                            } else {
                                organizer
                            }
                        }
                        _uiState.value = UiState.Success(updatedOrganizers)
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "❌ Failed to subscribe to organizer: $organizerId", exception)
                    _subscriptionActionState.value = UiState.Error("Failed to subscribe: ${exception.message}")
                }
        }
    }

    fun unsubscribeFromOrganizer(organizerId: Int) {
        Log.d(TAG, "🗑️ Unsubscribing from organizer: $organizerId")
        _subscriptionActionState.value = UiState.Loading
        
        viewModelScope.launch {
            subscriptionRepository.unsubscribeFromOrganizer(organizerId)
                .onSuccess { response ->
                    Log.d(TAG, "✅ Successfully unsubscribed from organizer: $organizerId")
                    _subscriptionActionState.value = UiState.Success(response.message)
                    
                    // Update the current organizer list to reflect the subscription change
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        val updatedOrganizers = currentState.data.map { organizer ->
                            if (organizer.id == organizerId) {
                                organizer.copy(
                                    isSubscribed = response.isSubscribed,
                                    subscriberCount = if (!response.isSubscribed) maxOf(0, organizer.subscriberCount - 1) else organizer.subscriberCount
                                )
                            } else {
                                organizer
                            }
                        }
                        _uiState.value = UiState.Success(updatedOrganizers)
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "❌ Failed to unsubscribe from organizer: $organizerId", exception)
                    _subscriptionActionState.value = UiState.Error("Failed to unsubscribe: ${exception.message}")
                }
        }
    }

    fun getUserSubscriptions() {
        Log.d(TAG, "📋 Getting user subscriptions")
        
        viewModelScope.launch {
            subscriptionRepository.getUserSubscriptions()
                .onSuccess { subscriptions ->
                    Log.d(TAG, "✅ Successfully loaded user subscriptions: ${subscriptions.size}")
                    // Handle subscriptions if needed
                }
                .onFailure { exception ->
                    Log.e(TAG, "❌ Failed to load user subscriptions", exception)
                }
        }
    }

    fun clearSubscriptionActionState() {
        _subscriptionActionState.value = UiState.Idle
    }
} 