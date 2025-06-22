package com.example.gooddeedfeed.presentation.viewmodel.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    object CreateEvent : HomeAction()
    object ManageEvents : HomeAction()
    object ViewDashboard : HomeAction()
    object ManagePrograms : HomeAction()
}

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeData>> = _uiState.asStateFlow()

    // Navigation events for home actions to switch bottom tabs
    private val _navigationEvent = MutableSharedFlow<HomeAction>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<HomeAction> = _navigationEvent.asSharedFlow()

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
        // Emit navigation event for bottom bar switching
        _navigationEvent.tryEmit(action)
    }

    private fun createUserTypeDisplay(user: DomainUser): UserTypeDisplay {
        return when (user.userType) {
            DomainUserType.VOLUNTEER -> UserTypeDisplay(
                title = "Welcome, Volunteer!",
                subtitle = "Find meaningful ways to give back to your community",
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
                ),
            )
            DomainUserType.ORGANIZER -> UserTypeDisplay(
                title = "Welcome, Organizer!",
                subtitle = "Manage your volunteer events and connect with volunteers",
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
                ),
            )
            DomainUserType.INSTITUTION -> UserTypeDisplay(
                title = "Welcome, Institution!",
                subtitle = "Review and validate volunteer activities",
                actionItems = listOf(
                    HomeActionItem(
                        iconName = "dashboard",
                        title = "Review Dashboard",
                        description = "Review pending volunteer activities",
                        action = HomeAction.ViewDashboard,
                    ),
                    HomeActionItem(
                        iconName = "business",
                        title = "Manage Programs",
                        description = "Oversee volunteer programs and organizations",
                        action = HomeAction.ManagePrograms,
                    ),
                ),
            )
            else -> UserTypeDisplay(
                title = "Welcome!",
                subtitle = "Discover volunteer opportunities",
                actionItems = listOf(
                    HomeActionItem(
                        iconName = "explore",
                        title = "Explore",
                        description = "Discover volunteer opportunities",
                        action = HomeAction.BrowseOpportunities,
                    ),
                ),
            )
        }
    }
} 
