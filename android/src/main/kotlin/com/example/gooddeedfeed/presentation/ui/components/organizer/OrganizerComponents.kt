package com.example.gooddeedfeed.presentation.ui.components.organizer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.R
import com.example.gooddeedfeed.domain.model.EventStatus
import com.example.gooddeedfeed.domain.model.SocialMediaLink
import com.example.gooddeedfeed.domain.model.SocialMediaPlatform
import com.example.gooddeedfeed.domain.model.VolunteerEvent
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer

/**
 * Card component for displaying events in organizer view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: VolunteerEvent,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )

                EventStatusChip(status = event.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            EventDetailRow(
                icon = Icons.Default.LocationOn,
                text = event.location,
            )

            EventDetailRow(
                icon = Icons.Default.DateRange,
                text = event.date,
            )

            EventDetailRow(
                icon = Icons.Default.Schedule,
                text = "${event.startTime} - ${event.endTime}",
            )

            EventDetailRow(
                icon = Icons.Default.Group,
                text = "${event.currentVolunteers}/${event.maxVolunteers} volunteers",
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit event",
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete event",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventDetailRow(
    icon: ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EventStatusChip(
    status: EventStatus,
    modifier: Modifier = Modifier,
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
            labelColor = color,
        ),
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(events) { event ->
            EventCard(
                event = event,
                onEditClick = { onEditEvent(event) },
                onDeleteClick = { onDeleteEvent(event.id) },
            )
        }
    }
}


@Composable
fun SocialMediaInputSection(
    socialMediaLinks: List<SocialMediaLink>,
    onLinksChanged: (List<SocialMediaLink>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Social Media Links",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )

            TextButton(
                onClick = { showAddDialog = true },
                enabled = socialMediaLinks.size < 4, // Max 4 platforms
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Link")
            }
        }

        VerticalSpacer(SpacingSize.Small)

        // Display current social media links
        socialMediaLinks.forEach { link ->
            SocialMediaLinkItem(
                link = link,
                onRemove = {
                    onLinksChanged(socialMediaLinks - link)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(SpacingSize.Small)
        }

        if (socialMediaLinks.isEmpty()) {
            Text(
                text = "No social media links added yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }

    // Add social media dialog
    if (showAddDialog) {
        SocialMediaAddDialog(
            existingPlatforms = socialMediaLinks.map { it.platform },
            onDismiss = { showAddDialog = false },
            onAdd = { platform, url ->
                onLinksChanged(socialMediaLinks + SocialMediaLink(platform, url))
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun SocialMediaLinkItem(
    link: SocialMediaLink,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = getSocialMediaIcon(link.platform),
                contentDescription = link.platform.displayName,
                tint = getSocialMediaColor(link.platform),
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.platform.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = link.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SocialMediaAddDialog(
    existingPlatforms: List<SocialMediaPlatform>,
    onDismiss: () -> Unit,
    onAdd: (SocialMediaPlatform, String) -> Unit,
) {
    var selectedPlatform by remember { mutableStateOf<SocialMediaPlatform?>(null) }
    var url by remember { mutableStateOf("") }
    var isValidUrl by remember { mutableStateOf(true) }

    val availablePlatforms = SocialMediaPlatform.values().filter { it !in existingPlatforms }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Social Media Link") },
        text = {
            Column {
                // Platform selection
                Text(
                    text = "Platform",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                VerticalSpacer(SpacingSize.Small)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(availablePlatforms) { platform ->
                        FilterChip(
                            selected = selectedPlatform == platform,
                            onClick = { selectedPlatform = platform },
                            label = { Text(platform.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getSocialMediaIcon(platform),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }

                VerticalSpacer(SpacingSize.Medium)

                // URL input
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        isValidUrl = isValidSocialMediaUrl(it, selectedPlatform)
                    },
                    label = { Text("Profile URL") },
                    placeholder = {
                        Text(getPlaceholderUrl(selectedPlatform))
                    },
                    isError = !isValidUrl,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (!isValidUrl) {
                    VerticalSpacer(SpacingSize.ExtraSmall)
                    Text(
                        text = "Please enter a valid URL",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedPlatform?.let { platform ->
                        if (url.isNotBlank() && isValidUrl) {
                            onAdd(platform, url)
                        }
                    }
                },
                enabled = selectedPlatform != null && url.isNotBlank() && isValidUrl,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun OrganizationImageCarousel(
    images: List<String>,
    onImagesChanged: (List<String>) -> Unit,
    onAddImages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Organization Images",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )

            TextButton(
                onClick = onAddImages,
                enabled = images.size < 10, // Max 10 images
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Images")
            }
        }

        Text(
            text = "${images.size}/10 images",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        VerticalSpacer(SpacingSize.Small)

        if (images.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                itemsIndexed(images) { index, imageUrl ->
                    OrganizationImageItem(
                        imageUrl = imageUrl,
                        onRemove = {
                            onImagesChanged(images - imageUrl)
                        },
                        modifier = Modifier.size(120.dp),
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onAddImages() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        VerticalSpacer(SpacingSize.Small)
                        Text(
                            text = "Add organization images",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganizationImageItem(
    imageUrl: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Organization image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(14.dp),
                ),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove image",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// Helper functions
@Composable
private fun getSocialMediaIcon(platform: SocialMediaPlatform): ImageVector {
    return when (platform) {
        SocialMediaPlatform.INSTAGRAM -> ImageVector.vectorResource(R.drawable.ic_instagram)
        SocialMediaPlatform.FACEBOOK -> ImageVector.vectorResource(R.drawable.ic_facebook)
        SocialMediaPlatform.TWITTER -> ImageVector.vectorResource(R.drawable.ic_twitter)
        SocialMediaPlatform.LINKEDIN -> ImageVector.vectorResource(R.drawable.ic_linkedin)
    }
}

private fun getSocialMediaColor(platform: SocialMediaPlatform): Color {
    return when (platform) {
        SocialMediaPlatform.INSTAGRAM -> Color(0xFFE4405F)
        SocialMediaPlatform.FACEBOOK -> Color(0xFF1877F2)
        SocialMediaPlatform.TWITTER -> Color(0xFF1DA1F2)
        SocialMediaPlatform.LINKEDIN -> Color(0xFF0A66C2)
    }
}

private fun getPlaceholderUrl(platform: SocialMediaPlatform?): String {
    return when (platform) {
        SocialMediaPlatform.INSTAGRAM -> "https://instagram.com/yourprofile"
        SocialMediaPlatform.FACEBOOK -> "https://facebook.com/yourpage"
        SocialMediaPlatform.TWITTER -> "https://twitter.com/yourhandle"
        SocialMediaPlatform.LINKEDIN -> "https://linkedin.com/company/yourcompany"
        null -> "Enter profile URL"
    }
}

private fun isValidSocialMediaUrl(url: String, platform: SocialMediaPlatform?): Boolean {
    if (url.isBlank()) return true // Allow empty for now

    return when (platform) {
        SocialMediaPlatform.INSTAGRAM -> url.contains("instagram.com")
        SocialMediaPlatform.FACEBOOK -> url.contains("facebook.com")
        SocialMediaPlatform.TWITTER -> url.contains("twitter.com") || url.contains("x.com")
        SocialMediaPlatform.LINKEDIN -> url.contains("linkedin.com")
        null -> true
    }
} 
