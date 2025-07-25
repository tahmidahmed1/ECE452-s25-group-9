package com.example.gooddeedfeed.presentation.viewmodel.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.repository.LocationSettingsRepository
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.repository.NotificationRepository
import com.example.gooddeedfeed.domain.repository.OpportunitiesRepository
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeData(
    val user: DomainUser,
    val userTypeDisplay: UserTypeDisplay,
)

data class UserTypeDisplay(
    val title: String,
    val subtitle: String,
    val actionItems: List<HomeActionItem>,
)

data class HomeActionItem(
    val iconName: String,
    val title: String,
    val description: String,
    val action: HomeAction,
)

sealed class HomeAction {
    object BrowseOpportunities : HomeAction()
    object ViewMyActivities : HomeAction()
    object LostAndFound : HomeAction()
    object CreateEvent : HomeAction()
    object ManageEvents : HomeAction()
    object Chat : HomeAction()
    object ViewDashboard : HomeAction()
    object ManagePrograms : HomeAction()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val locationSettingsRepository: LocationSettingsRepository,
    private val notificationRepository: NotificationRepository,
    private val opportunitiesRepository: OpportunitiesRepository,
) : ViewModel() {
    // Opportunity Idea Generator State
    private val _ideaSuggestions = MutableStateFlow<List<String>>(emptyList())
    val ideaSuggestions: StateFlow<List<String>> = _ideaSuggestions.asStateFlow()

    private val _isGeneratingIdeas = MutableStateFlow(false)
    val isGeneratingIdeas: StateFlow<Boolean> = _isGeneratingIdeas.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeData>> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<HomeAction>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<HomeAction> = _navigationEvent.asSharedFlow()

    private val _joinedEventsState = MutableStateFlow<UiState<List<VolunteerOpportunity>>>(UiState.Idle)
    val joinedEventsState: StateFlow<UiState<List<VolunteerOpportunity>>> = _joinedEventsState.asStateFlow()

    /**
     * Generate creative volunteer opportunity ideas and update state.
     */
    fun generateOpportunityIdeas() {
        viewModelScope.launch {
            _isGeneratingIdeas.value = true
            try {
                opportunitiesRepository.generateOpportunityIdeas()
                    .onSuccess { ideas -> 
                        _ideaSuggestions.value = ideas
                    }
                    .onFailure { e -> 
                        _uiState.value = UiState.Error("Failed to generate ideas: ${e.message}")
                    }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to generate ideas: ${e.message}")
            } finally {
                _isGeneratingIdeas.value = false
            }
        }
    }

    fun resetOpportunityIdeas() {
        _ideaSuggestions.value = emptyList()
        _isGeneratingIdeas.value = false
    }

    fun loadUserHome(user: DomainUser) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val userTypeDisplay = createUserTypeDisplay(user)
                _uiState.value = UiState.Success(
                    HomeData(
                        user = user,
                        userTypeDisplay = userTypeDisplay,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load home screen: ${e.message}")
            }
        }
    }

    fun handleAction(action: HomeAction) {
        _navigationEvent.tryEmit(action)
    }

    suspend fun hasNotificationPromptBeenShown(): Boolean {
        return locationSettingsRepository.hasNotificationPromptBeenShown.first()
    }

    fun markNotificationPromptAsShown() {
        viewModelScope.launch {
            locationSettingsRepository.setNotificationPromptShown(true)
        }
    }

    fun enableNotifications() {
        viewModelScope.launch {
            notificationRepository.setNotificationsEnabled(true)
            locationSettingsRepository.setNotificationsEnabled(true)
        }
    }

    fun disableNotifications() {
        viewModelScope.launch {
            notificationRepository.setNotificationsEnabled(false)
            locationSettingsRepository.setNotificationsEnabled(false)
        }
    }

    fun loadJoinedEvents() {
        viewModelScope.launch {
            try {
                android.util.Log.i("HomeViewModel", "📅 LOAD JOINED EVENTS - Starting to load joined events")
                _joinedEventsState.value = UiState.Loading
                opportunitiesRepository.getJoinedEvents().collect { events ->
                    android.util.Log.i("HomeViewModel", "📅 LOAD JOINED EVENTS - Received ${events.size} joined events from repository")
                    events.forEach { event ->
                        android.util.Log.i("HomeViewModel", "  📋 Event: '${event.title}' (ID: ${event.id}) on ${event.date} - isJoined: ${event.isJoined}")
                    }
                    _joinedEventsState.value = UiState.Success(events)
                    android.util.Log.i("HomeViewModel", "✅ LOAD JOINED EVENTS - Updated UI state with ${events.size} events")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ LOAD JOINED EVENTS - Failed to load joined events", e)
                _joinedEventsState.value = UiState.Error("Failed to load joined events: ${e.message}")
            }
        }
    }

    fun refreshJoinedEvents() {
        viewModelScope.launch {
            try {
                android.util.Log.i("HomeViewModel", "🔄 REFRESH JOINED EVENTS - Starting to refresh joined events")
                // Force refresh by getting a fresh flow
                opportunitiesRepository.getJoinedEvents().collect { events ->
                    android.util.Log.i("HomeViewModel", "🔄 REFRESH JOINED EVENTS - Received ${events.size} joined events from repository")
                    events.forEach { event ->
                        android.util.Log.i("HomeViewModel", "  📋 Refreshed Event: '${event.title}' (ID: ${event.id}) on ${event.date} - isJoined: ${event.isJoined}")
                    }
                    _joinedEventsState.value = UiState.Success(events)
                    android.util.Log.i("HomeViewModel", "✅ REFRESH JOINED EVENTS - Updated UI state with ${events.size} events")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ REFRESH JOINED EVENTS - Failed to refresh joined events", e)
                _joinedEventsState.value = UiState.Error("Failed to refresh joined events: ${e.message}")
            }
        }
    }

    private fun createUserTypeDisplay(user: DomainUser): UserTypeDisplay {
        return when (user.userType) {
            DomainUserType.VOLUNTEER -> UserTypeDisplay(
                title = "Welcome, Volunteer!",
                subtitle = "Find ways to give back to your community!",
                actionItems = listOf(
                    HomeActionItem(
                        iconName = "search",
                        title = "Browse Opportunities",
                        description = "Discover volunteer opportunities near you",
                        action = HomeAction.BrowseOpportunities,
                    ),
                    HomeActionItem(
                        iconName = "history",
                        title = "My Activities",
                        description = "Track your volunteer history and hours",
                        action = HomeAction.ViewMyActivities,
                    ),
                    HomeActionItem(
                        iconName = "search",
                        title = "Lost & Found",
                        description = "Report lost items or browse found items",
                        action = HomeAction.LostAndFound,
                    ),
                ),
            )
            DomainUserType.ORGANIZER -> UserTypeDisplay(
                title = "Welcome, Organizer!",
                subtitle = "Manage your events and connect with volunteers!",
                actionItems = listOf(
                    HomeActionItem(
                        iconName = "add_circle",
                        title = "Create Event",
                        description = "Post a new volunteer opportunity",
                        action = HomeAction.CreateEvent,
                    ),
                    HomeActionItem(
                        iconName = "event",
                        title = "Manage Events",
                        description = "View and edit your posted events",
                        action = HomeAction.ManageEvents,
                    ),
                    HomeActionItem(
                        iconName = "chat",
                        title = "Messages",
                        description = "View messages with volunteers",
                        action = HomeAction.Chat,
                    ),
                ),
            )
            null -> UserTypeDisplay(
                title = "Loading...",
                subtitle = "",
                actionItems = listOf(
                    HomeActionItem(
                        iconName = "progress",
                        title = "Loading",
                        description = "Please wait while we load your data",
                        action = HomeAction.ManageEvents,
                    ),
                ),
            )
        }
    }
} 
