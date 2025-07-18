package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.SocialMediaPlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewProfileScreen(
    user: DomainUser,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Profile Preview",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            item { BasicInfoCard(user) }
            item { ContactInfoCard(user) }
            if (user.userType?.name == "VOLUNTEER") {
                item { VolunteerInfoCard(user) }
                item { EmergencyContactCard(user) }
            }
            if (user.userType?.name == "ORGANIZER") {
                item { OrganizationInfoCard(user) }
                if (!user.organizationSocialMedia.isNullOrEmpty()) {
                    item { SocialMediaCard(user) }
                }
                if (!user.organizationImages.isNullOrEmpty()) {
                    item { OrganizationImagesCard(user) }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BasicInfoCard(user: DomainUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Banner image for organizers
            if (user.userType?.name == "ORGANIZER" && !user.bannerUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = user.bannerUrl,
                    contentDescription = "Organization Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (user.profilePictureUrl != null && user.profilePictureUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.profilePictureUrl,
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
            user.fullName?.let { fullName ->
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Role: ${user.userType?.name ?: "Not set"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ContactInfoCard(user: DomainUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Contact Information")
            user.phone?.let { InfoRow("Phone", it) }
            user.organizationName?.let { InfoRow("Organization", it) }
            user.locationArea?.let { InfoRow("Location", it) }
        }
    }
}

@Composable
private fun VolunteerInfoCard(user: DomainUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Volunteer Information")
            user.description?.let { InfoRow("Description", it) }
            user.skills?.let { InfoRow("Skills", it.joinToString(", ")) }
            user.age?.let { InfoRow("Age", it.toString()) }
            user.sex?.let { InfoRow("Sex", it.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
            if (user.hasDriversLicense == true) InfoRow("Driver's License", "Yes")
            user.disabilities?.let { if (it.isNotBlank()) InfoRow("Disabilities", it) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmergencyContactCard(user: DomainUser) {
    if (user.emergencyContactName != null || user.emergencyContactPhone != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                SectionHeader("Emergency Contact")
                user.emergencyContactName?.let { InfoRow("Name", it) }
                user.emergencyContactPhone?.let { InfoRow("Phone", it) }
            }
        }
    }
}

@Composable
private fun OrganizationInfoCard(user: DomainUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Organization Information")
            user.organizationName?.let { InfoRow("Organization", it) }
            user.organizationType?.let { InfoRow("Type", it.displayName) }
            user.organizationCustomType?.let { 
                if (it.isNotBlank()) InfoRow("Custom Type", it) 
            }
            user.organizationDescription?.let { 
                if (it.isNotBlank()) InfoRow("Description", it) 
            }
            user.organizationWebsite?.let { 
                if (it.isNotBlank()) InfoRow("Website", it) 
            }
            user.karmaPoints.let { InfoRow("Karma Points", it.toString()) }
        }
    }
}

@Composable
private fun SocialMediaCard(user: DomainUser) {
    user.organizationSocialMedia?.let { socialMedia ->
        if (socialMedia.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader("Social Media")
                    socialMedia.forEach { link ->
                        InfoRow(
                            label = link.platform.displayName,
                            value = link.url
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganizationImagesCard(user: DomainUser) {
    user.organizationImages?.let { images ->
        if (images.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader("Organization Images")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(images) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Organization Image",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }
        }
    }
} 
