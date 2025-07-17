package com.example.gooddeedfeed.presentation.ui.screens.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.domain.model.DomainOrganizerProfile
import com.example.gooddeedfeed.domain.model.SocialMediaLink
import com.example.gooddeedfeed.domain.model.SocialMediaPlatform
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.OrganizationType
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStepTwoScreen(
    userType: DomainUserType?,
    onComplete: (fullName: String, phone: String, organizationName: String?, profilePictureFile: File?, organizerProfile: DomainOrganizerProfile?) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Common form fields
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var profilePictureFile by remember { mutableStateOf<File?>(null) }
    var profilePictureUri by remember { mutableStateOf<Uri?>(null) }

    // Organizer-specific fields
    var organizationName by remember { mutableStateOf("") }
    var organizationType by remember { mutableStateOf<OrganizationType?>(null) }
    var organizationDescription by remember { mutableStateOf("") }
    var organizationWebsite by remember { mutableStateOf("") }
    var organizationCustomType by remember { mutableStateOf("") }
    
    // Social media fields
    var instagramHandle by remember { mutableStateOf("") }
    var twitterHandle by remember { mutableStateOf("") }
    var facebookPage by remember { mutableStateOf("") }
    
    // Organization images
    var organizationImageFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var mainOrgImageIndex by remember { mutableStateOf(0) }

    // UI state
    var isOrgTypeDropdownExpanded by remember { mutableStateOf(false) }

    // Profile picture launcher
    val profilePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            val file = ImageUtils.saveUriToFile(context, it)
            profilePictureFile = file
            profilePictureUri = it
        }
    }
    
    // Organization image launcher
    val orgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            ImageUtils.saveUriToFile(context, it)?.let { file -> 
                if (organizationImageFiles.size < 10) {
                    organizationImageFiles = organizationImageFiles + file
                }
            }
        }
    }

    // Form validation
    val fullNameError: String? by remember(fullName) {
        derivedStateOf { ImageUtils.FormValidation.validateFullName(fullName) }
    }

    val phoneError: String? by remember(phone) {
        derivedStateOf { ImageUtils.FormValidation.validatePhone(phone) }
    }

    val organizationError: String? by remember(organizationName, userType) {
        derivedStateOf { ImageUtils.FormValidation.validateOrganizationName(organizationName, userType) }
    }

    val orgTypeError: String? by remember(organizationType, userType) {
        derivedStateOf { ImageUtils.FormValidation.validateOrganizationType(organizationType, userType) }
    }

    val isFormValid = fullNameError == null && phoneError == null && organizationError == null && orgTypeError == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header with back arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            
            Text(
                text = "Complete Your Profile",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            
            // Invisible spacer to center the title
            Box(modifier = Modifier.size(48.dp))
        }

        VerticalSpacer(SpacingSize.Small)

        Text(
            text = when (userType) {
                DomainUserType.VOLUNTEER -> "Tell us about yourself to get started with volunteering"
                DomainUserType.ORGANIZER -> "Set up your organization profile to start creating events"
                else -> "Complete your profile to continue"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        VerticalSpacer(SpacingSize.Large)

        // Profile Picture Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Profile Picture",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )

                VerticalSpacer(SpacingSize.Small)

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { profilePictureLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    if (profilePictureUri != null) {
                        AsyncImage(
                            model = profilePictureUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Add Profile Picture",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                VerticalSpacer(SpacingSize.Small)

                Text(
                    text = "Tap to add photo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        VerticalSpacer(SpacingSize.Medium)

        // Basic Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )

                VerticalSpacer(SpacingSize.Medium)

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = fullNameError != null,
                )

                if (fullNameError != null) {
                    Text(
                        text = fullNameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )
                }

                VerticalSpacer(SpacingSize.Small)

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                )

                if (phoneError != null) {
                    Text(
                        text = phoneError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )
                }
            }
        }

        // User type specific fields
        when (userType) {
            DomainUserType.ORGANIZER -> {
                VerticalSpacer(SpacingSize.Medium)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = "Organization Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )

                        VerticalSpacer(SpacingSize.Medium)

                        OutlinedTextField(
                            value = organizationName,
                            onValueChange = { organizationName = it },
                            label = { Text("Organization Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = organizationError != null,
                        )

                        if (organizationError != null) {
                            Text(
                                text = organizationError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                            )
                        }

                        VerticalSpacer(SpacingSize.Small)

                        // Organization Type Dropdown
                        Box {
                            OutlinedTextField(
                                value = organizationType?.name?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Organization Type") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isOrgTypeDropdownExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                    )
                                },
                                isError = orgTypeError != null,
                            )

                            DropdownMenu(
                                expanded = isOrgTypeDropdownExpanded,
                                onDismissRequest = { isOrgTypeDropdownExpanded = false },
                            ) {
                                OrganizationType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            organizationType = type
                                            isOrgTypeDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        if (orgTypeError != null) {
                            Text(
                                text = orgTypeError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                            )
                        }

                        VerticalSpacer(SpacingSize.Small)

                        OutlinedTextField(
                            value = organizationDescription,
                            onValueChange = { organizationDescription = it },
                            label = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3,
                            maxLines = 5,
                        )

                        VerticalSpacer(SpacingSize.Small)

                        OutlinedTextField(
                            value = organizationWebsite,
                            onValueChange = { organizationWebsite = it },
                            label = { Text("Website (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )

                        if (organizationType == OrganizationType.CUSTOM) {
                            VerticalSpacer(SpacingSize.Small)

                            OutlinedTextField(
                                value = organizationCustomType,
                                onValueChange = { organizationCustomType = it },
                                label = { Text("Custom Organization Type") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                        
                        VerticalSpacer(SpacingSize.Medium)
                        
                        // Social media fields
                        Text(
                            text = "Social Media (Optional)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        
                        VerticalSpacer(SpacingSize.Small)
                        
                        OutlinedTextField(
                            value = instagramHandle,
                            onValueChange = { instagramHandle = it },
                            label = { Text("Instagram Handle") },
                            placeholder = { Text("@username") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                        
                        VerticalSpacer(SpacingSize.Small)
                        
                        OutlinedTextField(
                            value = twitterHandle,
                            onValueChange = { twitterHandle = it },
                            label = { Text("Twitter Handle") },
                            placeholder = { Text("@username") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                        
                        VerticalSpacer(SpacingSize.Small)
                        
                        OutlinedTextField(
                            value = facebookPage,
                            onValueChange = { facebookPage = it },
                            label = { Text("Facebook Page") },
                            placeholder = { Text("Page name or URL") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                        
                        VerticalSpacer(SpacingSize.Medium)
                        
                        // Organization images carousel
                        Text(
                            text = "Organization Images",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        
                        VerticalSpacer(SpacingSize.Small)
                        
                        OrganizerImageCarousel(
                            selectedImages = organizationImageFiles,
                            mainImageIndex = mainOrgImageIndex,
                            onAddImage = { 
                                if (organizationImageFiles.size < 10) {
                                    orgImageLauncher.launch("image/*")
                                }
                            },
                            onRemoveImage = { index ->
                                organizationImageFiles = organizationImageFiles.filterIndexed { i, _ -> i != index }
                                if (mainOrgImageIndex >= organizationImageFiles.size && organizationImageFiles.isNotEmpty()) {
                                    mainOrgImageIndex = organizationImageFiles.size - 1
                                } else if (organizationImageFiles.isEmpty()) {
                                    mainOrgImageIndex = 0
                                }
                            },
                            onSetMainImage = { index ->
                                mainOrgImageIndex = index
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            else -> {
                // For volunteers, we'll handle detailed profile in a separate step
            }
        }

        VerticalSpacer(SpacingSize.ExtraLarge)

        // Complete Button
        Button(
            onClick = {
                val organizerProfile = if (userType == DomainUserType.ORGANIZER) {
                    val socialMedia = mutableListOf<SocialMediaLink>().apply {
                        if (instagramHandle.isNotBlank()) {
                            add(SocialMediaLink(SocialMediaPlatform.INSTAGRAM, instagramHandle))
                        }
                        if (twitterHandle.isNotBlank()) {
                            add(SocialMediaLink(SocialMediaPlatform.TWITTER, twitterHandle))
                        }
                        if (facebookPage.isNotBlank()) {
                            add(SocialMediaLink(SocialMediaPlatform.FACEBOOK, facebookPage))
                        }
                    }.takeIf { it.isNotEmpty() }
                    
                    DomainOrganizerProfile(
                        fullName = fullName,
                        phone = phone,
                        organizationName = organizationName,
                        organizationType = organizationType!!,
                        organizationDescription = organizationDescription.takeIf { it.isNotBlank() },
                        organizationWebsite = organizationWebsite.takeIf { it.isNotBlank() },
                        organizationSocialMedia = socialMedia,
                        organizationImages = organizationImageFiles.takeIf { it.isNotEmpty() }?.map { it.absolutePath },
                        organizationCustomType = organizationCustomType.takeIf { it.isNotBlank() },
                    )
                } else {
                    null
                }

                onComplete(
                    fullName,
                    phone,
                    if (userType == DomainUserType.ORGANIZER) organizationName else null,
                    profilePictureFile,
                    organizerProfile,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = isFormValid,
        ) {
            Text(
                text = "Complete Profile",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        VerticalSpacer(SpacingSize.Medium)
    }
}

@Composable
private fun OrganizerImageCarousel(
    selectedImages: List<File>,
    mainImageIndex: Int,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSetMainImage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        if (selectedImages.isEmpty()) {
            // Empty state - entire tile clickable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAddImage() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Image",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Add Organization Images",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Showcase your organization (up to 10 images)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Show carousel with images
            Column(modifier = Modifier.fillMaxSize()) {
                // Main image display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    AsyncImage(
                        model = selectedImages[mainImageIndex],
                        contentDescription = "Main Organization Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Main image badge
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "MAIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Remove button
                    androidx.compose.material3.IconButton(
                        onClick = { onRemoveImage(mainImageIndex) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                CircleShape
                            )
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Thumbnail row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetMainImage(index) }
                                .then(
                                    if (index == mainImageIndex) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(8.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            AsyncImage(
                                model = selectedImages[index],
                                contentDescription = "Organization Image ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    // Add more button
                    if (selectedImages.size < 10) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAddImage() }
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add More Images",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 
