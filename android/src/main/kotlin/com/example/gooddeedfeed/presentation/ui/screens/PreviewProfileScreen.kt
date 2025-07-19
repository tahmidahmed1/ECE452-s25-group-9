package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.gooddeedfeed.domain.model.toDisplayString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewProfileScreen(
    user: DomainUser,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (user.userType?.name) {
        "VOLUNTEER" -> VolunteerPreviewProfileScreen(user, onBack, modifier)
        "ORGANIZER" -> OrganizerPreviewProfileScreen(user, onBack, modifier)
        else -> {
            // Fallback for users without userType
            VolunteerPreviewProfileScreen(user, onBack, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerPreviewProfileScreen(
    user: DomainUser,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
            BasicInfoCard(user)
            ContactInfoCard(user)
            VolunteerInfoCard(user)
            EmergencyContactCard(user)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerPreviewProfileScreen(
    user: DomainUser,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
            BasicInfoCard(user)
            AccountInfoCard(user)
            ContactInfoCard(user)
            OrganizerInfoCard(user)
            if (!user.organizationSocialMedia.isNullOrEmpty()) {
                SocialMediaCard(user)
            }
            if (!user.organizationImages.isNullOrEmpty()) {
                OrganizationImagesCard(user)
            }
            Spacer(modifier = Modifier.height(24.dp))
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AccountInfoCard(user: DomainUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Account Information")
            user.createdAt?.let { InfoRow("Member Since", it) }
            user.updatedAt?.let { InfoRow("Last Updated", it) }
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
            InfoRow("Email", user.email)
            user.phone?.let { InfoRow("Phone", it) }
            user.organizationName?.let { InfoRow("Organization", it) }
            // Only show location for organizers in contact info
            if (user.userType?.name == "ORGANIZER") {
                user.locationArea?.let { InfoRow("Location", it) }
            }
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

            // Personal Details
            user.age?.let { InfoRow("Age", it.toString()) }
            user.sex?.let { InfoRow("Gender", it.toDisplayString()) }

            // About/Description
            user.description?.let { description ->
                if (description.isNotBlank()) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                    )
                }
            }

            // Skills
            user.skills?.let { skills ->
                if (skills.isNotEmpty()) {
                    Text(
                        text = "Skills & Interests",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    Text(
                        text = skills.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                    )
                }
            }

            // Location & Transportation
            user.locationArea?.let { InfoRow("Preferred Area", it) }
            user.hasDriversLicense?.let { InfoRow("Driver's License", if (it) "Yes" else "No") }

            // Accessibility
            user.disabilities?.let { disabilities ->
                if (disabilities.isNotBlank()) {
                    InfoRow("Accessibility Needs", disabilities)
                }
            }

            // Stats
            InfoRow("Karma Points", user.karmaPoints.toString())
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
    if (user.emergencyContactName?.isNotBlank() == true || user.emergencyContactPhone?.isNotBlank() == true) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                SectionHeader("Emergency Contact")
                user.emergencyContactName?.let { name ->
                    if (name.isNotBlank()) InfoRow("Name", name)
                }
                user.emergencyContactPhone?.let { phone ->
                    if (phone.isNotBlank()) InfoRow("Phone", phone)
                }
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

            // Organization Basic Info
            user.organizationName?.let { InfoRow("Organization Name", it) }

            // Organization Description
            user.organizationDescription?.let { description ->
                if (description.isNotBlank()) {
                    Text(
                        text = "About Organization",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                    )
                }
            }

            // Website
            user.organizationWebsite?.let { website ->
                if (website.isNotBlank()) {
                    InfoRow("Website", website)
                }
            }

            // Location
            user.locationArea?.let { InfoRow("Location", it) }
        }
    }
}

@Composable
private fun OrganizerInfoCard(user: DomainUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Organization Information")

            // Organization Basic Info
            user.organizationName?.let { InfoRow("Organization Name", it) }

            // Organization Description
            user.organizationDescription?.let { description ->
                if (description.isNotBlank()) {
                    Text(
                        text = "About Organization",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                    )
                }
            }

            // Website
            user.organizationWebsite?.let { website ->
                if (website.isNotBlank()) {
                    InfoRow("Website", website)
                }
            }

            // Location
            user.locationArea?.let { InfoRow("Location", it) }
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
                            value = link.url,
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
