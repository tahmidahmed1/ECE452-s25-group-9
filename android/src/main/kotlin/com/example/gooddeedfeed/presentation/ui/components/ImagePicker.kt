package com.example.gooddeedfeed.presentation.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.graphics.ImageDecoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.text.input.TextFieldValue
import com.example.gooddeedfeed.domain.model.DomainUserUpdate
import com.example.gooddeedfeed.domain.model.DomainSex
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.domain.model.DomainInstitutionName

object ImageUtils {
    fun saveBitmapToFile(context: Context, bitmap: Bitmap): File? {
        return try {
            val file = File(context.cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveUriToFile(context: Context, uri: Uri): File? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = false
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            saveBitmapToFile(context, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatPhoneNumber(digitsOnly: String): String {
        val trimmed = digitsOnly.take(10) // limit to 10 digits for NA format
        val sb = StringBuilder()
        for (i in trimmed.indices) {
            sb.append(trimmed[i])
            if ((i == 2 || i == 5) && i != trimmed.lastIndex) sb.append('-')
        }
        return sb.toString()
    }

    /**
     * Builds a DomainUserUpdate from form fields, avoiding duplicate logic
     */
    fun buildProfileUpdate(
        user: DomainUser,
        fullName: TextFieldValue? = null,
        phone: TextFieldValue? = null,
        organizationName: TextFieldValue? = null,
        sex: DomainSex? = null,
        description: TextFieldValue? = null,
        skills: TextFieldValue? = null,
        age: TextFieldValue? = null,
        emergencyContactName: TextFieldValue? = null,
        emergencyContactPhone: TextFieldValue? = null,
        locationArea: TextFieldValue? = null,
        hasDriversLicense: Boolean? = null,
        disabilities: TextFieldValue? = null,
    ): DomainUserUpdate {
        return DomainUserUpdate(
            fullName = fullName?.text?.takeIf { it.isNotBlank() },
            phone = phone?.text?.takeIf { it.isNotBlank() },
            organizationName = organizationName?.text?.takeIf { it.isNotBlank() },
            sex = if (user.userType?.name == "VOLUNTEER") sex else null,
            description = description?.text?.takeIf { it.isNotBlank() },
            skills = skills?.text?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
            age = age?.text?.toIntOrNull(),
            emergencyContactName = emergencyContactName?.text?.takeIf { it.isNotBlank() },
            emergencyContactPhone = emergencyContactPhone?.text?.takeIf { it.isNotBlank() },
            locationArea = locationArea?.text?.takeIf { it.isNotBlank() },
            hasDriversLicense = hasDriversLicense,
            disabilities = disabilities?.text?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Common form validation logic
     */
    object FormValidation {
        fun validateFullName(fullName: String): String? {
            return if (fullName.isBlank()) "Full name is required" else null
        }

        fun validatePhone(phone: String): String? {
            return if (phone.isBlank()) "Phone number is required" else null
        }

        fun validateOrganization(organizationName: String, userType: DomainUserType?): String? {
            return if (userType == DomainUserType.ORGANIZER && organizationName.isBlank()) {
                "Organization name is required"
            } else null
        }

        fun validateInstitution(selectedInstitution: DomainInstitutionName?, userType: DomainUserType?): String? {
            return if (userType == DomainUserType.INSTITUTION && selectedInstitution == null) {
                "Institution is required"
            } else null
        }

        fun validateAge(age: String): String? {
            return if (age.isBlank()) {
                "Age is required"
            } else {
                val ageInt = age.toIntOrNull()
                if (ageInt == null || ageInt < 13 || ageInt > 120) {
                    "Please enter a valid age (13-120)"
                } else null
            }
        }

        fun validateDescription(description: String): String? {
            return if (description.isBlank()) "Description is required" else null
        }

        fun validateEmergencyContact(name: String, phone: String): Map<String, String?> {
            return mapOf(
                "emergencyName" to if (name.isBlank()) "Emergency contact name is required" else null,
                "emergencyPhone" to if (phone.isBlank()) "Emergency contact phone is required" else null
            )
        }

        fun validateLocation(location: String): String? {
            return if (location.isBlank()) "Location is required" else null
        }
    }
}

@Composable
fun ProfileImagePicker(
    currentImageUrl: String? = null,
    onImageSelected: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val file = ImageUtils.saveBitmapToFile(context, it)
            if (file != null) {
                onImageSelected(file)
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
                onImageSelected(file)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        // Profile picture display
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
            when {
                selectedImageUri != null -> {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                !currentImageUrl.isNullOrEmpty() -> {
                    AsyncImage(
                        model = currentImageUrl,
                        contentDescription = "Current profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default profile picture",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add Profile Picture",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "Tap to select from camera or gallery",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
