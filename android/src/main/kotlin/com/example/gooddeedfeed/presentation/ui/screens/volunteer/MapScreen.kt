package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.ui.components.base.EnhancedLocationPermissionManager
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.MapViewModel
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.OpportunitiesViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    user: DomainUser,
    onLogout: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedEvent by remember { mutableStateOf<VolunteerEvent?>(null) }
    var detailEvent by remember { mutableStateOf<VolunteerEvent?>(null) }

    // ViewModel to handle join/leave actions
    val oppViewModel: OpportunitiesViewModel = hiltViewModel()

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
        if (granted) viewModel.onLocationPermissionGranted() else viewModel.onLocationPermissionDenied()
    }

    EnhancedLocationPermissionManager(
        locationPermissionState = locationPermissionState,
        locationSettingsRepository = viewModel.locationSettingsRepository,
        onPermissionGranted = { viewModel.onLocationPermissionGranted() },
        onPermissionDenied = { viewModel.onLocationPermissionDenied() },
        onLocationDisabled = { viewModel.onLocationPermissionDenied() },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    MapView(
                        uiState = uiState,
                        locationPermissionState = locationPermissionState,
                        modifier = Modifier.fillMaxSize(),
                        // fixed zoom handled internally
                        onEventSelected = { selectedEvent = it },
                        onNavigateToDetails = { event ->
                            selectedEvent = null
                            detailEvent = event
                        },
                    )

                    // Legend panel removed in new design
                }
            }

            // Only show popup if no detail event is open
            if (detailEvent == null) {
                selectedEvent?.let { event ->
                    EventDetailDialog(event, uiState.currentLocation, onDismiss = { selectedEvent = null }, onNavigateToDetails = {
                        detailEvent = event
                        selectedEvent = null // Clear popup when opening details
                    })
                }
            }

            detailEvent?.let { ev ->
                val oppUiState by oppViewModel.uiState.collectAsStateWithLifecycle()
                val currentState = oppUiState
                val isJoined = when (currentState) {
                    is UiState.Success -> currentState.data.opportunities.any { it.id == ev.id && it.isJoined }
                    else -> false
                }

                val opp = VolunteerOpportunity(
                    id = ev.id,
                    title = ev.title,
                    organizationName = ev.organizationName,
                    location = ev.location,
                    date = ev.date,
                    startTime = ev.startTime,
                    endTime = ev.endTime,
                    description = ev.description,
                    requiredVolunteers = ev.maxVolunteers,
                    currentVolunteers = ev.currentVolunteers,
                    category = ev.category,
                    latitude = ev.latitude,
                    longitude = ev.longitude,
                    karmaPoints = ev.karmaPoints,
                    imageUrl = ev.imageUrl,
                    isJoined = isJoined,
                    images = ev.images,
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    VolunteerOpportunityDetailScreen(
                        opportunity = opp,
                        onBack = { detailEvent = null },
                        onJoin = { id -> oppViewModel.joinOpportunity(id) },
                        onLeave = { id -> oppViewModel.leaveOpportunity(id) },
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
} 
