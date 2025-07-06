package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gooddeedfeed.domain.model.DomainSex
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.toDisplayString
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import com.example.gooddeedfeed.presentation.ui.components.ProfileImagePicker
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthViewModel
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.gooddeedfeed.presentation.ui.components.onboarding.ProfileSectionHeader
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    user: DomainUser,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(),
) {
    // Editable fields
    var fullName by remember { mutableStateOf(TextFieldValue(user.fullName ?: "")) }
    var phone by remember { mutableStateOf(TextFieldValue(user.phone ?: "")) }
    var organizationName by remember { mutableStateOf(TextFieldValue(user.organizationName ?: "")) }

    // Volunteer-specific fields
    var sex by remember { mutableStateOf(user.sex ?: DomainSex.PREFER_NOT_TO_SAY) }
    var sexDropdownExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(TextFieldValue(user.description ?: "")) }
    var skills by remember { mutableStateOf(TextFieldValue(user.skills?.joinToString(", ") ?: "")) }
    var age by remember { mutableStateOf(TextFieldValue(user.age?.toString() ?: "")) }
    var emergencyContactName by remember { mutableStateOf(TextFieldValue(user.emergencyContactName ?: "")) }
    var emergencyContactPhone by remember { mutableStateOf(TextFieldValue(user.emergencyContactPhone ?: "")) }
    var locationArea by remember { mutableStateOf(TextFieldValue(user.locationArea ?: "")) }
    var hasDriversLicense by remember { mutableStateOf(user.hasDriversLicense ?: false) }
    var disabilities by remember { mutableStateOf(TextFieldValue(user.disabilities ?: "")) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
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
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ProfileImagePicker(
                            currentImageUrl = user.profilePictureUrl,
                            onImageSelected = { file -> viewModel.uploadProfilePicture(file) },
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileSectionHeader("Personal Information")
                        VerticalSpacer(SpacingSize.Medium)

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (user.userType?.name == "ORGANIZER") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = organizationName,
                                onValueChange = { organizationName = it },
                                label = { Text("Organization Name") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (user.userType?.name == "VOLUNTEER") {
                            Spacer(modifier = Modifier.height(8.dp))

                            ExposedDropdownMenuBox(
                                expanded = sexDropdownExpanded,
                                onExpandedChange = { sexDropdownExpanded = !sexDropdownExpanded },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedTextField(
                                    value = sex.toDisplayString(),
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

                                DropdownMenu(
                                    expanded = sexDropdownExpanded,
                                    onDismissRequest = { sexDropdownExpanded = false },
                                ) {
                                    DomainSex.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.toDisplayString()) },
                                            onClick = {
                                                sex = option
                                                sexDropdownExpanded = false
                                            },
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = skills,
                                onValueChange = { skills = it },
                                label = { Text("Skills (comma separated)") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it },
                                label = { Text("Age") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = emergencyContactName,
                                onValueChange = { emergencyContactName = it },
                                label = { Text("Emergency Contact Name") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = emergencyContactPhone,
                                onValueChange = { emergencyContactPhone = it },
                                label = { Text("Emergency Contact Phone") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = locationArea,
                                onValueChange = { locationArea = it },
                                label = { Text("Location Area") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Checkbox(
                                    checked = hasDriversLicense,
                                    onCheckedChange = { hasDriversLicense = it },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Has Driver's License")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = disabilities,
                                onValueChange = { disabilities = it },
                                label = { Text("Disabilities (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            // Save changes
                            val update = ImageUtils.buildProfileUpdate(
                                user = user,
                                fullName = fullName,
                                phone = phone,
                                organizationName = organizationName,
                                sex = sex,
                                description = description,
                                skills = skills,
                                age = age,
                                emergencyContactName = emergencyContactName,
                                emergencyContactPhone = emergencyContactPhone,
                                locationArea = locationArea,
                                hasDriversLicense = hasDriversLicense,
                                disabilities = disabilities,
                            )
                            viewModel.updateUserProfile(update)
                            onSave()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
} 
