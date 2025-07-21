package com.example.gooddeedfeed.presentation.ui.components.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.R
import com.example.gooddeedfeed.domain.model.EventStatus
import com.example.gooddeedfeed.domain.model.SocialMediaLink
import com.example.gooddeedfeed.domain.model.SocialMediaPlatform
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer

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
