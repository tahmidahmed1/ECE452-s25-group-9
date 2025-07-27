package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.R
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.SocialMediaPlatform
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
            ContactInfoCard(user)
            OrganizerInfoCard(user)
            if (!user.organizationSocialMedia.isNullOrEmpty()) {
                SocialMediaCard(user)
            }
            android.util.Log.d("PreviewProfileScreen", "📸 Checking organizationImages condition")
            android.util.Log.d("PreviewProfileScreen", "📸 organizationImages is null: ${user.organizationImages == null}")
            android.util.Log.d("PreviewProfileScreen", "📸 organizationImages is empty: ${user.organizationImages?.isEmpty() == true}")
            android.util.Log.d("PreviewProfileScreen", "📸 organizationImages size: ${user.organizationImages?.size ?: 0}")
            if (!user.organizationImages.isNullOrEmpty()) {
                android.util.Log.d("PreviewProfileScreen", "📸 Condition passed, calling OrganizationImagesCard")
                OrganizationImagesCard(user)
            } else {
                android.util.Log.d("PreviewProfileScreen", "📸 Condition failed, NOT showing OrganizationImagesCard")
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
            if (user.userType?.name == "ORGANIZER") {
                val bannerImage = when {
                    !user.organizationImages.isNullOrEmpty() -> user.organizationImages.first()
                    else -> null
                }

                if (bannerImage != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        AsyncImage(
                            model = bannerImage,
                            contentDescription = "Organization Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )

                        if (user.profilePictureUrl != null && user.profilePictureUrl.isNotEmpty()) {
                            AsyncImage(
                                model = user.profilePictureUrl,
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .size(200.dp)
                                    .offset(y = 60.dp)
                                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .offset(y = 60.dp)
                                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Profile Picture",
                                    modifier = Modifier.size(100.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(72.dp))
                } else {
                    if (user.profilePictureUrl != null && user.profilePictureUrl.isNotEmpty()) {
                        AsyncImage(
                            model = user.profilePictureUrl,
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .size(200.dp)
                                .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Profile Picture",
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                if (user.profilePictureUrl != null && user.profilePictureUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.profilePictureUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(200.dp)
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile Picture",
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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

            user.age?.let { InfoRow("Age", it.toString()) }
            user.sex?.let { InfoRow("Gender", it.toDisplayString()) }

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

            user.locationArea?.let { InfoRow("Preferred Area", it) }
            user.hasDriversLicense?.let { InfoRow("Driver's License", if (it) "Yes" else "No") }

            user.disabilities?.let { disabilities ->
                if (disabilities.isNotBlank()) {
                    InfoRow("Accessibility Needs", disabilities)
                }
            }

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
private fun OrganizerInfoCard(user: DomainUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Organization Information")

            user.organizationName?.let { InfoRow("Organization Name", it) }

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

            user.organizationWebsite?.let { website ->
                if (website.isNotBlank()) {
                    InfoRow("Website", website)
                }
            }

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
                        SocialMediaRow(
                            platform = link.platform,
                            url = link.url,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialMediaRow(platform: SocialMediaPlatform, url: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = getSocialMediaIcon(platform),
                contentDescription = platform.displayName,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = platform.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun OrganizationImagesCard(user: DomainUser) {
    android.util.Log.d("PreviewProfileScreen", "📸 OrganizationImagesCard called")
    android.util.Log.d("PreviewProfileScreen", "📸 User organizationImages: ${user.organizationImages?.size ?: 0} items")
    android.util.Log.d("PreviewProfileScreen", "📸 Organization images URLs: ${user.organizationImages}")
    
    user.organizationImages?.let { images ->
        android.util.Log.d("PreviewProfileScreen", "📸 Images not null, checking if empty")
        if (images.isNotEmpty()) {
            android.util.Log.d("PreviewProfileScreen", "📸 Images not empty, showing ${images.size} images")
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
                            android.util.Log.d("PreviewProfileScreen", "📸 Loading image: $imageUrl")
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Organization Image",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                onError = { error ->
                                    android.util.Log.e("PreviewProfileScreen", "📸 Failed to load image: $imageUrl, error: ${error.result.throwable}")
                                },
                                onSuccess = { success ->
                                    android.util.Log.d("PreviewProfileScreen", "📸 Successfully loaded image: $imageUrl")
                                }
                            )
                        }
                    }
                }
            }
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
