package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.MapUiContract
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
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
import com.example.gooddeedfeed.presentation.ui.components.base.PermissionRationaleCard
import com.example.gooddeedfeed.domain.util.calculateDistanceKm

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapView(
    uiState: MapUiContract,
    locationPermissionState: PermissionState,
    modifier: Modifier = Modifier,
    onEventSelected: (VolunteerEvent) -> Unit,
) {
    val defaultLocation = LatLng(43.6532, -79.3832)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { loc ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 13f),
                1000,
            )
        }
    }

    if (locationPermissionState.status.isGranted) {
        GoogleMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true, mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false),
        ) {
            uiState.currentLocation?.let { loc ->
                Circle(
                    center = LatLng(loc.latitude, loc.longitude),
                    radius = (uiState.radiusKm * 1000).toDouble(),
                    strokeColor = Color.Blue.copy(alpha = 0.5f),
                    fillColor = Color.Blue.copy(alpha = 0.1f),
                    strokeWidth = 2f,
                )
            }

            uiState.filteredEvents.forEach { event ->
                Marker(
                    state = MarkerState(position = LatLng(event.latitude, event.longitude)),
                    title = event.title,
                    snippet = event.organizationName,
                    onClick = {
                        onEventSelected(event)
                        true
                    },
                    icon = BitmapDescriptorFactory.defaultMarker(event.category.hue()),
                )
            }
        }
    } else {
        PermissionRationaleCard { locationPermissionState.launchPermissionRequest() }
    }
}

@Composable
fun LegendPanel(filteredEvents: List<VolunteerEvent>, modifier: Modifier = Modifier) {
    if (filteredEvents.isEmpty()) return

    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text("Nearby Events", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            OpportunityCategory.values().forEach { category ->
                val count = filteredEvents.count { it.category == category }
                if (count > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(category.color(), CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${category.name.replace("_", " ")} ($count)", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RadiusSlider(radiusKm: Float, onRadiusChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Search radius", fontWeight = FontWeight.Medium)
                Text("${radiusKm.toInt()} km", fontWeight = FontWeight.Bold)
            }
            Slider(value = radiusKm, onValueChange = onRadiusChange, valueRange = 1f..50f, steps = 49)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1 km", fontSize = 12.sp)
                Text("50 km", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EventDetailDialog(event: VolunteerEvent, currentLocation: Location?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(event.title, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "close") }
                }
                Spacer(Modifier.height(8.dp))
                Text(event.organizationName, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(event.description)
                Spacer(Modifier.height(12.dp))
                DetailsRow("Date & Time", "${event.date}\n${event.startTime}-${event.endTime}")
                DetailsRow("Available", "${event.maxVolunteers - event.currentVolunteers}/${event.maxVolunteers}")
                Spacer(Modifier.height(12.dp))
                Text("Location: ${event.location}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                currentLocation?.let { loc ->
                    val dist = calculateDistanceKm(loc.latitude, loc.longitude, event.latitude, event.longitude)
                    Text("Distance: ${"%.1f".format(dist)} km", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Join Event") }
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

// Helper extensions (kept locally for map visuals)
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
