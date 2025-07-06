package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
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

    // Permission handling – request only when user taps button in PermissionRationaleCard
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
        if (granted) viewModel.onLocationPermissionGranted() else viewModel.onLocationPermissionDenied()
    }

    // If permission becomes granted (either already or after user tap), notify ViewModel
    androidx.compose.runtime.LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.onLocationPermissionGranted()
        }
    }

    // Root layout
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            MapView(
                uiState = uiState,
                locationPermissionState = locationPermissionState,
                modifier = Modifier.fillMaxSize(),
                onEventSelected = { selectedEvent = it },
            )

            LegendPanel(
                filteredEvents = uiState.filteredEvents,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
        }

        RadiusSlider(
            radiusKm = uiState.radiusKm,
            onRadiusChange = viewModel::updateRadius,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    // Event detail dialog
    selectedEvent?.let { event ->
        EventDetailDialog(event, uiState.currentLocation) { selectedEvent = null }
    }
} 
