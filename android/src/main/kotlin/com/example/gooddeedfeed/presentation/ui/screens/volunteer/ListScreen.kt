package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.ui.components.ToastUtils
import com.example.gooddeedfeed.presentation.ui.components.base.PermissionRationaleCard
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.theme.AppConstants
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.OpportunitiesData
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.OpportunitiesViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ListScreen(
    user: DomainUser,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: OpportunitiesViewModel = hiltViewModel<OpportunitiesViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var organizerSearch by remember { mutableStateOf("") }
    var selectedOrganizer by remember { mutableStateOf<AppConstants.OrganizerProfile?>(null) }
    val subscriptions = remember { mutableStateListOf<String>() }
    var selectedOpportunity by remember { mutableStateOf<VolunteerOpportunity?>(null) }

    // Location permission handling
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
        if (granted) viewModel.onLocationPermissionGranted() else viewModel.onLocationPermissionDenied()
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.onLocationPermissionGranted()
        }
    }

    if (!locationPermissionState.status.isGranted) {
        PermissionRationaleCard { locationPermissionState.launchPermissionRequest() }
        return
    }

    if (selectedOpportunity != null) {
        VolunteerOpportunityDetailScreen(opportunity = selectedOpportunity!!, onBack = { selectedOpportunity = null })
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Search radius slider
        if (uiState is UiState.Success<*>) {
            val radiusKm = (uiState as UiState.Success<OpportunitiesData>).data.radiusKm
            RadiusSlider(radiusKm = radiusKm, onRadiusChange = viewModel::updateRadius, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }

        OutlinedTextField(
            value = organizerSearch,
            onValueChange = { organizerSearch = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            label = { Text("Search organizers") },
            singleLine = true,
            trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
        )

        if (organizerSearch.isNotBlank()) {
            // show organizer search mode
            val filteredOrganizers = AppConstants.MOCK_ORGANIZERS.filter { it.name.contains(organizerSearch, ignoreCase = true) }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = filteredOrganizers,
                    key = { org -> org.name },
                ) { org ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOrganizer = org },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(org.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(org.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = {
                                if (org.name !in subscriptions) subscriptions += org.name
                            }) {
                                Icon(
                                    imageVector = if (org.name in subscriptions) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (org.name in subscriptions) "Subscribed" else "Subscribe")
                            }
                        }
                    }
                }
            }

            selectedOrganizer?.let { org ->
                OrganizerProfileScreen(org, onBack = { selectedOrganizer = null })
            }

            return // skip rendering opportunities when searching
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = "List",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Volunteer Opportunities",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        when (val currentState = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Success<OpportunitiesData> -> {
                val opportunitiesData = currentState.data

                if (opportunitiesData.opportunities.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "No Opportunities",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No opportunities available",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Check back later for new volunteer opportunities",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = if (organizerSearch.isBlank()) opportunitiesData.opportunities else emptyList(),
                            key = { opportunity -> opportunity.id },
                        ) { opportunity ->
                            OpportunityCard(
                                opportunity = opportunity,
                                onJoinClick = { viewModel.joinOpportunity(opportunity.id) },
                                onClick = { selectedOpportunity = opportunity },
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadOpportunities() },
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun OpportunityCard(
    opportunity: VolunteerOpportunity,
    onJoinClick: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header row with title and volunteer count
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = opportunity.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = "${opportunity.currentVolunteers}/${opportunity.requiredVolunteers}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = opportunity.organizationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = opportunity.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = opportunity.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = opportunity.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onJoinClick,
                modifier = Modifier.align(Alignment.End),
                enabled = opportunity.currentVolunteers < opportunity.requiredVolunteers,
            ) {
                Text(if (opportunity.currentVolunteers < opportunity.requiredVolunteers) "Join" else "Full")
            }
        }
    }
}

@Composable
private fun OrganizerProfileScreen(profile: AppConstants.OrganizerProfile, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Spacer(modifier = Modifier.width(8.dp))
            Text(profile.name, style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(profile.description, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        FilledTonalButton(onClick = {
            // Mock: open chat
            ToastUtils.showSuccessToast(context, "Chat opened (mock)")
        }) {
            Icon(Icons.Default.Chat, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Message")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Events", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        VerticalSpacer()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                items = profile.events,
                key = { event -> event.title + event.date },
            ) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // TODO: navigate to event detail
                        },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(event.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun VolunteerOpportunityDetailScreen(opportunity: VolunteerOpportunity, onBack: () -> Unit) {
    // Mock images list (replace with real once available)
    val images = listOf(
        "https://images.unsplash.com/photo-1492724441997-5dc865305da7?auto=format&fit=crop&w=600&q=60",
        "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=600&q=60",
        "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=600&q=60",
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(8.dp))
            Text(opportunity.title, style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(8.dp))
        Text(opportunity.description, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(16.dp))
        Text("Images", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(images) { url ->
                Card(shape = RoundedCornerShape(8.dp)) {
                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(140.dp).aspectRatio(1f), contentScale = ContentScale.Crop)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column {
                Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(opportunity.date, style = MaterialTheme.typography.bodyMedium)
            }
            Column {
                Text("Volunteers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${opportunity.currentVolunteers}/${opportunity.requiredVolunteers}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Location", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(opportunity.location, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(24.dp))
        Button(onClick = { /* mock apply */ }, enabled = opportunity.currentVolunteers < opportunity.requiredVolunteers) {
            Text(if (opportunity.currentVolunteers < opportunity.requiredVolunteers) "Apply" else "Full")
        }
    }
} 
