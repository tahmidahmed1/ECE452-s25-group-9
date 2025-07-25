package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.ui.components.ShutterLoadingView
import com.example.gooddeedfeed.presentation.ui.components.ToastManager
import com.example.gooddeedfeed.presentation.viewmodel.ChatConversation
import com.example.gooddeedfeed.presentation.viewmodel.ChatMessage
import com.example.gooddeedfeed.presentation.viewmodel.ChatViewModel

@Composable
fun SimpleChatScreen(
    user: DomainUser,
    otherUserId: Int? = null,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel<ChatViewModel>(),
) {
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val sendMessageState by viewModel.sendMessageState.collectAsStateWithLifecycle()

    var selectedConversation by remember { mutableStateOf<ChatConversation?>(null) }
    var showMessages by remember { mutableStateOf(otherUserId != null) }

    LaunchedEffect(user) {
        if (otherUserId != null) {
            viewModel.loadMessages(otherUserId, user)
        } else {
            viewModel.loadConversations(user)
        }
    }

    LaunchedEffect(sendMessageState) {
        when (val state = sendMessageState) {
            is UiState.Success -> {
                ToastManager.showSuccess(state.data)
                viewModel.clearSendMessageState()
            }
            is UiState.Error -> {
                ToastManager.showError(state.message)
                viewModel.clearSendMessageState()
            }
            else -> {}
        }
    }

    if (showMessages) {
        val conversation = selectedConversation ?: ChatConversation(
            id = "direct",
            title = "Direct Message",
            subtitle = "Chat with organizer",
            lastMessage = "",
            timestamp = "",
            otherUserId = otherUserId ?: 0,
        )

        ChatMessagesScreen(
            conversation = conversation,
            user = user,
            onBackClick = {
                showMessages = false
                selectedConversation = null
            },
            viewModel = viewModel,
            modifier = modifier,
        )
    } else {
        ChatListScreen(
            user = user,
            conversationsState = conversationsState,
            onConversationClick = { conversation ->
                selectedConversation = conversation
                showMessages = true
                viewModel.loadMessages(conversation.otherUserId, user)
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun ChatListScreen(
    user: DomainUser,
    conversationsState: UiState<List<ChatConversation>>,
    onConversationClick: (ChatConversation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Messages",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        when (conversationsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ShutterLoadingView()
                }
            }
            is UiState.Success -> {
                val conversations = conversationsState.data
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "No conversations",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No conversations yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = when (user.userType) {
                                    com.example.gooddeedfeed.domain.model.DomainUserType.ORGANIZER -> "Wait for incoming messages from volunteers"
                                    else -> "Start a conversation with an organizer"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(conversations) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = { onConversationClick(conversation) },
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = conversationsState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* Retry loading */ },
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ChatConversation,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = conversation.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatMessagesScreen(
    conversation: ChatConversation,
    user: DomainUser,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = conversation.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
        )

        Box(modifier = Modifier.weight(1f)) {
            when (messagesState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ShutterLoadingView()
                    }
                }
                is UiState.Success -> {
                    val messages = (messagesState as UiState.Success).data
                    if (messages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No messages yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            reverseLayout = true,
                        ) {
                            items(messages.reversed()) { message ->
                                ChatMessageItem(
                                    message = message,
                                    currentUserId = user.id,
                                )
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = (messagesState as UiState.Error).message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadMessages(conversation.otherUserId, user) },
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type your message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                )

                FilledIconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText, conversation.otherUserId, user)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    enabled = messageText.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message",
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    currentUserId: Int,
) {
    val isFromCurrentUser = message.senderId == currentUserId
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isFromCurrentUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                if (!isFromCurrentUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (message.senderType) {
                                        "volunteer" -> MaterialTheme.colorScheme.secondary
                                        "organizer" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.outline
                                    },
                                ),
                        )
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFromCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFromCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.align(if (isFromCurrentUser) Alignment.End else Alignment.Start),
                )
            }
        }
    }
} 
