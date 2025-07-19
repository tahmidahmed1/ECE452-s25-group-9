package com.example.gooddeedfeed.presentation.ui.screens.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.domain.model.VolunteerEvent

@Composable
fun OrganizerEventDetailScreen(
    event: VolunteerEvent,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(contentAlignment = Alignment.TopStart) {
            event.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Event Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
            }
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(icon = Icons.Default.Event, text = "${event.date} • ${event.startTime} - ${event.endTime}")
            InfoRow(icon = Icons.Default.LocationOn, text = event.location)
            InfoRow(icon = Icons.Default.Group, text = "${event.currentVolunteers}/${event.maxVolunteers} volunteers")
            InfoRow(icon = Icons.Default.Star, text = "${event.karmaPoints} Karma Points")
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Text(text = event.description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
} 