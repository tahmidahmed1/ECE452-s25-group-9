package com.example.gooddeedfeed.presentation.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
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
            val file = saveBitmapToFile(context, it)
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
            val file = saveUriToFile(context, it)
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

private fun saveBitmapToFile(context: Context, bitmap: Bitmap): File? {
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

private fun saveUriToFile(context: Context, uri: Uri): File? {
    return try {
        @Suppress("DEPRECATION")
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        saveBitmapToFile(context, bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
} 
