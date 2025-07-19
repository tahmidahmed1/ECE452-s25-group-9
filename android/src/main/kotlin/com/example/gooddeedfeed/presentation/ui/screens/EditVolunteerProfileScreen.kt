package com.example.gooddeedfeed.presentation.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.DomainSex
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import com.example.gooddeedfeed.presentation.ui.components.ProfileImagePicker
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.onboarding.ProfileSectionHeader
import com.example.gooddeedfeed.presentation.ui.components.onboarding.SkillChip
import com.example.gooddeedfeed.presentation.ui.theme.AppConstants
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVolunteerProfileScreen(
    user: DomainUser,
    onSave: (DomainUserUpdate, File?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Initialize form fields with current user data
    var fullName by remember(user) { mutableStateOf(user.fullName ?: "") }
    var phone by remember(user) { mutableStateOf(user.phone ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var profilePictureFile by remember { mutableStateOf<File?>(null) }
    var selectedSex by remember(user) { mutableStateOf(user.sex) }
    var description by remember(user) { mutableStateOf(user.description ?: "") }
    var customSkill by remember { mutableStateOf("") }
    var selectedSkills by remember(user) { mutableStateOf(user.skills?.toSet() ?: setOf()) }
    var age by remember(user) { mutableStateOf(user.age?.toString() ?: "") }
    var emergencyContactName by remember(user) { mutableStateOf(user.emergencyContactName ?: "") }
    var emergencyContactPhone by remember(user) { mutableStateOf(user.emergencyContactPhone ?: "") }
    var locationArea by remember(user) { mutableStateOf(user.locationArea ?: "") }
    var hasDriversLicense by remember(user) { mutableStateOf(user.hasDriversLicense ?: false) }
    var disabilities by remember(user) { mutableStateOf(user.disabilities ?: "") }
    var sexDropdownExpanded by remember { mutableStateOf(false) }

    val skillsScrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val predefinedSkills = AppConstants.PREDEFINED_SKILLS

    val displayedSkills by remember(selectedSkills) {
        derivedStateOf {
            val customSkills = selectedSkills.filter { it !in predefinedSkills }
            customSkills + predefinedSkills
        }
    }

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

    // Form validation
    val validationErrors by remember(selectedSex, description, age, emergencyContactName, emergencyContactPhone, locationArea, fullName, phone) {
        derivedStateOf {
            val emergencyValidation = ImageUtils.FormValidation.validateEmergencyContact(emergencyContactName, emergencyContactPhone)
            mapOf(
                "sex" to if (selectedSex == null) "Please select your gender" else null,
                "description" to ImageUtils.FormValidation.validateDescription(description),
                "age" to ImageUtils.FormValidation.validateAge(age),
                "emergencyContactName" to emergencyValidation["emergencyName"],
                "emergencyContactPhone" to emergencyValidation["emergencyPhone"],
                "locationArea" to ImageUtils.FormValidation.validateLocation(locationArea),
                "fullName" to ImageUtils.FormValidation.validateFullName(fullName),
                "phone" to ImageUtils.FormValidation.validatePhone(phone),
            )
        }
    }

    val isFormValid by remember(validationErrors) {
        derivedStateOf { validationErrors.values.all { it == null } }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header with back button and title (matching Profile Preview style)
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
                    text = "Edit Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            VerticalSpacer(SpacingSize.Large)

            // Profile Picture Section
            ProfileImagePicker(
                currentImageUrl = selectedImageUri?.toString() ?: user.profilePictureUrl,
                onImageSelected = { file ->
                    profilePictureFile = file
                    selectedImageUri = Uri.fromFile(file)
                },
                onImageRemoved = {
                    profilePictureFile = null
                    selectedImageUri = null
                },
            )

            VerticalSpacer(SpacingSize.Large)

            // Basic Information Section
            ProfileSectionHeader(
                title = "Basic Information",
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Medium)

            // Full Name Field
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                isError = validationErrors["fullName"] != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            validationErrors["fullName"]?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VerticalSpacer(SpacingSize.Medium)

            // Phone Field
            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    phone = ImageUtils.formatPhoneNumber(digits)
                },
                label = { Text("Phone Number") },
                isError = validationErrors["phone"] != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            validationErrors["phone"]?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
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

            // Sex Field
            ExposedDropdownMenuBox(
                expanded = sexDropdownExpanded,
                onExpandedChange = { sexDropdownExpanded = !sexDropdownExpanded },
            ) {
                OutlinedTextField(
                    value = when (selectedSex) {
                        DomainSex.MALE -> "Male"
                        DomainSex.FEMALE -> "Female"
                        DomainSex.NON_BINARY -> "Non-binary"
                        DomainSex.PREFER_NOT_TO_SAY -> "Prefer not to say"
                        null -> ""
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = validationErrors["sex"] != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                    ),
                )

                ExposedDropdownMenu(
                    expanded = sexDropdownExpanded,
                    onDismissRequest = { sexDropdownExpanded = false },
                ) {
                    DomainSex.entries.forEach { sex ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (sex) {
                                        DomainSex.MALE -> "Male"
                                        DomainSex.FEMALE -> "Female"
                                        DomainSex.NON_BINARY -> "Non-binary"
                                        DomainSex.PREFER_NOT_TO_SAY -> "Prefer not to say"
                                    },
                                )
                            },
                            onClick = {
                                selectedSex = sex
                                sexDropdownExpanded = false
                            },
                        )
                    }
                }
            }

            validationErrors["sex"]?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VerticalSpacer(SpacingSize.Medium)

            // Age Field
            OutlinedTextField(
                value = age,
                onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 3) age = it },
                label = { Text("Age") },
                isError = validationErrors["age"] != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            validationErrors["age"]?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VerticalSpacer(SpacingSize.Medium)

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("About You") },
                placeholder = { Text("Tell us about yourself, your interests, and what motivates you to volunteer...") },
                isError = validationErrors["description"] != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            validationErrors["description"]?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VerticalSpacer(SpacingSize.Large)

            // Skills Section
            ProfileSectionHeader(
                title = "Skills & Interests",
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Medium)

            // Custom skill input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = customSkill,
                    onValueChange = { customSkill = it },
                    label = { Text("Add Custom Skill") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (customSkill.isNotBlank() && customSkill !in selectedSkills) {
                                selectedSkills = selectedSkills + customSkill
                                customSkill = ""
                                coroutineScope.launch {
                                    skillsScrollState.animateScrollToItem(0)
                                }
                            }
                        },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        if (customSkill.isNotBlank() && customSkill !in selectedSkills) {
                            selectedSkills = selectedSkills + customSkill
                            customSkill = ""
                            coroutineScope.launch {
                                skillsScrollState.animateScrollToItem(0)
                            }
                        }
                    },
                    enabled = customSkill.isNotBlank() && customSkill !in selectedSkills,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Skill",
                        tint = if (customSkill.isNotBlank() && customSkill !in selectedSkills) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            VerticalSpacer(SpacingSize.Medium)

            // Skills chips
            LazyRow(
                state = skillsScrollState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(displayedSkills) { skill ->
                    SkillChip(
                        text = skill,
                        isSelected = skill in selectedSkills,
                        onToggle = {
                            selectedSkills = if (skill in selectedSkills) {
                                selectedSkills - skill
                            } else {
                                selectedSkills + skill
                            }
                        },
                    )
                }
            }

            VerticalSpacer(SpacingSize.Large)

            // Emergency Contact Section
            ProfileSectionHeader(
                title = "Emergency Contact",
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Medium)

            // Emergency Contact Name
            OutlinedTextField(
                value = emergencyContactName,
                onValueChange = { emergencyContactName = it },
                label = { Text("Emergency Contact Name") },
                isError = validationErrors["emergencyContactName"] != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            validationErrors["emergencyContactName"]?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VerticalSpacer(SpacingSize.Medium)

            // Emergency Contact Phone
            OutlinedTextField(
                value = emergencyContactPhone,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    emergencyContactPhone = ImageUtils.formatPhoneNumber(digits)
                },
                label = { Text("Emergency Contact Phone") },
                isError = validationErrors["emergencyContactPhone"] != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            validationErrors["emergencyContactPhone"]?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VerticalSpacer(SpacingSize.Large)

            // Location & Accessibility Section
            ProfileSectionHeader(
                title = "Location & Accessibility",
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Medium)

            // Location Area
            OutlinedTextField(
                value = locationArea,
                onValueChange = { locationArea = it },
                label = { Text("Preferred Location/Area") },
                placeholder = { Text("e.g., Downtown, North Side, etc.") },
                isError = validationErrors["locationArea"] != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            validationErrors["locationArea"]?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VerticalSpacer(SpacingSize.Medium)

            // Driver's License Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = hasDriversLicense,
                    onCheckedChange = { hasDriversLicense = it },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I have a valid driver's license",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            VerticalSpacer(SpacingSize.Medium)

            // Accessibility Needs
            OutlinedTextField(
                value = disabilities,
                onValueChange = { disabilities = it },
                label = { Text("Accessibility Needs (Optional)") },
                placeholder = { Text("Any accommodations or accessibility requirements...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )

            VerticalSpacer(SpacingSize.ExtraLarge)

            // Save Button
            PrimaryButton(
                text = "Save Changes",
                onClick = {
                    val userUpdate = DomainUserUpdate(
                        fullName = if (fullName != user.fullName) fullName.takeIf { it.isNotBlank() } else null,
                        phone = if (phone != user.phone) phone.takeIf { it.isNotBlank() } else null,
                        sex = if (selectedSex != user.sex) selectedSex else null,
                        description = if (description != user.description) description.takeIf { it.isNotBlank() } else null,
                        skills = if (selectedSkills.toList() != user.skills) selectedSkills.toList().takeIf { it.isNotEmpty() } else null,
                        age = if (age.toIntOrNull() != user.age) age.toIntOrNull() else null,
                        emergencyContactName = if (emergencyContactName != user.emergencyContactName) emergencyContactName.takeIf { it.isNotBlank() } else null,
                        emergencyContactPhone = if (emergencyContactPhone != user.emergencyContactPhone) emergencyContactPhone.takeIf { it.isNotBlank() } else null,
                        locationArea = if (locationArea != user.locationArea) locationArea.takeIf { it.isNotBlank() } else null,
                        hasDriversLicense = if (hasDriversLicense != user.hasDriversLicense) hasDriversLicense else null,
                        disabilities = if (disabilities != user.disabilities) disabilities.takeIf { it.isNotBlank() } else null,
                    )
                    Log.d("EditVolunteerProfileScreen", "Save clicked: update=$userUpdate, file=$profilePictureFile")

                    onSave(userUpdate, profilePictureFile)
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Large)
        }
    }
}
