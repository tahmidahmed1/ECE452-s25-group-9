package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import android.location.Location
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.domain.util.calculateDistanceKm
import com.example.gooddeedfeed.presentation.ui.components.base.LocationPermissionHandler
import com.example.gooddeedfeed.presentation.ui.components.base.PermissionRationaleCard
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.MapUiContract
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapView(
    uiState: MapUiContract,
    locationPermissionState: PermissionState,
    modifier: Modifier = Modifier,
    onEventSelected: (VolunteerEvent) -> Unit,
    onNavigateToDetails: (VolunteerEvent) -> Unit,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(43.6532, -79.3832), 12f)
    }

    // Only center on user location once when first available
    var hasAnimatedToUserLocation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { location ->
            if (!hasAnimatedToUserLocation) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        12f,
                    ),
                )
                hasAnimatedToUserLocation = true
            }
        }
    }

    // Removed auto recentring to prevent camera jump while user pans

    if (locationPermissionState.status.isGranted) {
        GoogleMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapType = MapType.NORMAL,
                isIndoorEnabled = false,
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = true,
                compassEnabled = true,
                rotationGesturesEnabled = true,
                scrollGesturesEnabled = true,
                tiltGesturesEnabled = true,
                zoomGesturesEnabled = true,
            ),
        ) {
            uiState.currentLocation?.let { loc ->
                Circle(
                    center = LatLng(loc.latitude, loc.longitude),
                    radius = 1000.0,
                    strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeWidth = 3f,
                )
            }

            uiState.filteredEvents.forEach { event ->
                Marker(
                    state = MarkerState(position = LatLng(event.latitude, event.longitude)),
                    title = event.title,
                    snippet = event.organizationName,
                    onClick = {
                        Log.d("MapScreen", "Marker clicked: ${event.title}")
                        onEventSelected(event)
                        true
                    },
                    icon = BitmapDescriptorFactory.defaultMarker(event.category.hue()),
                )
            }
        }
    } else {
        if (locationPermissionState.status.shouldShowRationale) {
            PermissionRationaleCard { locationPermissionState.launchPermissionRequest() }
        } else {
            LocationPermissionHandler(
                onOpenSettings = { /* This is handled within the component */ },
            )
        }
    }
}

// Radius slider removed – we now always show all events.

@Composable
fun EventDetailDialog(event: VolunteerEvent, currentLocation: Location?, onDismiss: () -> Unit, onNavigateToDetails: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(12.dp)) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                // Placeholder organizer image (could replace with real url when available)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(event.title, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${event.date}  ${event.startTime}-${event.endTime}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    val desc = if (event.description.length > 120) event.description.take(117).trimEnd() + "…" else event.description
                    Text(desc, maxLines = 2, fontSize = 12.sp)

                    currentLocation?.let { loc ->
                        val dist = calculateDistanceKm(loc.latitude, loc.longitude, event.latitude, event.longitude)
                        Text("${"%.1f".format(dist)} km away", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                IconButton(onClick = { onNavigateToDetails() }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "details", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun DetailsRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(value, fontSize = 12.sp)
    }
}

fun OpportunityCategory.color(): Color = when (this) {
    OpportunityCategory.ENVIRONMENTAL -> Color(0xFF4CAF50)
    OpportunityCategory.EDUCATION -> Color(0xFF2196F3)
    OpportunityCategory.SOCIAL_SERVICES -> Color(0xFFFF9800)
    OpportunityCategory.COMMUNITY_SERVICE -> Color(0xFF9C27B0)
    else -> Color(0xFF757575)
}

fun OpportunityCategory.hue(): Float = when (this) {
    OpportunityCategory.ENVIRONMENTAL -> BitmapDescriptorFactory.HUE_GREEN
    OpportunityCategory.EDUCATION -> BitmapDescriptorFactory.HUE_BLUE
    OpportunityCategory.SOCIAL_SERVICES -> BitmapDescriptorFactory.HUE_ORANGE
    OpportunityCategory.COMMUNITY_SERVICE -> BitmapDescriptorFactory.HUE_VIOLET
    else -> BitmapDescriptorFactory.HUE_AZURE
} 
