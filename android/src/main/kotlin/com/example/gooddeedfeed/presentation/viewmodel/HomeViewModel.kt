package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.data.remote.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val user: User,
        val userTypeDisplay: UserTypeDisplay
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

data class UserTypeDisplay(
    val title: String,
    val subtitle: String,
    val actionItems: List<HomeActionItem>
)

data class HomeActionItem(
    val iconName: String,
    val title: String,
    val description: String,
    val action: HomeAction
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
class HomeViewModel @Inject constructor(
    // TODO: Inject repositories when they're created
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun loadUserHome(user: User) {
        viewModelScope.launch {
            try {
                val userTypeDisplay = createUserTypeDisplay(user)
                _uiState.value = HomeUiState.Success(
                    user = user,
                    userTypeDisplay = userTypeDisplay
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Failed to load home screen")
            }
        }
    }
    
    fun handleAction(action: HomeAction) {
        viewModelScope.launch {
            // TODO: Handle navigation actions through navigation events
            when (action) {
                HomeAction.BrowseOpportunities -> {
                    // TODO: Navigate to opportunities screen
                }
                HomeAction.ViewMyActivities -> {
                    // TODO: Navigate to activities screen
                }
                HomeAction.CreateEvent -> {
                    // TODO: Navigate to create event screen
                }
                HomeAction.ManageEvents -> {
                    // TODO: Navigate to manage events screen
                }
                HomeAction.ViewDashboard -> {
                    // TODO: Navigate to dashboard screen
                }
                HomeAction.ManagePrograms -> {
                    // TODO: Navigate to programs screen
                }
            }
        }
    }
    
    private fun createUserTypeDisplay(user: User): UserTypeDisplay {
        return when (user.user_type) {
            UserType.VOLUNTEER -> createVolunteerDisplay(user)
            UserType.ORGANIZER -> createOrganizerDisplay(user)
            UserType.INSTITUTION -> createInstitutionDisplay(user)
            null -> createDefaultDisplay()
        }
    }
    
    private fun createVolunteerDisplay(user: User): UserTypeDisplay {
        return UserTypeDisplay(
            title = "Find opportunities to help your community!",
            subtitle = "Volunteer",
            actionItems = listOf(
                HomeActionItem(
                    iconName = "favorite",
                    title = "Browse Opportunities",
                    description = "Find volunteer opportunities near you",
                    action = HomeAction.BrowseOpportunities
                ),
                HomeActionItem(
                    iconName = "list",
                    title = "My Activities",
                    description = "View your volunteer history",
                    action = HomeAction.ViewMyActivities
                )
            )
        )
    }
    
    private fun createOrganizerDisplay(user: User): UserTypeDisplay {
        return UserTypeDisplay(
            title = "Manage your events and volunteers",
            subtitle = "Organizer${user.organization_name?.let { " • $it" } ?: ""}",
            actionItems = listOf(
                HomeActionItem(
                    iconName = "star",
                    title = "Create Event",
                    description = "Organize a new volunteer opportunity",
                    action = HomeAction.CreateEvent
                ),
                HomeActionItem(
                    iconName = "list",
                    title = "Manage Events",
                    description = "View and edit your events",
                    action = HomeAction.ManageEvents
                )
            )
        )
    }
    
    private fun createInstitutionDisplay(user: User): UserTypeDisplay {
        return UserTypeDisplay(
            title = "Coordinate institutional volunteer programs",
            subtitle = "Institution${user.institution_name?.name?.replace("_", " ")?.let { " • $it" } ?: ""}",
            actionItems = listOf(
                HomeActionItem(
                    iconName = "info",
                    title = "Dashboard",
                    description = "View volunteer program analytics",
                    action = HomeAction.ViewDashboard
                ),
                HomeActionItem(
                    iconName = "list",
                    title = "Manage Programs",
                    description = "Oversee institutional volunteer programs",
                    action = HomeAction.ManagePrograms
                )
            )
        )
    }
    
    private fun createDefaultDisplay(): UserTypeDisplay {
        return UserTypeDisplay(
            title = "Please complete your profile setup",
            subtitle = "User",
            actionItems = emptyList()
        )
    }
} 