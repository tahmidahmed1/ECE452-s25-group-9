package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.presentation.ui.theme.CornerRadius
import com.example.gooddeedfeed.presentation.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

// Data classes for chat list
data class ChatItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isStarred: Boolean = false,
    val participantCount: Int = 0,
    val eventType: String = "Community Event"
)

data class ChatMessage(
    val id: String,
    val content: String,
    val senderName: String,
    val senderType: String, // "volunteer", "organizer", "institution"
    val timestamp: String,
    val isFromCurrentUser: Boolean = false
)

// Sample chat data
private val sampleChats = listOf(
    ChatItem("1", "Beach Cleanup - Santa Monica", "with Mike Johnson + 15 others", "Perfect! The more volunteers, the bigger impact we can make...", "2:42 PM", 2, true, 16),
    ChatItem("2", "Food Bank Volunteer Drive", "with Sarah Wilson + 8 others", "Don't forget to bring your ID for check-in", "1:30 PM", 0, false, 9),
    ChatItem("3", "Community Garden Project", "with Alex Chen + 12 others", "The tomatoes are looking great this week!", "Yesterday", 1, false, 13),
    ChatItem("4", "Senior Center Reading Program", "with Emma Davis + 5 others", "Thank you all for making this week special for our seniors", "Yesterday", 0, true, 6),
    ChatItem("5", "Animal Shelter Weekend Help", "with Jordan Kim + 20 others", "We have some new puppies that need extra attention", "Tuesday", 3, false, 21),
    ChatItem("6", "Park Restoration Initiative", "with Maya Patel + 7 others", "Meet at the main entrance by 9 AM sharp", "Monday", 0, false, 8),
)

// Sample messages for individual chat
private val sampleMessages = listOf(
    ChatMessage("1", "Hey! I'm interested in volunteering for the beach cleanup event. What should I bring?", "Sarah Chen", "volunteer", "2:30 PM"),
    ChatMessage("2", "Hi Sarah! Thanks for your interest. Please bring work gloves, water bottle, and wear comfortable clothes. We'll provide trash bags and tools.", "Mike Johnson", "organizer", "2:32 PM", true),
    ChatMessage("3", "Perfect! Is there parking available at the location?", "Sarah Chen", "volunteer", "2:33 PM"),
    ChatMessage("4", "Yes, there's free parking at the beach lot. I'll send the exact address closer to the event date.", "Mike Johnson", "organizer", "2:35 PM", true),
    ChatMessage("5", "Great! Looking forward to it. How many volunteers are signed up so far?", "Sarah Chen", "volunteer", "2:36 PM"),
    ChatMessage("6", "We have 15 volunteers confirmed and space for 5 more. It's going to be a great turnout!", "Mike Johnson", "organizer", "2:38 PM", true),
    ChatMessage("7", "That's awesome! I'll invite some friends who might be interested.", "Sarah Chen", "volunteer", "2:40 PM"),
    ChatMessage("8", "Perfect! The more volunteers, the bigger impact we can make. Thanks for helping spread the word!", "Mike Johnson", "organizer", "2:42 PM", true),
)

