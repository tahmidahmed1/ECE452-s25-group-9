package com.example.gooddeedfeed.presentation.ui.components.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.EventStatus
import com.example.gooddeedfeed.domain.model.VolunteerEvent

/**
 * Card component for displaying events in organizer view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: VolunteerEvent,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                EventStatusChip(status = event.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            EventDetailRow(
                icon = Icons.Default.LocationOn,
                text = event.location
            )
            
            EventDetailRow(
                icon = Icons.Default.DateRange,
                text = event.date
            )
            
            EventDetailRow(
                icon = Icons.Default.Schedule,
                text = "${event.startTime} - ${event.endTime}"
            )
            
            EventDetailRow(
                icon = Icons.Default.Group,
                text = "${event.currentVolunteers}/${event.maxVolunteers} volunteers"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit event"
                    )
                }
                
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete event",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EventDetailRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EventStatusChip(
    status: EventStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        EventStatus.DRAFT -> MaterialTheme.colorScheme.outline to "Draft"
        EventStatus.PUBLISHED -> MaterialTheme.colorScheme.primary to "Published"
        EventStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary to "In Progress"
        EventStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to "Completed"
        EventStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Cancelled"
    }
    
    AssistChip(
        onClick = { },
        label = { Text(text) },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

/**
 * Lazy column for displaying list of events for organizers
 */
@Composable
fun EventsList(
    events: List<VolunteerEvent>,
    onEditEvent: (VolunteerEvent) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(events) { event ->
            EventCard(
                event = event,
                onEditClick = { onEditEvent(event) },
                onDeleteClick = { onDeleteEvent(event.id) }
            )
        }
    }
} 