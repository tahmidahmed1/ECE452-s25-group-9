package com.example.gooddeedfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OpportunitiesUiState {
    object Loading : OpportunitiesUiState()
    data class Success(
        val opportunities: List<VolunteerOpportunity>,
        val categories: List<OpportunityCategory>,
        val selectedCategory: OpportunityCategory?,
        val isMapView: Boolean
    ) : OpportunitiesUiState()
    data class Error(val message: String) : OpportunitiesUiState()
}

@HiltViewModel
class OpportunitiesViewModel @Inject constructor(
    // TODO: Inject OpportunitiesRepository when created
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<OpportunitiesUiState>(OpportunitiesUiState.Loading)
    val uiState: StateFlow<OpportunitiesUiState> = _uiState.asStateFlow()
    
    init {
        loadOpportunities()
    }
    
    fun loadOpportunities() {
        viewModelScope.launch {
            try {
                _uiState.value = OpportunitiesUiState.Loading
                
                // TODO: Replace with actual repository call
                val mockOpportunities = getMockOpportunities()
                val categories = OpportunityCategory.values().toList()
                
                _uiState.value = OpportunitiesUiState.Success(
                    opportunities = mockOpportunities,
                    categories = categories,
                    selectedCategory = null,
                    isMapView = false
                )
            } catch (e: Exception) {
                _uiState.value = OpportunitiesUiState.Error("Failed to load opportunities")
            }
        }
    }
    
    fun filterByCategory(category: OpportunityCategory?) {
        val currentState = _uiState.value
        if (currentState is OpportunitiesUiState.Success) {
            val filteredOpportunities = if (category != null) {
                getMockOpportunities().filter { it.category == category }
            } else {
                getMockOpportunities()
            }
            
            _uiState.value = currentState.copy(
                opportunities = filteredOpportunities,
                selectedCategory = category
            )
        }
    }
    
    fun toggleView() {
        val currentState = _uiState.value
        if (currentState is OpportunitiesUiState.Success) {
            _uiState.value = currentState.copy(
                isMapView = !currentState.isMapView
            )
        }
    }
    
    fun joinOpportunity(opportunityId: Int) {
        viewModelScope.launch {
            try {
                // TODO: Implement with repository
                // opportunitiesRepository.joinOpportunity(opportunityId)
            } catch (e: Exception) {
                // TODO: Handle error with proper error state or toast
            }
        }
    }
    
    // TODO: Replace with actual repository data
    private fun getMockOpportunities(): List<VolunteerOpportunity> {
        return listOf(
            VolunteerOpportunity(
                id = 1,
                title = "Community Garden Cleanup",
                organizationName = "Green City Initiative",
                location = "Central Park",
                date = "March 15, 2024",
                description = "Help maintain and beautify our community garden spaces.",
                requiredVolunteers = 20,
                currentVolunteers = 12,
                category = OpportunityCategory.ENVIRONMENTAL
            ),
            VolunteerOpportunity(
                id = 2,
                title = "Food Bank Volunteer",
                organizationName = "City Food Bank",
                location = "Downtown Center",
                date = "March 18, 2024",
                description = "Assist with food sorting and distribution to families in need.",
                requiredVolunteers = 15,
                currentVolunteers = 8,
                category = OpportunityCategory.SOCIAL_SERVICES
            ),
            VolunteerOpportunity(
                id = 3,
                title = "Reading Program Assistant",
                organizationName = "Library Foundation",
                location = "Public Library",
                date = "March 20, 2024",
                description = "Help children with reading activities and homework support.",
                requiredVolunteers = 10,
                currentVolunteers = 5,
                category = OpportunityCategory.EDUCATION
            )
        )
    }
} 