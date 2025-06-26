package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedEvent by remember { mutableStateOf<VolunteerEvent?>(null) }

    // Permission handling
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
        if (granted) viewModel.onLocationPermissionGranted() else viewModel.onLocationPermissionDenied()
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        } else {
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
