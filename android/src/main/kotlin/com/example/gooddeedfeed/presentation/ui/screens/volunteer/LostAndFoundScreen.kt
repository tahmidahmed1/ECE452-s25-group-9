package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostAndFoundViewModel
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundItem
import com.example.gooddeedfeed.presentation.viewmodel.volunteer.LostFoundType
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen(
    user: DomainUser,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: LostAndFoundViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val createItemState by viewModel.createItemState.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<LostFoundItem?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }
    var showEditForm by remember { mutableStateOf<LostFoundItem?>(null) }
    var itemToDelete by remember { mutableStateOf<LostFoundItem?>(null) }
    var selectedFilter by remember { mutableStateOf<LostFoundType?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadItems()
    }

    LaunchedEffect(selectedFilter) {
        viewModel.loadItems(selectedFilter)
    }

    if (selectedItem != null) {
        LostFoundDetailScreen(
            item = selectedItem!!,
            onBack = { selectedItem = null },
        )
        return
    }

    if (showCreateForm) {
        CreateLostFoundScreen(
            onBack = { showCreateForm = false },
            onSubmit = { title, description, location, itemType, reward, tags, expiryDays, images ->
                viewModel.createItem(
                    title = title,
                    description = description,
                    location = location,
                    itemType = itemType,
                    reward = reward,
                    tags = tags,
                    expiryDays = expiryDays,
                    images = images
                )
            },
            onCreateSuccess = { showCreateForm = false },
            viewModel = viewModel,
        )
        return
    }

    if (showEditForm != null) {
        EditLostFoundScreen(
            item = showEditForm!!,
            onBack = { showEditForm = null },
            onSubmit = { title, description, location, reward, tags, isResolved ->
                viewModel.updateItem(
                    itemId = showEditForm!!.id,
                    title = title,
                    description = description,
                    location = location,
                    reward = reward,
                    tags = tags,
                    isResolved = isResolved
                )
            },
            onUpdateSuccess = { showEditForm = null },
            viewModel = viewModel,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Lost & Found",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lost & Found",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            FloatingActionButton(
                onClick = { showCreateForm = true },
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                    ),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Report Item")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All") },
            )
            FilterChip(
                selected = selectedFilter == LostFoundType.LOST,
                onClick = { selectedFilter = LostFoundType.LOST },
                label = { Text("Lost Items") },
            )
            FilterChip(
                selected = selectedFilter == LostFoundType.FOUND,
                onClick = { selectedFilter = LostFoundType.FOUND },
                label = { Text("Found Items") },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val currentState = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = { viewModel.loadItems(selectedFilter) },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Retry")
                    }
                }
            }
            is UiState.Success -> {
                val items = currentState.data
                if (items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "No items",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No lost or found items yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Be the first to report a lost or found item!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(items) { item ->
                            LostFoundItemCard(
                                item = item,
                                currentUserId = user.id.toString(),
                                onClick = { selectedItem = item },
                                onEdit = { showEditForm = item },
                                onDelete = { itemToDelete = item },
                            )
                        }
                    }
                }
            }
            else -> {
            }
        }
    }

    // Delete confirmation dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item?") },
            text = { Text("Are you sure you want to delete '${itemToDelete!!.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem(itemToDelete!!.id)
                        itemToDelete = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { itemToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun LostFoundItemCard(
    item: LostFoundItem,
    currentUserId: String,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val isOwner = item.userId == currentUserId
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )

                Surface(
                    color = if (item.type == LostFoundType.LOST) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = item.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.type == LostFoundType.LOST) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (item.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(item.images.take(3)) { imageUrl ->
                        try {
                            android.util.Log.d("LostFoundCard", "Loading image for item ${item.id}: $imageUrl")
                        } catch (_: Throwable) {}
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Item image",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            // Edit and delete buttons for owner
            if (isOwner) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun LostFoundDetailScreen(
    item: LostFoundItem,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = if (item.type == LostFoundType.LOST) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "${item.type.name} ITEM",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.type == LostFoundType.LOST) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Description",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.location,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item.reward?.let { reward ->
            if (reward.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Reward",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reward,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (item.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(item.tags) { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        item.daysRemaining?.let { days ->
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = if (days <= 3) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = if (days > 0) "$days days remaining" else "Expired",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (days <= 3) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        if (item.images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Images",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(item.images) { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Item image",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (item.contactPhone != null || item.contactEmail != null) {
            Text(
                text = "Contact Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            item.contactPhone?.let { phone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = phone, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            item.contactEmail?.let { email ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = email, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Button(
            onClick = {
                item.contactPhone?.let { phone ->
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                ),
            shape = RoundedCornerShape(12.dp),
            enabled = item.contactPhone != null,
        ) {
            Icon(Icons.Default.Phone, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Call ${item.contactName}")
        }
    }
}

@Composable
private fun CreateLostFoundScreen(
    onBack: () -> Unit,
    onSubmit: (title: String, description: String, location: String, itemType: LostFoundType, reward: String?, tags: List<String>, expiryDays: Int, images: List<String>) -> Unit,
    onCreateSuccess: () -> Unit,
    viewModel: LostAndFoundViewModel,
) {
    val createItemState by viewModel.createItemState.collectAsStateWithLifecycle()
    
    LaunchedEffect(createItemState) {
        when (createItemState) {
            is UiState.Success -> {
                onCreateSuccess()
                viewModel.clearCreateItemState()
            }
            else -> {}
        }
    }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(LostFoundType.LOST) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Report Item",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedType == LostFoundType.LOST,
                onClick = { selectedType = LostFoundType.LOST },
                label = { Text("Lost Item") },
            )
            FilterChip(
                selected = selectedType == LostFoundType.FOUND,
                onClick = { selectedType = LostFoundType.FOUND },
                label = { Text("Found Item") },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val titleError = title.isBlank()
        val descriptionError = description.isBlank()
        val locationError = location.isBlank()

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Item Title *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = titleError,
            supportingText = if (titleError) { { Text("Title is required", color = MaterialTheme.colorScheme.error) } } else null,
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description *") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            isError = descriptionError,
            supportingText = if (descriptionError) { { Text("Description is required", color = MaterialTheme.colorScheme.error) } } else null,
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = locationError,
            supportingText = if (locationError) { { Text("Location is required", color = MaterialTheme.colorScheme.error) } } else null,
        )

        var reward by remember { mutableStateOf("") }
        OutlinedTextField(
            value = reward,
            onValueChange = { reward = it },
            label = { Text("Reward (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        var tags by remember { mutableStateOf("") }
        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            label = { Text("Tags (e.g., technology, personal, jewelry)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        var expiryDays by remember { mutableStateOf(7) }
        Text(
            text = "Expiry: $expiryDays days",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = expiryDays.toFloat(),
            onValueChange = { expiryDays = it.toInt() },
            valueRange = 1f..30f,
            steps = 29,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents(),
        ) { uris: List<Uri> ->
            val newImages = (selectedImages + uris).take(10)
            selectedImages = newImages
        }

        Button(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Images (${selectedImages.size}/10)")
        }

        if (selectedImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(selectedImages) { imageUri ->
                    Box {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = { selectedImages = selectedImages - imageUri },
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val hasValidation = title.isBlank() || description.isBlank() || location.isBlank() || selectedImages.isEmpty()
        
        // Show validation errors or create item errors
        val errorText = when (val currentState = createItemState) {
            is UiState.Error -> currentState.message
            else -> when {
                hasValidation -> when {
                    title.isBlank() -> "Title is required"
                    description.isBlank() -> "Description is required"
                    location.isBlank() -> "Location is required"
                    selectedImages.isEmpty() -> "At least one image is required"
                    else -> ""
                }
                else -> ""
            }
        }
        
        if (errorText.isNotEmpty()) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Button(
            onClick = {
                if (!hasValidation) {
                    val tagsList = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }
                    val imageUrls = selectedImages.map { it.toString() }
                    val rewardText = if (reward.isBlank()) null else reward
                    
                    onSubmit(
                        title,
                        description,
                        location,
                        selectedType,
                        rewardText,
                        tagsList,
                        expiryDays,
                        imageUrls
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                ),
            enabled = !hasValidation && createItemState !is UiState.Loading,
            shape = RoundedCornerShape(12.dp),
        ) {
            if (createItemState is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Submit Report")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EditLostFoundScreen(
    item: LostFoundItem,
    onBack: () -> Unit,
    onSubmit: (title: String, description: String, location: String, reward: String?, tags: List<String>, isResolved: Boolean) -> Unit,
    onUpdateSuccess: () -> Unit,
    viewModel: LostAndFoundViewModel,
) {
    val updateItemState by viewModel.updateItemState.collectAsStateWithLifecycle()
    
    LaunchedEffect(updateItemState) {
        when (updateItemState) {
            is UiState.Success -> {
                onUpdateSuccess()
                viewModel.clearUpdateItemState()
            }
            else -> {}
        }
    }

    var title by remember { mutableStateOf(item.title) }
    var description by remember { mutableStateOf(item.description) }
    var location by remember { mutableStateOf(item.location) }
    var reward by remember { mutableStateOf(item.reward ?: "") }
    var tags by remember { mutableStateOf(item.tags.joinToString(", ")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit Item",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val titleError = title.isBlank()
        val descriptionError = description.isBlank()
        val locationError = location.isBlank()

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Item Title *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = titleError,
            supportingText = if (titleError) { { Text("Title is required", color = MaterialTheme.colorScheme.error) } } else null,
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description *") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            isError = descriptionError,
            supportingText = if (descriptionError) { { Text("Description is required", color = MaterialTheme.colorScheme.error) } } else null,
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = locationError,
            supportingText = if (locationError) { { Text("Location is required", color = MaterialTheme.colorScheme.error) } } else null,
        )

        OutlinedTextField(
            value = reward,
            onValueChange = { reward = it },
            label = { Text("Reward (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            label = { Text("Tags (e.g., technology, personal, jewelry)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Show validation or update errors
        val hasValidation = title.isBlank() || description.isBlank() || location.isBlank()
        val errorText = when (val currentState = updateItemState) {
            is UiState.Error -> currentState.message
            else -> when {
                hasValidation -> when {
                    title.isBlank() -> "Title is required"
                    description.isBlank() -> "Description is required"
                    location.isBlank() -> "Location is required"
                    else -> ""
                }
                else -> ""
            }
        }
        
        if (errorText.isNotEmpty()) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Button(
            onClick = {
                if (!hasValidation) {
                    val tagsList = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }
                    val rewardText = if (reward.isBlank()) null else reward
                    
                    onSubmit(
                        title,
                        description,
                        location,
                        rewardText,
                        tagsList,
                        false
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                ),
            enabled = !hasValidation && updateItemState !is UiState.Loading,
            shape = RoundedCornerShape(12.dp),
        ) {
            if (updateItemState is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Update Item")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
} 
