package com.example.gooddeedfeed.presentation.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.DomainSex
import com.example.gooddeedfeed.domain.model.DomainVolunteerProfile
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.onboarding.ProfileSectionHeader
import com.example.gooddeedfeed.presentation.ui.components.onboarding.SkillChip
import com.example.gooddeedfeed.presentation.ui.theme.AppConstants
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

    val predefinedSkills = AppConstants.PREDEFINED_SKILLS

    val displayedSkills by remember(selectedSkills) {
        derivedStateOf {
            buildList {
                addAll(selectedSkills)
                addAll(predefinedSkills.filter { it !in selectedSkills })
            }
        }
    }

    val validationErrors by remember(selectedSex, description, age, emergencyContactName, emergencyContactPhone, locationArea) {
        derivedStateOf {
            val emergencyValidation = ImageUtils.FormValidation.validateEmergencyContact(emergencyContactName, emergencyContactPhone)
            mapOf(
                "sex" to if (selectedSex == null) "Sex is required" else null,
                "description" to ImageUtils.FormValidation.validateDescription(description),
                "age" to ImageUtils.FormValidation.validateAge(age),
                "emergencyName" to emergencyValidation["emergencyName"],
                "emergencyPhone" to emergencyValidation["emergencyPhone"],
                "location" to ImageUtils.FormValidation.validateLocation(locationArea),
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
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                    )
                }
            }

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

                ProfileSectionHeader("Personal Information")
                VerticalSpacer(SpacingSize.Medium)

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

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Tell us about yourself") },
                    placeholder = { Text("Share your interests, background, or motivation for volunteering...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    isError = validationErrors["description"] != null,
                    supportingText = validationErrors["description"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                )

                VerticalSpacer(SpacingSize.Medium)

                OutlinedTextField(
                    value = age,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                            age = newValue
                        }
                    },
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = validationErrors["age"] != null,
                    supportingText = validationErrors["age"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                )

                VerticalSpacer(SpacingSize.Large)
                ProfileSectionHeader("Skills & Abilities")
                VerticalSpacer(SpacingSize.Medium)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(
                        items = displayedSkills,
                        key = { skill -> skill }
                    ) { skill ->
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

                VerticalSpacer(SpacingSize.Medium)

                OutlinedTextField(
                    value = customSkill,
                    onValueChange = { customSkill = it },
                    label = { Text("Add custom skill") },
                    placeholder = { Text("Enter a skill not listed above") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (customSkill.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    selectedSkills = selectedSkills + customSkill.trim()
                                    customSkill = ""
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add skill",
                                )
                            }
                        }
                    },
                )

                VerticalSpacer(SpacingSize.Large)
                ProfileSectionHeader("Emergency Contact")
                VerticalSpacer(SpacingSize.Medium)

                OutlinedTextField(
                    value = emergencyContactName,
                    onValueChange = { emergencyContactName = it },
                    label = { Text("Emergency Contact Name") },
                    placeholder = { Text("Full name of emergency contact") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = validationErrors["emergencyName"] != null,
                    supportingText = validationErrors["emergencyName"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                )

                VerticalSpacer(SpacingSize.Medium)

                OutlinedTextField(
                    value = emergencyContactPhone,
                    onValueChange = { emergencyContactPhone = it },
                    label = { Text("Emergency Contact Phone") },
                    placeholder = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = validationErrors["emergencyPhone"] != null,
                    supportingText = validationErrors["emergencyPhone"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                )

                VerticalSpacer(SpacingSize.Large)
                ProfileSectionHeader("Location & Accessibility")
                VerticalSpacer(SpacingSize.Medium)

                OutlinedTextField(
                    value = locationArea,
                    onValueChange = { locationArea = it },
                    label = { Text("Preferred Location/Area") },
                    placeholder = { Text("City, neighborhood, or area where you'd like to volunteer") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = validationErrors["location"] != null,
                    supportingText = validationErrors["location"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                )

                VerticalSpacer(SpacingSize.Medium)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = hasDriversLicense,
                            onCheckedChange = { hasDriversLicense = it },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "I have a valid driver's license",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "This helps us match you with opportunities that may require transportation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                VerticalSpacer(SpacingSize.Medium)

                OutlinedTextField(
                    value = disabilities,
                    onValueChange = { disabilities = it },
                    label = { Text("Accessibility needs (optional)") },
                    placeholder = { Text("Any accommodations or accessibility requirements") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                )

                VerticalSpacer(SpacingSize.ExtraLarge)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                PrimaryButton(
                    text = "Complete Profile",
                    onClick = {
                        val profile = DomainVolunteerProfile(
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
                        onComplete(profile, null)
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
} 
