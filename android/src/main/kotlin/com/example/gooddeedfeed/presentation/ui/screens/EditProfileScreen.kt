package com.example.gooddeedfeed.presentation.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.gooddeedfeed.domain.model.DomainSex
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.SocialMediaLink
import com.example.gooddeedfeed.domain.model.SocialMediaPlatform
import com.example.gooddeedfeed.domain.model.toDisplayString
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import com.example.gooddeedfeed.presentation.ui.components.ProfileImagePicker
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.onboarding.ProfileSectionHeader
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    user: DomainUser,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(),
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf(TextFieldValue(user.fullName ?: "")) }
    var phone by remember { mutableStateOf(TextFieldValue(user.phone ?: "")) }

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

    var organizationName by remember { mutableStateOf(TextFieldValue(user.organizationName ?: "")) }
    var organizationDescription by remember { mutableStateOf(TextFieldValue(user.organizationDescription ?: "")) }
    var organizationWebsite by remember { mutableStateOf(TextFieldValue(user.organizationWebsite ?: "")) }

    var instagramHandle by remember { mutableStateOf(user.organizationSocialMedia?.find { it.platform.name == "INSTAGRAM" }?.url ?: "") }
    var twitterHandle by remember { mutableStateOf(user.organizationSocialMedia?.find { it.platform.name == "TWITTER" }?.url ?: "") }
    var facebookPage by remember { mutableStateOf(user.organizationSocialMedia?.find { it.platform.name == "FACEBOOK" }?.url ?: "") }

    var organizationImageFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var organizationImageUrls by remember { mutableStateOf(user.organizationImages ?: emptyList()) }

    var bannerImageFile by remember { mutableStateOf<File?>(null) }
    var bannerImageUri by remember { mutableStateOf<Uri?>(null) }

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

    val bannerImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            bannerImageUri = it
            bannerImageFile = ImageUtils.saveUriToFile(context, it)
        }
    }

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
                            onImageRemoved = { viewModel.removeProfilePicture() },
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
                            VerticalSpacer(SpacingSize.Medium)
                            ProfileSectionHeader("Organization Information")
                            VerticalSpacer(SpacingSize.Medium)

                            OutlinedTextField(
                                value = organizationName,
                                onValueChange = { organizationName = it },
                                label = { Text("Organization Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            )

                            VerticalSpacer(SpacingSize.Small)

                            OutlinedTextField(
                                value = organizationDescription,
                                onValueChange = { organizationDescription = it },
                                label = { Text("Organization Description") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp),
                            )

                            VerticalSpacer(SpacingSize.Small)

                            OutlinedTextField(
                                value = organizationWebsite,
                                onValueChange = { organizationWebsite = it },
                                label = { Text("Website (Optional)") },
                                placeholder = { Text("https://example.com") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                shape = RoundedCornerShape(12.dp),
                            )

                            VerticalSpacer(SpacingSize.Medium)

                            ProfileSectionHeader("Social Media (Optional)")
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

                            ProfileSectionHeader("Banner Image (Optional)")
                            VerticalSpacer(SpacingSize.Small)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clickable { bannerImageLauncher.launch("image/*") },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when {
                                        bannerImageUri != null -> {
                                            Image(
                                                painter = rememberAsyncImagePainter(bannerImageUri),
                                                contentDescription = "Banner Image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                        !user.bannerUrl.isNullOrEmpty() -> {
                                            Image(
                                                painter = rememberAsyncImagePainter(user.bannerUrl),
                                                contentDescription = "Current Banner",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                        else -> {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Banner",
                                                    modifier = Modifier.size(32.dp),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                                Text(
                                                    text = "Add Banner Image",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            VerticalSpacer(SpacingSize.Medium)

                            ProfileSectionHeader("Organization Images (Optional)")
                            VerticalSpacer(SpacingSize.Small)

                            SimpleImageCarousel(
                                imageFiles = organizationImageFiles,
                                imageUrls = organizationImageUrls,
                                onAddImage = {
                                    if (organizationImageFiles.size + organizationImageUrls.size < 10) {
                                        orgImageLauncher.launch("image/*")
                                    }
                                },
                                onRemoveNewImage = { index ->
                                    organizationImageFiles = organizationImageFiles.filterIndexed { i, _ -> i != index }
                                },
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
                            val socialMedia = if (user.userType?.name == "ORGANIZER") {
                                mutableListOf<SocialMediaLink>().apply {
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
                            } else {
                                null
                            }

                            val orgImages = if (user.userType?.name == "ORGANIZER") {
                                (organizationImageUrls + organizationImageFiles.map { it.absolutePath }).takeIf { it.isNotEmpty() }
                            } else {
                                null
                            }

                            val update = ImageUtils.buildProfileUpdate(
                                user = user,
                                fullName = fullName,
                                phone = phone,
                                organizationName = if (user.userType?.name == "ORGANIZER") organizationName else null,
                                sex = sex,
                                description = description,
                                skills = skills,
                                age = age,
                                emergencyContactName = emergencyContactName,
                                emergencyContactPhone = emergencyContactPhone,
                                locationArea = locationArea,
                                hasDriversLicense = hasDriversLicense,
                                disabilities = disabilities,
                                organizationDescription = if (user.userType?.name == "ORGANIZER") organizationDescription else null,
                                organizationWebsite = if (user.userType?.name == "ORGANIZER") organizationWebsite else null,
                                organizationSocialMedia = socialMedia,
                                organizationImages = orgImages,
                            )

                            bannerImageFile?.let {
                            }

                            if (organizationImageFiles.isNotEmpty()) {
                            }

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

@Composable
private fun SimpleImageCarousel(
    imageFiles: List<File>,
    imageUrls: List<String>,
    onAddImage: () -> Unit,
    onRemoveNewImage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (imageFiles.isEmpty() && imageUrls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAddImage() },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Images",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Add Organization Images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "${imageFiles.size + imageUrls.size} image(s) selected",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (imageFiles.size + imageUrls.size < 10) {
                    IconButton(
                        onClick = onAddImage,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add More Images",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
} 
