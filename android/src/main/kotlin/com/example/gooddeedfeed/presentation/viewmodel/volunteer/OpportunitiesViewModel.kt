package com.example.gooddeedfeed.presentation.viewmodel.volunteer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.usecase.volunteer.ApplyForOpportunityUseCase
import com.example.gooddeedfeed.domain.usecase.volunteer.GetOpportunitiesUseCase
import com.example.gooddeedfeed.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpportunitiesData(
    val opportunities: List<VolunteerOpportunity>,
    val categories: List<OpportunityCategory>,
    val selectedCategory: OpportunityCategory?,
    val isMapView: Boolean,
)

@HiltViewModel
class OpportunitiesViewModel @Inject constructor(
    private val getOpportunitiesUseCase: GetOpportunitiesUseCase,
    private val applyForOpportunityUseCase: ApplyForOpportunityUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<OpportunitiesData>>(UiState.Loading)
    val uiState: StateFlow<UiState<OpportunitiesData>> = _uiState.asStateFlow()

    init {
        loadOpportunities()
    }

    fun loadOpportunities() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            getOpportunitiesUseCase()
                .catch { e ->
                    _uiState.value = UiState.Error("Failed to load opportunities: ${e.message}")
                }
                .collect { opportunities ->
                    val categories = OpportunityCategory.values().toList()
                    _uiState.value = UiState.Success(
                        OpportunitiesData(
                            opportunities = opportunities,
                            categories = categories,
                            selectedCategory = null,
                            isMapView = false,
                        ),
                    )
                }
        }
    }

    fun filterByCategory(category: OpportunityCategory?) {
        viewModelScope.launch {
            if (category == null) {
                loadOpportunities()
            } else {
                getOpportunitiesUseCase.getByCategory(category)
                    .catch { e ->
                        _uiState.value = UiState.Error("Failed to filter opportunities: ${e.message}")
                    }
                    .collect { opportunities ->
                        val currentState = _uiState.value
                        if (currentState is UiState.Success) {
                            _uiState.value = currentState.copy(
                                data = currentState.data.copy(
                                    opportunities = opportunities,
                                    selectedCategory = category,
                                ),
                            )
                        }
                    }
            }
        }
    }

    fun searchOpportunities(query: String) {
        viewModelScope.launch {
            getOpportunitiesUseCase.search(query)
                .catch { e ->
                    _uiState.value = UiState.Error("Failed to search opportunities: ${e.message}")
                }
                .collect { opportunities ->
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        _uiState.value = currentState.copy(
                            data = currentState.data.copy(
                                opportunities = opportunities,
                                selectedCategory = null,
                            ),
                        )
                    }
                }
        }
    }

    fun joinOpportunity(opportunityId: Int) {
        viewModelScope.launch {
            applyForOpportunityUseCase(opportunityId)
                .onSuccess {
                    // Opportunity joined successfully - could show a success message
                    loadOpportunities() // Refresh to update status
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error("Failed to join opportunity: ${e.message}")
                }
        }
    }

    fun toggleMapView() {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(isMapView = !currentState.data.isMapView),
            )
        }
    }
} 
