package com.example.gooddeedfeed.presentation.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.DomainSex
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import com.example.gooddeedfeed.presentation.ui.components.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.onboarding.ProfileSectionHeader
import com.example.gooddeedfeed.presentation.ui.components.onboarding.SkillChip
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStepThreeVolunteerScreen(
    fullName: String,
    phone: String,
    onComplete: (DomainVolunteerProfile, File?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSex by remember { mutableStateOf<DomainSex?>(null) }
    var description by remember { mutableStateOf("") }
    var customSkill by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf(setOf<String>()) }
    var age by remember { mutableStateOf("") }
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }
    var locationArea by remember { mutableStateOf("") }
    var hasDriversLicense by remember { mutableStateOf(false) }
    var disabilities by remember { mutableStateOf("") }

    var sexDropdownExpanded by remember { mutableStateOf(false) }

    // Predefined skill options
    val predefinedSkills = listOf(
        "First Aid", "CPR", "Teaching", "Cooking", "Construction",
        "Gardening", "Event Planning", "Photography", "Translation",
        "Computer Skills", "Social Media", "Leadership", "Customer Service",
        "Animal Care", "Child Care", "Senior Care", "Art & Crafts",
        "Music", "Sports", "Driving",
    )

    val isFormValid by remember(selectedSex, description, age, emergencyContactName, emergencyContactPhone, locationArea) {
        derivedStateOf {
            val ageNumber = age.toIntOrNull() ?: -1
            selectedSex != null &&
                description.isNotBlank() &&
                ageNumber > 0 &&
                emergencyContactName.isNotBlank() &&
                emergencyContactPhone.isNotBlank() &&
                locationArea.isNotBlank()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Top bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                    )
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Your Volunteer Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                VerticalSpacer(SpacingSize.Small)

                Text(
                    text = "Help us match you with the perfect volunteer opportunities",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                VerticalSpacer(SpacingSize.Large)

                // Personal Information Section
                ProfileSectionHeader("Personal Information")
                VerticalSpacer(SpacingSize.Medium)

                // Sex Selection
                ExposedDropdownMenuBox(
                    expanded = sexDropdownExpanded,
                    onExpandedChange = { sexDropdownExpanded = !sexDropdownExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selectedSex?.let {
                            when (it) {
                                DomainSex.MALE -> "Male"
                                DomainSex.FEMALE -> "Female"
                                DomainSex.NON_BINARY -> "Non-binary"
                                DomainSex.PREFER_NOT_TO_SAY -> "Prefer not to say"
                            }
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sex") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    ExposedDropdownMenu(
                        expanded = sexDropdownExpanded,
                        onDismissRequest = { sexDropdownExpanded = false },
                    ) {
                        DomainSex.values().forEach { sex ->
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

                VerticalSpacer(SpacingSize.Medium)

                // Age Field
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                VerticalSpacer(SpacingSize.Medium)

                // Location Area
                OutlinedTextField(
                    value = locationArea,
                    onValueChange = { locationArea = it },
                    label = { Text("Location/Area") },
                    placeholder = { Text("e.g., Downtown Toronto, North York") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                VerticalSpacer(SpacingSize.Large)

                // About You Section
                ProfileSectionHeader("About You")
                VerticalSpacer(SpacingSize.Medium)

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Tell us about yourself") },
                    placeholder = { Text("Share your interests, motivation for volunteering, or any relevant experience...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                VerticalSpacer(SpacingSize.Large)

                // Skills Section
                ProfileSectionHeader("Skills & Abilities")
                VerticalSpacer(SpacingSize.Medium)

                Text(
                    text = "Select your skills or add custom ones:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                VerticalSpacer(SpacingSize.Small)

                // Skills Selection
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(predefinedSkills) { skill ->
                        SkillChip(
                            text = skill,
                            isSelected = selectedSkills.contains(skill),
                            onToggle = {
                                selectedSkills = if (selectedSkills.contains(skill)) {
                                    selectedSkills - skill
                                } else {
                                    selectedSkills + skill
                                }
                            },
                        )
                    }
                }

                VerticalSpacer(SpacingSize.Medium)

                // Custom skill input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = customSkill,
                        onValueChange = { customSkill = it },
                        label = { Text("Add custom skill") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )

                    Button(
                        onClick = {
                            if (customSkill.isNotBlank() && !selectedSkills.contains(customSkill)) {
                                selectedSkills = selectedSkills + customSkill
                                customSkill = ""
                            }
                        },
                        enabled = customSkill.isNotBlank(),
                    ) {
                        Text("Add")
                    }
                }

                VerticalSpacer(SpacingSize.Large)

                // Emergency Contact Section
                ProfileSectionHeader("Emergency Contact")
                VerticalSpacer(SpacingSize.Medium)

                OutlinedTextField(
                    value = emergencyContactName,
                    onValueChange = { emergencyContactName = it },
                    label = { Text("Emergency Contact Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                VerticalSpacer(SpacingSize.Medium)

                OutlinedTextField(
                    value = emergencyContactPhone,
                    onValueChange = { emergencyContactPhone = it },
                    label = { Text("Emergency Contact Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                VerticalSpacer(SpacingSize.Large)

                // Additional Information Section
                ProfileSectionHeader("Additional Information")
                VerticalSpacer(SpacingSize.Medium)

                // Driver's License Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = hasDriversLicense,
                        onCheckedChange = { hasDriversLicense = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Text(
                        text = "I have a valid G driver's license",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                VerticalSpacer(SpacingSize.Medium)

                // Disabilities/Accommodations
                OutlinedTextField(
                    value = disabilities,
                    onValueChange = { disabilities = it },
                    label = { Text("Disabilities or Accommodations Needed (Optional)") },
                    placeholder = { Text("Please describe any accommodations you may need...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                VerticalSpacer(SpacingSize.ExtraLarge)
            }

            // Fixed bottom button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp,
            ) {
                PrimaryButton(
                    text = "Complete Profile",
                    onClick = {
                        val volunteerProfile = DomainVolunteerProfile(
                            fullName = fullName,
                            phone = phone,
                            sex = selectedSex!!,
                            description = description,
                            skills = selectedSkills.toList(),
                            age = age.toInt(),
                            emergencyContactName = emergencyContactName,
                            emergencyContactPhone = emergencyContactPhone,
                            locationArea = locationArea,
                            hasDriversLicense = hasDriversLicense,
                            disabilities = disabilities.ifBlank { null },
                        )
                        onComplete(volunteerProfile, null)
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
        }
    }
} 
