package com.example.gooddeedfeed.presentation.ui.screens.organizer

import android.util.Log
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gooddeedfeed.data.mapper.toEmulatorAccessibleUrl
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.viewmodel.organizer.EventManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventManagementScreen(
    user: DomainUser,
    modifier: Modifier = Modifier,
    viewModel: EventManagementViewModel = hiltViewModel<EventManagementViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedEvent by remember { mutableStateOf<VolunteerEvent?>(null) }
    var creatingEvent by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<VolunteerEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<VolunteerEvent?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    if (creatingEvent) {
        CreateEventScreen(onBack = { creatingEvent = false })
        return
    }

    if (editingEvent != null) {
        CreateEventScreen(
            onBack = { editingEvent = null },
            eventToEdit = editingEvent,
        )
        return
    }

    if (selectedEvent != null) {
        OrganizerEventDetailScreen(event = selectedEvent!!, onBack = { selectedEvent = null })
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Manage Events",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manage Events",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            FloatingActionButton(
                onClick = { creatingEvent = true },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Event",
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.isNotBlank()) {
                    viewModel.searchEvents(it)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Search events...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(),
        )

        when (val currentState = uiState) {
            is UiState.Idle -> {
            }
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Success -> {
                val eventData = currentState.data
                val eventsToShow = if (searchQuery.isNotBlank()) {
                    viewModel.searchResults.collectAsStateWithLifecycle().value
                } else {
                    eventData.events
                }

                if (eventsToShow.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Card {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "No Events",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No events yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Create your first volunteer event",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(eventsToShow) { event ->
                            EventCard(
                                event = event,
                                onEditClick = { editingEvent = event },
                                onDeleteClick = { eventToDelete = event },
                                onViewClick = { selectedEvent = event },
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
                        onClick = { viewModel.loadEvents() },
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }

    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Delete Event?") },
            text = { Text("Are you sure you want to delete '${eventToDelete!!.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(eventToDelete!!.id)
                        eventToDelete = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { eventToDelete = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun EventCard(
    event: VolunteerEvent,
    onEditClick: (VolunteerEvent) -> Unit,
    onDeleteClick: (VolunteerEvent) -> Unit,
    onViewClick: (VolunteerEvent) -> Unit,
) {
    Log.d("EventCard", "=== EVENT CARD DEBUGGING ===")
    Log.d("EventCard", "Event ID: ${event.id}, Title: ${event.title}")
    Log.d("EventCard", "Event imageUrl: ${event.imageUrl}")
    Log.d("EventCard", "Event images count: ${event.images.size}")

    event.images.forEachIndexed { index, image ->
        Log.d("EventCard", "Image $index - ID: ${image.id}, URL: ${image.image_url}, is_main: ${image.is_main}")
    }

    val mainImage = event.images.firstOrNull { it.is_main }
    Log.d("EventCard", "Main image found: ${mainImage?.let { "ID: ${it.id}, URL: ${it.image_url}" } ?: "NONE"}")

    val fallbackImage = if (mainImage == null && event.images.isNotEmpty()) {
        event.images.first().also {
            Log.d("EventCard", "Using fallback image: ID: ${it.id}, URL: ${it.image_url}")
        }
    } else {
        null
    }

    val bannerUrl = mainImage?.image_url?.toEmulatorAccessibleUrl()
        ?: fallbackImage?.image_url?.toEmulatorAccessibleUrl()
        ?: event.imageUrl?.toEmulatorAccessibleUrl()
    Log.d("EventCard", "Final banner URL: $bannerUrl")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick(event) },
    ) {
        Column {
            if (bannerUrl != null) {
                Log.d("EventCard", "Displaying banner for event ${event.id}: $bannerUrl")
                AsyncImage(
                    model = bannerUrl,
                    contentDescription = "Event Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                    onSuccess = {
                        Log.d("EventCard", "Banner loaded successfully for event ${event.id}: $bannerUrl")
                    },
                    onError = { error ->
                        Log.e("EventCard", "Banner failed to load for event ${event.id}: $bannerUrl, error: $error")
                    },
                )
            } else {
                Log.w("EventCard", "No banner URL for event ${event.id} - banner will not be displayed")
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )

                    Row {
                        IconButton(
                            onClick = { onEditClick(event) },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Event",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        IconButton(
                            onClick = { onDeleteClick(event) },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Event",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${event.date}  •  ${event.startTime} - ${event.endTime}", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(8.dp))

                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = "Volunteers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${event.currentVolunteers}/${event.maxVolunteers}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Column {
                        Text(
                            text = "Karma Points",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${event.karmaPoints}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
