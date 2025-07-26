package com.example.gooddeedfeed.presentation.ui.screens.organizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.gooddeedfeed.domain.model.EventVolunteer
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.presentation.viewmodel.organizer.EventManagementViewModel
import kotlinx.coroutines.launch

data class VolunteerAttendance(
    val volunteer: EventVolunteer,
    var hoursWorked: String = "",
    var isRejected: Boolean = false,
    var rejectionReason: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerAttendanceScreen(
    event: VolunteerEvent,
    onBack: () -> Unit,
    onSubmit: (List<VolunteerAttendance>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventManagementViewModel = hiltViewModel(),
) {
    var volunteers by remember { mutableStateOf<List<VolunteerAttendance>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf<VolunteerAttendance?>(null) }

    val scope = rememberCoroutineScope()

    // Load volunteers when screen opens
    LaunchedEffect(event.id) {
        isLoading = true
        error = null

        scope.launch {
            viewModel.getEventVolunteersForAttendance(event.id).fold(
                onSuccess = { eventVolunteers ->
                    volunteers = eventVolunteers.map { volunteer ->
                        VolunteerAttendance(volunteer = volunteer)
                    }
                    isLoading = false
                },
                onFailure = { exception ->
                    error = exception.message ?: "Failed to load volunteers"
                    isLoading = false
                },
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TopAppBar(
            title = { Text("Volunteer Attendance") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Event info header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${event.date} • ${event.startTime} - ${event.endTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Volunteers (${volunteers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(volunteers) { volunteer ->
                    VolunteerAttendanceCard(
                        volunteer = volunteer,
                        onHoursChanged = { newHours ->
                            volunteers = volunteers.map {
                                if (it.volunteer.id == volunteer.volunteer.id) {
                                    it.copy(hoursWorked = newHours, isRejected = false)
                                } else {
                                    it
                                }
                            }
                        },
                        onRejectClick = {
                            showRejectDialog = volunteer
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSubmit(volunteers) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = volunteers.all { it.hoursWorked.isNotBlank() || it.isRejected },
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Attendance")
            }
        }
    }

    // Rejection dialog
    showRejectDialog?.let { volunteer ->
        RejectVolunteerDialog(
            volunteer = volunteer,
            onDismiss = { showRejectDialog = null },
            onConfirm = { reason ->
                volunteers = volunteers.map {
                    if (it.volunteer.id == volunteer.volunteer.id) {
                        it.copy(isRejected = true, rejectionReason = reason, hoursWorked = "")
                    } else {
                        it
                    }
                }
                showRejectDialog = null
            },
        )
    }
}

@Composable
private fun VolunteerAttendanceCard(
    volunteer: VolunteerAttendance,
    onHoursChanged: (String) -> Unit,
    onRejectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Profile picture
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    if (volunteer.volunteer.profilePictureUrl != null) {
                        AsyncImage(
                            model = volunteer.volunteer.profilePictureUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = volunteer.volunteer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "@${volunteer.volunteer.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = volunteer.volunteer.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (volunteer.isRejected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = "REJECTED",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = volunteer.rejectionReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = volunteer.hoursWorked,
                        onValueChange = onHoursChanged,
                        label = { Text("Hours worked") },
                        placeholder = { Text("0.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                    )

                    OutlinedButton(
                        onClick = onRejectClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
private fun RejectVolunteerDialog(
    volunteer: VolunteerAttendance,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var rejectionReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Volunteer") },
        text = {
            Column {
                Text("Please provide a reason for rejecting ${volunteer.volunteer.name}:")
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
                onClick = { onConfirm(rejectionReason) },
                enabled = rejectionReason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Reject")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
