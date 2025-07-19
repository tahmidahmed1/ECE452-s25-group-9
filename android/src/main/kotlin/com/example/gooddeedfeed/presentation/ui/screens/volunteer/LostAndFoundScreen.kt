package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.domain.model.DomainUser

data class LostFoundItem(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val type: LostFoundType,
    val images: List<String>,
    val contactName: String,
    val isResolved: Boolean = false,
)

enum class LostFoundType {
    LOST, FOUND
}

private val mockItems = listOf(
    LostFoundItem(
        id = "1",
        title = "Black iPhone 14",
        description = "Lost my black iPhone 14 near the beach cleanup event. Has a blue case with stickers.",
        location = "Santa Monica Beach",
        date = "2 days ago",
        type = LostFoundType.LOST,
        images = listOf(
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=400&q=60",
            "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=400&q=60",
        ),
        contactName = "Sarah Chen",
    ),
    LostFoundItem(
        id = "2",
        title = "Blue Water Bottle",
        description = "Found this blue water bottle at the community garden. Has 'Mike' written on it.",
        location = "Community Garden",
        date = "1 day ago",
        type = LostFoundType.FOUND,
        images = listOf(
            "https://images.unsplash.com/photo-1523362628745-0c100150b504?auto=format&fit=crop&w=400&q=60",
        ),
        contactName = "Alex Johnson",
    ),
    LostFoundItem(
        id = "3",
        title = "Red Backpack",
        description = "Lost my red hiking backpack during the park cleanup. Contains my volunteer badge and some personal items.",
        location = "Central Park",
        date = "3 days ago",
        type = LostFoundType.LOST,
        images = listOf(
            "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=400&q=60",
        ),
        contactName = "Emma Davis",
    ),
    LostFoundItem(
        id = "4",
        title = "Silver Watch",
        description = "Found this silver watch near the food bank entrance. Looks like it might be expensive.",
        location = "Food Bank",
        date = "5 days ago",
        type = LostFoundType.FOUND,
        images = listOf(
            "https://images.unsplash.com/photo-1524592094714-0f0654e20314?auto=format&fit=crop&w=400&q=60",
        ),
        contactName = "Jordan Kim",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen(
    user: DomainUser,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var selectedItem by remember { mutableStateOf<LostFoundItem?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<LostFoundType?>(null) }

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
            onSubmit = { showCreateForm = false },
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Header
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
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Report Item")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips
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

        // Items list
        val filteredItems = if (selectedFilter == null) {
            mockItems
        } else {
            mockItems.filter { it.type == selectedFilter }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(filteredItems) { item ->
                LostFoundItemCard(
                    item = item,
                    onClick = { selectedItem = item },
                )
            }
        }
    }
}

@Composable
private fun LostFoundItemCard(
    item: LostFoundItem,
    onClick: () -> Unit,
) {
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
        }
    }
}

@Composable
private fun LostFoundDetailScreen(
    item: LostFoundItem,
    onBack: () -> Unit,
) {
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

        Button(
            onClick = { /* Mock contact */ },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Phone, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Contact ${item.contactName}")
        }
    }
}

@Composable
private fun CreateLostFoundScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
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

        // Type selection
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

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Item Title") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { /* Mock image picker */ },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Images")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && description.isNotBlank() && location.isNotBlank(),
        ) {
            Text("Submit Report")
        }
    }
} 