@Composable
fun ChatScreen(
    user: DomainUser,
    modifier: Modifier = Modifier
) {
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    
    if (selectedChatId != null) {
        val selectedChat = sampleChats.find { it.id == selectedChatId }
        if (selectedChat != null) {
            ChatMessagesScreen(
                chat = selectedChat,
                user = user,
                onBackClick = { selectedChatId = null }
            )
        }
    } else {
        ChatListScreen(
            user = user,
            onChatClick = { chatId -> selectedChatId = chatId },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListScreen(
    user: DomainUser,
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var chats by remember { mutableStateOf(sampleChats) }
    var chatToDelete by remember { mutableStateOf<ChatItem?>(null) }
    
    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isBlank()) {
            chats
        } else {
            chats.filter { chat ->
                chat.title.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with icon and title (similar to ListScreen)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Event Chats",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                isSearchActive = it.isNotBlank()
            },
            placeholder = { Text("Search chats...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { 
                        searchQuery = ""
                        isSearchActive = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(CornerRadius.large),
            singleLine = true
        )

        // Chat list
        if (filteredChats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "No Chats",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isSearchActive) "No chats found" else "No active chats",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (isSearchActive) "Try a different search term" else "Join an event to start chatting",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredChats, key = { it.id }) { chat ->
                    SwipeableChatItem(
                        chat = chat,
                        onClick = { onChatClick(chat.id) },
                        onStar = { chatId ->
                            chats = chats.map { 
                                if (it.id == chatId) it.copy(isStarred = !it.isStarred)
                                else it
                            }
                        },
                        onDelete = { chatToDelete = it }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (chatToDelete != null) {
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Delete Chat") },
            text = { 
                Text("Are you sure you want to delete the chat \"${chatToDelete!!.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        chats = chats.filter { it.id != chatToDelete!!.id }
                        chatToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SwipeableChatItem(
    chat: ChatItem,
    onClick: () -> Unit,
    onStar: (String) -> Unit,
    onDelete: (ChatItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    
    val swipeThreshold = with(density) { 100.dp.toPx() }
    val maxSwipe = with(density) { 200.dp.toPx() }
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipe_offset"
    )

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Background actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Star action (left side)
            AnimatedVisibility(
                visible = offsetX > swipeThreshold,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (chat.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (chat.isStarred) "Unstar" else "Star",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Delete action (right side)
            AnimatedVisibility(
                visible = offsetX < -swipeThreshold,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            MaterialTheme.colorScheme.error,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Chat item card
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(chat.id) {
                    detectDragGestures(
                        onDragEnd = {
                            when {
                                offsetX > swipeThreshold -> {
                                    onStar(chat.id)
                                    offsetX = 0f
                                }
                                offsetX < -swipeThreshold -> {
                                    if (abs(offsetX) > maxSwipe * 0.8f) {
                                        onDelete(chat)
                                    }
                                    offsetX = 0f
                                }
                                else -> {
                                    offsetX = 0f
                                }
                            }
                        }
                    ) { _, dragAmount ->
                        val newOffset = offsetX + dragAmount.x
                        offsetX = newOffset.coerceIn(-maxSwipe, maxSwipe)
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chat info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chat.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (chat.isStarred) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Starred",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = chat.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Timestamp and unread count
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = chat.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (chat.unreadCount > 0) {
                        Badge(
                            modifier = Modifier.size(20.dp)
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessagesScreen(
    chat: ChatItem,
    user: DomainUser,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = chat.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        // Messages List
        Box(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                reverseLayout = true // Show newest messages at bottom
            ) {
                items(sampleMessages.reversed()) { message ->
                    ChatMessageItem(
                        message = message,
                        currentUserType = user.userType?.name?.lowercase() ?: "volunteer"
                    )
                }
                
                // Pagination loading indicator (for future implementation)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Load earlier messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(CornerRadius.medium)
                                )
                                .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                        )
                    }
                }
            }
        }

        // Message Input Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { 
                        Text(
                            text = "Type your message...",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(CornerRadius.large),
                    maxLines = 4
                )
                
                FilledIconButton(
                    onClick = { 
                        // TODO: Implement send message functionality
                        if (messageText.isNotBlank()) {
                            messageText = ""
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    currentUserType: String,
    modifier: Modifier = Modifier
) {
    val isFromCurrentUser = message.isFromCurrentUser
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = CornerRadius.medium,
                topEnd = CornerRadius.medium,
                bottomStart = if (isFromCurrentUser) CornerRadius.medium else CornerRadius.small,
                bottomEnd = if (isFromCurrentUser) CornerRadius.small else CornerRadius.medium
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isFromCurrentUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.medium)
            ) {
                if (!isFromCurrentUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        // User type indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (message.senderType) {
                                        "volunteer" -> MaterialTheme.colorScheme.secondary
                                        "organizer" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                        
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = message.senderType.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                }
                
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFromCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFromCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.align(if (isFromCurrentUser) Alignment.End else Alignment.Start)
                )
            }
        }
    }
} 