package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gooddeedfeed.domain.model.DomainOrganizerWithSubscriptionStatus
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.OpportunityFilters
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.ui.components.ToastManager
import com.example.gooddeedfeed.presentation.ui.components.base.EnhancedLocationPermissionManager
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.volunteer.FiltersDrawer
import com.example.gooddeedfeed.presentation.ui.components.volunteer.OpportunitiesList
import com.example.gooddeedfeed.presentation.ui.theme.CornerRadius
import com.example.gooddeedfeed.presentation.viewmodel.ChatViewModel
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.OpportunitiesViewModel
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.SubscriptionViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ListScreen(
    user: DomainUser,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: OpportunitiesViewModel = hiltViewModel<OpportunitiesViewModel>(),
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel<SubscriptionViewModel>(),
    chatViewModel: ChatViewModel = hiltViewModel<ChatViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionUiState by subscriptionViewModel.uiState.collectAsStateWithLifecycle()
    var organizerSearch by remember { mutableStateOf("") }
    var selectedOrganizer by remember { mutableStateOf<DomainOrganizerWithSubscriptionStatus?>(null) }
    var selectedOpportunity by remember { mutableStateOf<VolunteerOpportunity?>(null) }
    var filtersExpanded by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(OpportunityFilters()) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(organizerSearch) {
        if (organizerSearch.isNotBlank()) {
            subscriptionViewModel.searchOrganizers(organizerSearch)
        }
    }

    LaunchedEffect(filters) {
        viewModel.applyFilters(filters)
    }

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
            if (selectedOrganizer != null) {
                OrganizerProfileScreen(
                    organizer = selectedOrganizer!!,
                    onBack = { selectedOrganizer = null },
                    onMessage = {
                        showMessageDialog = true
                        messageText = ""
                    },
                    onSubscriptionToggle = {
                        if (selectedOrganizer!!.isSubscribed) {
                            subscriptionViewModel.unsubscribeFromOrganizer(selectedOrganizer!!.id)
                            selectedOrganizer = selectedOrganizer!!.copy(
                                isSubscribed = false,
                                subscriberCount = selectedOrganizer!!.subscriberCount - 1,
                            )
                            ToastManager.showSuccess("Unsubscribed from ${selectedOrganizer!!.organizationName ?: selectedOrganizer!!.fullName ?: selectedOrganizer!!.username}")
                        } else {
                            subscriptionViewModel.subscribeToOrganizer(selectedOrganizer!!.id)
                            selectedOrganizer = selectedOrganizer!!.copy(
                                isSubscribed = true,
                                subscriberCount = selectedOrganizer!!.subscriberCount + 1,
                            )
                            ToastManager.showSuccess("Subscribed to ${selectedOrganizer!!.organizationName ?: selectedOrganizer!!.fullName ?: selectedOrganizer!!.username}")
                        }
                    },
                )
            } else if (selectedOpportunity != null) {
                VolunteerOpportunityDetailScreen(opportunity = selectedOpportunity!!, onBack = { selectedOpportunity = null })
            } else {
                Column(modifier = modifier.fillMaxSize()) {
                    HeaderSection(
                        organizerSearch = organizerSearch,
                        onOrganizerSearchChange = { organizerSearch = it },
                        selectedOrganizer = selectedOrganizer,
                        onOrganizerSelected = { selectedOrganizer = it },
                        onFiltersClick = { filtersExpanded = true },
                    )

                    if (organizerSearch.isNotBlank()) {
                        when (val searchState = subscriptionUiState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Success -> {
                                OrganizersList(
                                    organizers = searchState.data,
                                    onOrganizerClick = { organizer -> selectedOrganizer = organizer },
                                    onSubscriptionClick = { organizer ->
                                        if (organizer.isSubscribed) {
                                            subscriptionViewModel.unsubscribeFromOrganizer(organizer.id)
                                            ToastManager.showSuccess("Unsubscribed from ${organizer.organizationName ?: organizer.fullName ?: organizer.username}")
                                        } else {
                                            subscriptionViewModel.subscribeToOrganizer(organizer.id)
                                            ToastManager.showSuccess("Subscribed to ${organizer.organizationName ?: organizer.fullName ?: organizer.username}")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = searchState.message,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Start typing to search organizers...",
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    } else {
                        when (val state = uiState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Success -> {
                                OpportunitiesList(
                                    opportunities = state.data.opportunities,
                                    onJoinOpportunity = { opportunityId -> viewModel.joinOpportunity(opportunityId) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Unknown state",
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }

                if (filtersExpanded) {
                    FiltersDrawer(
                        isExpanded = filtersExpanded,
                        onToggle = { filtersExpanded = !filtersExpanded },
                        filters = filters,
                        onFiltersChange = { filters = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (showMessageDialog && selectedOrganizer != null) {
                MessageDialog(
                    organizerName = selectedOrganizer!!.organizationName ?: selectedOrganizer!!.fullName ?: selectedOrganizer!!.username,
                    messageText = messageText,
                    onMessageTextChange = { messageText = it },
                    onSend = {
                        if (selectedOrganizer != null && messageText.isNotBlank()) {
                            chatViewModel.sendMessage(messageText, selectedOrganizer!!.id, user)
                            ToastManager.showSuccess("Message sent to ${selectedOrganizer!!.organizationName ?: selectedOrganizer!!.fullName ?: selectedOrganizer!!.username}")
                        }
                        showMessageDialog = false
                        messageText = ""
                    },
                    onDismiss = {
                        showMessageDialog = false
                        messageText = ""
                    },
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun HeaderSection(
    organizerSearch: String,
    onOrganizerSearchChange: (String) -> Unit,
    selectedOrganizer: DomainOrganizerWithSubscriptionStatus?,
    onOrganizerSelected: (DomainOrganizerWithSubscriptionStatus?) -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "List",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Volunteer Opportunities",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = organizerSearch,
                onValueChange = onOrganizerSearchChange,
                placeholder = { Text("Search organizers...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                shape = RoundedCornerShape(12.dp),
            )

            IconButton(onClick = onFiltersClick) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun OrganizerCard(
    organizer: DomainOrganizerWithSubscriptionStatus,
    onSubscriptionClick: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (organizer.profilePictureUrl != null && organizer.profilePictureUrl.isNotEmpty()) {
                AsyncImage(
                    model = organizer.profilePictureUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Profile Picture",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = organizer.organizationName ?: organizer.fullName ?: organizer.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = organizer.organizationDescription ?: "No description available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (organizer.subscriberCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${organizer.subscriberCount} subscribers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                onClick = onSubscriptionClick,
                modifier = Modifier.padding(start = 8.dp),
                shape = RoundedCornerShape(CornerRadius.medium),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (organizer.isSubscribed) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (organizer.isSubscribed) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
                border = BorderStroke(
                    1.dp,
                    if (organizer.isSubscribed) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
            ) {
                Icon(
                    imageVector = if (organizer.isSubscribed) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (organizer.isSubscribed) "Unsubscribe" else "Subscribe")
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
private fun OrganizerProfileScreen(
    organizer: DomainOrganizerWithSubscriptionStatus,
    onBack: () -> Unit,
    onMessage: () -> Unit,
    onSubscriptionToggle: () -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Profile", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (organizer.profilePictureUrl != null && organizer.profilePictureUrl.isNotEmpty()) {
                AsyncImage(
                    model = organizer.profilePictureUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Profile Picture",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            organizer.organizationName?.let { orgName ->
                Text(
                    text = orgName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            } ?: organizer.fullName?.let { fullName ->
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = "@${organizer.username}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = organizer.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(organizer.organizationDescription ?: "No description available", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onSubscriptionToggle,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(CornerRadius.medium),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (organizer.isSubscribed) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (organizer.isSubscribed) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
                border = BorderStroke(
                    1.dp,
                    if (organizer.isSubscribed) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
            ) {
                Icon(
                    imageVector = if (organizer.isSubscribed) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (organizer.isSubscribed) "Unsubscribe" else "Subscribe")
            }

            OutlinedButton(
                onClick = onMessage,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(CornerRadius.medium),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(Icons.Default.Chat, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Message")
            }
        }

        if (organizer.subscriberCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${organizer.subscriberCount} subscribers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Events", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        VerticalSpacer()
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Events will be loaded here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VolunteerOpportunityDetailScreen(opportunity: VolunteerOpportunity, onBack: () -> Unit) {
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

@Composable
private fun OrganizersList(
    organizers: List<DomainOrganizerWithSubscriptionStatus>,
    onOrganizerClick: (DomainOrganizerWithSubscriptionStatus) -> Unit,
    onSubscriptionClick: (DomainOrganizerWithSubscriptionStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(organizers) { organizer ->
            OrganizerCard(
                organizer = organizer,
                onSubscriptionClick = { onSubscriptionClick(organizer) },
                onClick = { onOrganizerClick(organizer) },
            )
        }
    }
}

@Composable
private fun MessageDialog(
    organizerName: String,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Message $organizerName")
        },
        text = {
            Column {
                Text(
                    text = "Send a message to start a conversation:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageTextChange,
                    placeholder = { Text("Type your message...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSend,
                enabled = messageText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(CornerRadius.medium),
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(CornerRadius.medium),
            ) {
                Text("Cancel")
            }
        },
    )
} 
