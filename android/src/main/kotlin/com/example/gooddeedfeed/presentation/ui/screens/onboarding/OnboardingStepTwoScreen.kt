package com.example.gooddeedfeed.presentation.ui.screens.onboarding

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.gooddeedfeed.domain.model.DomainInstitutionName
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStepTwoScreen(
    userType: DomainUserType,
    onComplete: (fullName: String, phone: String, organizationName: String?, institutionName: DomainInstitutionName?, profilePictureFile: File?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var organizationName by remember { mutableStateOf("") }
    var selectedInstitution by remember { mutableStateOf<DomainInstitutionName?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var profilePictureFile by remember { mutableStateOf<File?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val file = ImageUtils.saveBitmapToFile(context, it)
            if (file != null) {
                profilePictureFile = file
                selectedImageUri = Uri.fromFile(file)
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val file = ImageUtils.saveUriToFile(context, it)
            if (file != null) {
                profilePictureFile = file
            }
        }
    }

    val fullNameError: String? by remember(fullName) {
        derivedStateOf { ImageUtils.FormValidation.validateFullName(fullName) }
    }

    val phoneError: String? by remember(phone) {
        derivedStateOf { ImageUtils.FormValidation.validatePhone(phone) }
    }

    val organizationError: String? by remember(organizationName, userType) {
        derivedStateOf { ImageUtils.FormValidation.validateOrganization(organizationName, userType) }
    }

    val institutionError: String? by remember(selectedInstitution, userType) {
        derivedStateOf { ImageUtils.FormValidation.validateInstitution(selectedInstitution, userType) }
    }

    val isFormValid = fullNameError == null && phoneError == null && organizationError == null && institutionError == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VerticalSpacer(SpacingSize.Large)

        Text(
            text = "Almost there!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        VerticalSpacer(SpacingSize.Small)

        Text(
            text = "Please provide your contact information",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        VerticalSpacer(SpacingSize.Large)

        // Profile Picture Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                    .clickable { showImageSourceDialog = true }
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default profile picture",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Add/edit indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            VerticalSpacer(SpacingSize.Small)

            Text(
                text = "Add Profile Picture (Optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Tap to select from camera or gallery",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        VerticalSpacer(SpacingSize.Large)

        // Full Name Field
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            isError = fullNameError != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )

        if (fullNameError != null) {
            Text(
                text = fullNameError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start),
            )
        }

        VerticalSpacer(SpacingSize.Medium)

        // Phone Field (+1 numbers, auto-formatted as XXX-XXX-XXXX)
        OutlinedTextField(
            value = phone,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                phone = ImageUtils.formatPhoneNumber(digits)
            },
            label = { Text("Phone Number") },
            isError = phoneError != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )

        if (phoneError != null) {
            Text(
                text = phoneError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start),
            )
        }

        Text(
            text = "Only +1 North American phone numbers are supported",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )

        VerticalSpacer(SpacingSize.Medium)

        // User type specific fields
        when (userType) {
            DomainUserType.ORGANIZER -> {
                OutlinedTextField(
                    value = organizationName,
                    onValueChange = { organizationName = it },
                    label = { Text("Organization Name") },
                    isError = organizationError != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                if (organizationError != null) {
                    Text(
                        text = organizationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start),
                    )
                }
            }
            DomainUserType.INSTITUTION -> {
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Use deprecated menuAnchor() for dropdown positioning
                    @Suppress("DEPRECATION")
                    val dropdownModifier = Modifier.menuAnchor().fillMaxWidth()
                    OutlinedTextField(
                        value = selectedInstitution?.name?.replace("_", " ") ?: "",
                        onValueChange = { _ -> },
                        modifier = dropdownModifier,
                        readOnly = true,
                        label = { Text("Select Institution") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                    ) {
                        DomainInstitutionName.values().forEach { institution ->
                            DropdownMenuItem(
                                text = { Text(institution.name.replace("_", " ")) },
                                onClick = {
                                    selectedInstitution = institution
                                    isDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                if (institutionError != null) {
                    Text(
                        text = institutionError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start),
                    )
                }
            }
            DomainUserType.VOLUNTEER -> {
                // No additional fields for volunteers
            }
        }

        VerticalSpacer(SpacingSize.Large)
        VerticalSpacer(SpacingSize.Medium)

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Back",
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            PrimaryButton(
                text = "Complete",
                onClick = {
                    onComplete(
                        fullName,
                        phone,
                        if (userType == DomainUserType.ORGANIZER) organizationName else null,
                        if (userType == DomainUserType.INSTITUTION) selectedInstitution else null,
                        profilePictureFile,
                    )
                },
                enabled = isFormValid,
                modifier = Modifier.weight(1f),
            )
        }

        VerticalSpacer(SpacingSize.Large)

        // Extra bottom padding for safe scrolling area
        VerticalSpacer(SpacingSize.Medium)
    }

    // Image source selection dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Image Source") },
            text = { Text("Choose how you'd like to add your profile picture") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        cameraLauncher.launch(null)
                    },
                ) {
                    Text("Camera")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch("image/*")
                    },
                ) {
                    Text("Gallery")
                }
            },
        )
    }
} 
