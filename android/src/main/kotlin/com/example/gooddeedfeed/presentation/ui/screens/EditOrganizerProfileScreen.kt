package com.example.gooddeedfeed.presentation.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.R
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.model.SocialMediaLink
import com.example.gooddeedfeed.domain.model.SocialMediaPlatform
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import com.example.gooddeedfeed.presentation.ui.components.ProfileImagePicker
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.onboarding.ProfileSectionHeader
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrganizerProfileScreen(
    user: DomainUser,
    onSave: (DomainUserUpdate, File?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var fullName by remember(user) { mutableStateOf(user.fullName ?: "") }
    var phone by remember(user) { mutableStateOf(user.phone ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var profilePictureFile by remember { mutableStateOf<File?>(null) }
    var organizationName by remember(user) { mutableStateOf(user.organizationName ?: "") }
    var organizationDescription by remember(user) { mutableStateOf(user.organizationDescription ?: "") }
    var organizationWebsite by remember(user) { mutableStateOf(user.organizationWebsite ?: "") }

    val existingSocialMedia = user.organizationSocialMedia ?: emptyList()
    var instagramHandle by remember(user) {
        mutableStateOf(existingSocialMedia.find { it.platform == SocialMediaPlatform.INSTAGRAM }?.url ?: "")
    }
    var twitterHandle by remember(user) {
        mutableStateOf(existingSocialMedia.find { it.platform == SocialMediaPlatform.TWITTER }?.url ?: "")
    }
    var facebookPage by remember(user) {
        mutableStateOf(existingSocialMedia.find { it.platform == SocialMediaPlatform.FACEBOOK }?.url ?: "")
    }

    var organizationImageFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var organizationImageUrls by remember(user) { mutableStateOf(user.organizationImages ?: emptyList()) }
    var mainOrgImageIndex by remember { mutableStateOf(0) }

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

    val fullNameError: String? by remember(fullName) {
        derivedStateOf { ImageUtils.FormValidation.validateFullName(fullName) }
    }

    val phoneError: String? by remember(phone) {
        derivedStateOf { ImageUtils.FormValidation.validatePhone(phone) }
    }

    val organizationError: String? by remember(organizationName) {
        derivedStateOf {
            if (organizationName.isBlank()) "Organization name is required" else null
        }
    }

    val isFormValid = fullNameError == null && phoneError == null && organizationError == null

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

            ProfileSectionHeader(
                title = "Basic Information",
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Medium)

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                isError = fullNameError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            fullNameError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start),
                )
            }

            VerticalSpacer(SpacingSize.Medium)

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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            phoneError?.let { error ->
                Text(
                    text = error,
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

            VerticalSpacer(SpacingSize.Large)

            ProfileSectionHeader(
                title = "Organization Information",
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Medium)

            OutlinedTextField(
                value = organizationName,
                onValueChange = { organizationName = it },
                label = { Text("Organization Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = organizationError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                ),
            )

            organizationError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start),
                )
            }

            VerticalSpacer(SpacingSize.Medium)

            OutlinedTextField(
                value = organizationDescription,
                onValueChange = { organizationDescription = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Tell us about your organization's mission and goals...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )

            VerticalSpacer(SpacingSize.Medium)

            OutlinedTextField(
                value = organizationWebsite,
                onValueChange = { organizationWebsite = it },
                label = { Text("Website (Optional)") },
                placeholder = { Text("https://yourorganization.com") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )

            VerticalSpacer(SpacingSize.Large)

            ProfileSectionHeader(
                title = "Social Media",
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Medium)

            OutlinedTextField(
                value = instagramHandle,
                onValueChange = { instagramHandle = it },
                label = { Text("Instagram Handle") },
                placeholder = { Text("@username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = getSocialMediaIcon(SocialMediaPlatform.INSTAGRAM),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )

            VerticalSpacer(SpacingSize.Medium)

            OutlinedTextField(
                value = twitterHandle,
                onValueChange = { twitterHandle = it },
                label = { Text("Twitter Handle") },
                placeholder = { Text("@username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = getSocialMediaIcon(SocialMediaPlatform.TWITTER),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )

            VerticalSpacer(SpacingSize.Medium)

            OutlinedTextField(
                value = facebookPage,
                onValueChange = { facebookPage = it },
                label = { Text("Facebook Page") },
                placeholder = { Text("Page name or URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = getSocialMediaIcon(SocialMediaPlatform.FACEBOOK),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )

            VerticalSpacer(SpacingSize.Large)

            ProfileSectionHeader(
                title = "Organization Images",
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Medium)

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
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.ExtraLarge)

            PrimaryButton(
                text = "Save Changes",
                onClick = {
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

                    val userUpdate = DomainUserUpdate(
                        fullName = if (fullName != user.fullName) fullName.takeIf { it.isNotBlank() } else null,
                        phone = if (phone != user.phone) phone.takeIf { it.isNotBlank() } else null,
                        organizationName = if (organizationName != user.organizationName) organizationName.takeIf { it.isNotBlank() } else null,
                        organizationDescription = if (organizationDescription != user.organizationDescription) organizationDescription.takeIf { it.isNotBlank() } else null,
                        organizationWebsite = if (organizationWebsite != user.organizationWebsite) organizationWebsite.takeIf { it.isNotBlank() } else null,
                        organizationSocialMedia = if (socialMedia != user.organizationSocialMedia) socialMedia else null,
                        organizationImages = if (organizationImageFiles.isNotEmpty()) organizationImageFiles.map { it.absolutePath } else null,
                    )
                    Log.d("EditOrganizerProfileScreen", "Save clicked: update=$userUpdate, file=$profilePictureFile")

                    onSave(userUpdate, profilePictureFile)
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpacer(SpacingSize.Large)
        }
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        if (selectedImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAddImage() },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Image",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Add Organization Images",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Showcase your organization (up to 10 images)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(8.dp),
                ) {
                    AsyncImage(
                        model = selectedImages[mainImageIndex],
                        contentDescription = "Main Organization Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )

                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "MAIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    IconButton(
                        onClick = { onRemoveImage(mainImageIndex) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                CircleShape,
                            )
                            .size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                            RoundedCornerShape(8.dp),
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            AsyncImage(
                                model = selectedImages[index],
                                contentDescription = "Organization Image ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }

                    if (selectedImages.size < 10) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAddImage() }
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add More Images",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
