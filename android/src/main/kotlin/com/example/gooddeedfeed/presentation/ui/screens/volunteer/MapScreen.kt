package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.example.gooddeedfeed.presentation.ui.components.base.EnhancedLocationPermissionManager
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.ln

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    user: DomainUser,
    onLogout: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedEvent by remember { mutableStateOf<VolunteerEvent?>(null) }

    val initialZoomLevel = calculateZoomForRadius(uiState.radiusKm)
    var zoomLevel by remember { mutableStateOf(initialZoomLevel) }

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
                        zoomLevel = zoomLevel,
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
                    onZoomChange = { newZoom -> zoomLevel = newZoom },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            selectedEvent?.let { event ->
                EventDetailDialog(event, uiState.currentLocation) { selectedEvent = null }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * Calculate zoom level to ensure the radius circle fits within the screen bounds
 * This uses the approximate relationship between Google Maps zoom levels and visible distance
 */
private fun calculateZoomForRadius(radiusKm: Float): Float {
    val radiusMeters = radiusKm * 1000.0
    val paddingFactor = 1.8 // This ensures the circle fits comfortably with some padding
    val requiredViewDistance = radiusMeters * paddingFactor

    val metersPerPixelAtZoom0 = 156543.03392 * 0.7071
    val screenWidthPixels = 1000.0
    val requiredMetersPerPixel = requiredViewDistance / screenWidthPixels

    val zoomLevel = ln(metersPerPixelAtZoom0 / requiredMetersPerPixel) / ln(2.0)

    return zoomLevel.toFloat().coerceIn(8f, 18f)
} 
