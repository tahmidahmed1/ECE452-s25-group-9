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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.domain.model.DomainOrganizerProfile
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
    modifier: Modifier = Modifier
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
    
    // UI state
    var isOrgTypeDropdownExpanded by remember { mutableStateOf(false) }

    // Profile picture launcher
    val profilePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = ImageUtils.saveUriToFile(context, it)
            profilePictureFile = file
            profilePictureUri = it
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Complete Your Profile",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        VerticalSpacer(SpacingSize.Small)

        Text(
            text = when (userType) {
                DomainUserType.VOLUNTEER -> "Tell us about yourself to get started with volunteering"
                DomainUserType.ORGANIZER -> "Set up your organization profile to start creating events"
                else -> "Complete your profile to continue"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        VerticalSpacer(SpacingSize.Large)

        // Profile Picture Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profile Picture",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                VerticalSpacer(SpacingSize.Small)
                
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { profilePictureLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePictureUri != null) {
                        AsyncImage(
                            model = profilePictureUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                    Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Add Profile Picture",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            VerticalSpacer(SpacingSize.Small)

            Text(
                    text = "Tap to add photo",
                style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            }
        }

        VerticalSpacer(SpacingSize.Medium)

        // Basic Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                VerticalSpacer(SpacingSize.Medium)
                
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
                    isError = fullNameError != null
        )

        if (fullNameError != null) {
            Text(
                text = fullNameError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

                VerticalSpacer(SpacingSize.Small)

        OutlinedTextField(
            value = phone,
                    onValueChange = { phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null
        )

        if (phoneError != null) {
            Text(
                text = phoneError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
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
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Organization Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        
                        VerticalSpacer(SpacingSize.Medium)
                        
                OutlinedTextField(
                    value = organizationName,
                    onValueChange = { organizationName = it },
                    label = { Text("Organization Name") },
                    modifier = Modifier.fillMaxWidth(),
                            isError = organizationError != null
                )

                if (organizationError != null) {
                    Text(
                        text = organizationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
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
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown"
                                    )
                                },
                                isError = orgTypeError != null
                            )
                            
                            DropdownMenu(
                                expanded = isOrgTypeDropdownExpanded,
                                onDismissRequest = { isOrgTypeDropdownExpanded = false }
                    ) {
                                OrganizationType.values().forEach { type ->
                            DropdownMenuItem(
                                        text = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                            organizationType = type
                                            isOrgTypeDropdownExpanded = false
                                        }
                            )
                        }
                    }
                }

                        if (orgTypeError != null) {
                    Text(
                                text = orgTypeError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
                        }
                
                VerticalSpacer(SpacingSize.Small)
                
                        OutlinedTextField(
                            value = organizationDescription,
                            onValueChange = { organizationDescription = it },
                            label = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                )

                VerticalSpacer(SpacingSize.Small)

                        OutlinedTextField(
                            value = organizationWebsite,
                            onValueChange = { organizationWebsite = it },
                            label = { Text("Website (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        
                        if (organizationType == OrganizationType.CUSTOM) {
                            VerticalSpacer(SpacingSize.Small)
                            
                            OutlinedTextField(
                                value = organizationCustomType,
                                onValueChange = { organizationCustomType = it },
                                label = { Text("Custom Organization Type") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                        DomainOrganizerProfile(
                            fullName = fullName,
                            phone = phone,
                            organizationName = organizationName,
                        organizationType = organizationType!!,
                        organizationDescription = organizationDescription.takeIf { it.isNotBlank() },
                        organizationWebsite = organizationWebsite.takeIf { it.isNotBlank() },
                        organizationSocialMedia = null,
                        organizationImages = null,
                        organizationCustomType = organizationCustomType.takeIf { it.isNotBlank() }
                        )
                    } else null
                    
                    onComplete(
                        fullName,
                        phone,
                        if (userType == DomainUserType.ORGANIZER) organizationName else null,
                        profilePictureFile,
                    organizerProfile
                    )
                },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isFormValid
        ) {
            Text(
                text = "Complete Profile",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        VerticalSpacer(SpacingSize.Medium)
    }
} 
