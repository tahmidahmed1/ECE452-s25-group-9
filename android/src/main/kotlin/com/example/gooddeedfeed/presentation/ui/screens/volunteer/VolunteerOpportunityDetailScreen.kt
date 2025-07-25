package com.example.gooddeedfeed.presentation.ui.screens.volunteer

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
import com.example.gooddeedfeed.data.mapper.toEmulatorAccessibleUrl
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity
import com.example.gooddeedfeed.domain.model.hasPassed

@Composable
fun VolunteerOpportunityDetailScreen(
    opportunity: VolunteerOpportunity,
    onBack: () -> Unit,
    onJoin: (Int) -> Unit,
    onLeave: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(contentAlignment = Alignment.TopStart) {
            val bannerUrl = opportunity.imageUrl?.toEmulatorAccessibleUrl()
            if (bannerUrl != null) {
                AsyncImage(
                    model = bannerUrl,
                    contentDescription = "Event Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                )
            }
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = opportunity.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(icon = Icons.Default.Event, text = opportunity.date)
            InfoRow(icon = Icons.Default.LocationOn, text = opportunity.location)
            InfoRow(icon = Icons.Default.Category, text = opportunity.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
            InfoRow(icon = Icons.Default.Group, text = "${opportunity.currentVolunteers}/${opportunity.requiredVolunteers} volunteers")
            InfoRow(icon = Icons.Default.Star, text = "${opportunity.karmaPoints} Karma Points")
            InfoRow(icon = Icons.Default.Business, text = opportunity.organizationName)

            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Event Photos Section
            if (opportunity.images.isNotEmpty()) {
                Text(
                    text = "Event Photos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(opportunity.images) { image ->
                        AsyncImage(
                            model = image.image_url,
                            contentDescription = "Event Photo",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }
            
            Text(text = opportunity.description, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(24.dp))

            val eventHasPassed = opportunity.hasPassed()
            
            Button(
                onClick = {
                    if (opportunity.isJoined && !eventHasPassed) {
                        onLeave(opportunity.id)
                    } else if (!opportunity.isJoined) {
                        onJoin(opportunity.id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = when {
                    eventHasPassed && opportunity.isJoined -> false // Can't leave past events
                    eventHasPassed -> false // Can't join past events
                    opportunity.isJoined -> true // Can leave current events
                    opportunity.requiredVolunteers == 0 -> true // No limit
                    opportunity.currentVolunteers < opportunity.requiredVolunteers -> true // Space available
                    else -> false // Event is full
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        eventHasPassed -> MaterialTheme.colorScheme.outline
                        opportunity.isJoined -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = when {
                        eventHasPassed -> MaterialTheme.colorScheme.onSurfaceVariant
                        opportunity.isJoined -> MaterialTheme.colorScheme.onError
                        else -> MaterialTheme.colorScheme.onPrimary
                    },
                ),
            ) {
                Text(
                    text = when {
                        eventHasPassed -> "Event Ended"
                        opportunity.isJoined -> "Leave Event"
                        opportunity.requiredVolunteers != 0 && opportunity.currentVolunteers >= opportunity.requiredVolunteers -> "Event Full"
                        else -> "Join Event"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }

            when {
                eventHasPassed -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This event has already ended",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                opportunity.requiredVolunteers != 0 && opportunity.currentVolunteers >= opportunity.requiredVolunteers -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This opportunity is full",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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
