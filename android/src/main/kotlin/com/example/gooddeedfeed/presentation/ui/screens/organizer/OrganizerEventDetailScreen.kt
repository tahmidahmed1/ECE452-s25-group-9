package com.example.gooddeedfeed.presentation.ui.screens.organizer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.gooddeedfeed.data.mapper.toEmulatorAccessibleUrl
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.presentation.viewmodel.organizer.EventManagementViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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

    fun isEventCompleted(event: VolunteerEvent): Boolean {
        return try {
            Log.d("AttendanceDebug", "=== CHECKING EVENT COMPLETION ===")
            Log.d("AttendanceDebug", "Event ID: ${event.id}")
            Log.d("AttendanceDebug", "Event Title: ${event.title}")
            Log.d("AttendanceDebug", "Raw event date: '${event.date}'")
            Log.d("AttendanceDebug", "Raw event endTime: '${event.endTime}'")

            // Parse date using multiple formats (copied from VolunteerOpportunity.hasPassed())
            fun parseDate(dateStr: String): LocalDate? {
                val patterns = listOf("yyyy-MM-dd", "MMM d, yyyy", "MMMM d, yyyy")
                for (p in patterns) {
                    runCatching {
                        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(p, java.util.Locale.ENGLISH))
                    }
                }
                return null
            }

            // Parse time using multiple formats (copied from VolunteerOpportunity.hasPassed())
            fun parseTime(timeStr: String): LocalTime? {
                val patterns = listOf("H:mm", "H:mm:ss", "h:mm a", "hh:mm a")
                for (p in patterns) {
                    runCatching {
                        return LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern(p, java.util.Locale.ENGLISH))
                    }
                }
                return null
            }

            val eventDate = parseDate(event.date)
            val eventEndTime = parseTime(event.endTime)

            if (eventDate == null || eventEndTime == null) {
                Log.e("AttendanceDebug", "Failed to parse date or time - eventDate: $eventDate, eventEndTime: $eventEndTime")
                return false // Default to not completed if parsing fails
            }

            val currentDate = LocalDate.now()
            val currentTime = LocalTime.now()
            val eventEndDateTime = LocalDateTime.of(eventDate, eventEndTime)
            val currentDateTime = LocalDateTime.now()

            Log.d("AttendanceDebug", "Parsed event date: $eventDate")
            Log.d("AttendanceDebug", "Parsed event end time: $eventEndTime")
            Log.d("AttendanceDebug", "Current date: $currentDate")
            Log.d("AttendanceDebug", "Current time: $currentTime")
            Log.d("AttendanceDebug", "Event end date-time: $eventEndDateTime")
            Log.d("AttendanceDebug", "Current date-time: $currentDateTime")

            val isCompleted = eventEndDateTime.isBefore(currentDateTime)
            Log.d("AttendanceDebug", "Is event completed? $isCompleted")
            Log.d("AttendanceDebug", "eventEndDateTime.isBefore(currentDateTime): $isCompleted")
            Log.d("AttendanceDebug", "=== END EVENT COMPLETION CHECK ===")

            isCompleted
        } catch (e: Exception) {
            Log.e("AttendanceDebug", "Error checking event completion", e)
            Log.e("AttendanceDebug", "Event date format: '${event.date}', endTime format: '${event.endTime}'")
            false // Default to not completed if parsing fails
        }
    }

    val eventCompleted = isEventCompleted(event)
    val showAttendanceSection = eventCompleted

    Log.d("AttendanceDebug", "=== ATTENDANCE SECTION DECISION ===")
    Log.d("AttendanceDebug", "eventCompleted: $eventCompleted")
    Log.d("AttendanceDebug", "showAttendanceSection: $showAttendanceSection")
    Log.d("AttendanceDebug", "=== END ATTENDANCE SECTION DECISION ===")

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

            Log.d("AttendanceDebug", "=== UI SECTION DISPLAY ===")
            Log.d("AttendanceDebug", "showAttendanceSection in UI: $showAttendanceSection")
            Log.d("AttendanceDebug", "Section title will be: ${if (showAttendanceSection) "Volunteer Attendance" else "Volunteers Joined"}")

            Text(
                text = if (showAttendanceSection) "Volunteer Attendance" else "Volunteers Joined",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!showAttendanceSection) {
                Log.d("AttendanceDebug", "=== DISPLAYING VOLUNTEERS JOINED SECTION ===")
                Log.d("AttendanceDebug", "Volunteers count: ${volunteers.size}")
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
                                OutlinedButton(
                                    onClick = { viewModel.kickVolunteer(event.id, v.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    Text("Reject", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            } else {
                // Attendance section
                Log.d("AttendanceDebug", "=== DISPLAYING ATTENDANCE SECTION ===")
                Log.d("AttendanceDebug", "Volunteers count for attendance: ${volunteers.size}")
                if (volunteers.isEmpty()) {
                    Text(text = "No volunteers to track attendance for", style = MaterialTheme.typography.bodyMedium)
                } else {
                    AttendanceSection(volunteers = volunteers, eventId = event.id, viewModel = viewModel)
                }
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

@Composable
private fun AttendanceSection(
    volunteers: List<com.example.gooddeedfeed.domain.model.JoinedVolunteer>,
    eventId: Int,
    viewModel: EventManagementViewModel,
) {
    var volunteerHours by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    var rejectionDialogVolunteer by remember { mutableStateOf<com.example.gooddeedfeed.domain.model.JoinedVolunteer?>(null) }
    var approvedVolunteers by remember { mutableStateOf(setOf<Int>()) }
    var rejectedVolunteers by remember { mutableStateOf(setOf<Int>()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        volunteers.forEach { volunteer ->
            val isApproved = approvedVolunteers.contains(volunteer.id)
            val isRejected = rejectedVolunteers.contains(volunteer.id)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isApproved -> MaterialTheme.colorScheme.primaryContainer
                        isRejected -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AsyncImage(
                            model = volunteer.profilePictureUrl?.toEmulatorAccessibleUrl(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = volunteer.fullName.ifBlank { volunteer.username },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "@${volunteer.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (!isApproved && !isRejected) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Hours input
                        OutlinedTextField(
                            value = volunteerHours[volunteer.id] ?: "",
                            onValueChange = { hours ->
                                volunteerHours = volunteerHours.toMutableMap().apply {
                                    put(volunteer.id, hours)
                                }
                            },
                            label = { Text("Hours contributed") },
                            placeholder = { Text("0.0") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val hours = volunteerHours[volunteer.id]?.toDoubleOrNull() ?: 0.0
                                    if (hours > 0) {
                                        approvedVolunteers = approvedVolunteers + volunteer.id
                                        // TODO: Call viewModel to submit attendance with hours
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = (volunteerHours[volunteer.id]?.toDoubleOrNull() ?: 0.0) > 0,
                            ) {
                                Text("Approve")
                            }

                            OutlinedButton(
                                onClick = { rejectionDialogVolunteer = volunteer },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Reject")
                            }
                        }
                    } else if (isApproved) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Approved - ${volunteerHours[volunteer.id] ?: "0"} hours",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    } else if (isRejected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✗ Rejected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }

    // Rejection dialog
    rejectionDialogVolunteer?.let { volunteer ->
        var rejectionReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { rejectionDialogVolunteer = null },
            title = { Text("Reject Volunteer") },
            text = {
                Column {
                    Text("Please provide a reason for rejecting ${volunteer.fullName.ifBlank { volunteer.username }}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        placeholder = { Text("Enter reason...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReason.isNotBlank()) {
                            rejectedVolunteers = rejectedVolunteers + volunteer.id
                            rejectionDialogVolunteer = null
                            // TODO: Call viewModel to submit rejection with reason
                        }
                    },
                    enabled = rejectionReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { rejectionDialogVolunteer = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }
} 
