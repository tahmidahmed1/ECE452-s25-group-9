package com.example.gooddeedfeed.presentation.ui.screens.organizer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.gooddeedfeed.data.mapper.toEmulatorAccessibleUrl
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.presentation.viewmodel.organizer.EventManagementViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import com.example.gooddeedfeed.domain.model.DomainUser
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun OrganizerEventDetailScreen(
    event: VolunteerEvent,
    onBack: () -> Unit,
    viewModel: EventManagementViewModel = hiltViewModel(),
) {
    Log.d("EventDetailScreen", "=== EVENT DETAIL DEBUGGING ===")
    Log.d("EventDetailScreen", "Event ID: ${event.id}")
    Log.d("EventDetailScreen", "Event Title: ${event.title}")
    Log.d("EventDetailScreen", "Event imageUrl: ${event.imageUrl}")
    Log.d("EventDetailScreen", "Event images count: ${event.images.size}")

    event.images.forEachIndexed { index, image ->
        Log.d("EventDetailScreen", "Image $index - ID: ${image.id}, URL: ${image.image_url}, is_main: ${image.is_main}")
    }

    val mainImage = event.images.firstOrNull { it.is_main }
    Log.d("EventDetailScreen", "Main image found: ${mainImage?.let { "ID: ${it.id}, URL: ${it.image_url}" } ?: "NONE"}")

    val fallbackImage = if (mainImage == null && event.images.isNotEmpty()) {
        event.images.first().also {
            Log.d("EventDetailScreen", "Using fallback image: ID: ${it.id}, URL: ${it.image_url}")
        }
    } else {
        null
    }

    val bannerUrl = mainImage?.image_url?.toEmulatorAccessibleUrl()
        ?: fallbackImage?.image_url?.toEmulatorAccessibleUrl()
        ?: event.imageUrl?.toEmulatorAccessibleUrl()
    Log.d("EventDetailScreen", "Final banner URL: $bannerUrl")

    val volunteers by viewModel.volunteers.collectAsState()

    // State for previewing a volunteer profile
    var previewVolunteer by remember { mutableStateOf<com.example.gooddeedfeed.domain.model.JoinedVolunteer?>(null) }

    // Load volunteers when screen appears
    LaunchedEffect(event.id) {
        viewModel.loadEventVolunteers(event.id)
    }

    val eventDate = try {
        LocalDate.parse(event.date, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: Exception) { null }
    val showJoinedSection = eventDate == null || LocalDate.now().isBefore(eventDate.plusDays(1))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(contentAlignment = Alignment.TopStart) {
            if (bannerUrl != null) {
                Log.d("EventDetailScreen", "Displaying banner image: $bannerUrl")
                AsyncImage(
                    model = bannerUrl,
                    contentDescription = "Event Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                    onSuccess = {
                        Log.d("EventDetailScreen", "Banner image loaded successfully: $bannerUrl")
                    },
                    onError = { error ->
                        Log.e("EventDetailScreen", "Banner image failed to load: $bannerUrl, error: $error")
                    },
                )
            } else {
                Log.w("EventDetailScreen", "No banner URL available - banner will not be displayed")
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
            InfoRow(icon = Icons.Default.Category, text = event.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
            InfoRow(icon = Icons.Default.Group, text = "${event.currentVolunteers}/${event.maxVolunteers} volunteers")
            InfoRow(icon = Icons.Default.Star, text = "${event.karmaPoints} Karma Points")
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Text(text = event.description, style = MaterialTheme.typography.bodyLarge)

            if (event.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Event Photos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    items(event.images) { image ->
                        Box {
                            AsyncImage(
                                model = image.image_url.toEmulatorAccessibleUrl(),
                                contentDescription = "Event Photo",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }

            // Volunteers Joined or Attendance
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (showJoinedSection) "Volunteers Joined" else "Volunteer Attendance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (showJoinedSection) {
                if (volunteers.isEmpty()) {
                    Text(text = "No volunteers yet", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        volunteers.forEach { v ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { previewVolunteer = v }
                                    .padding(vertical = 8.dp),
                            ) {
                                AsyncImage(
                                    model = v.profilePictureUrl?.toEmulatorAccessibleUrl(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(v.fullName.ifBlank { v.username }, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.kickVolunteer(event.id, v.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Kick", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            } else {
                // Attendance section placeholder
                Text(text = "Attendance tracking coming soon", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Overlay Preview
    AnimatedVisibility(visible = previewVolunteer != null) {
        previewVolunteer?.let { vol ->
            val user = DomainUser(
                id = vol.id,
                username = vol.username,
                email = "",
                isActive = true,
                fullName = vol.fullName,
                profilePictureUrl = vol.profilePictureUrl,
                userType = com.example.gooddeedfeed.domain.model.DomainUserType.VOLUNTEER,
            )
            com.example.gooddeedfeed.presentation.ui.screens.PreviewProfileScreen(
                user = user,
                onBack = { previewVolunteer = null },
                modifier = Modifier.fillMaxSize(),
            )
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
