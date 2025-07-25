@file:OptIn(ExperimentalFoundationApi::class)

package com.example.gooddeedfeed.presentation.ui.components.messaging

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gooddeedfeed.domain.model.*
import com.example.gooddeedfeed.presentation.ui.theme.Constants
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedMessageItem(
    messageUiState: MessageUiState,
    onReactionClick: (String) -> Unit,
    onReactionRemove: (Int) -> Unit,
    onShowReactionPicker: () -> Unit,
    onToggleImportant: (Boolean) -> Unit,
    onToggleDeleted: (Boolean) -> Unit,
    showReactionPicker: Boolean,
    isImportant: Boolean,
    userReaction: MessageReaction?,
    modifier: Modifier = Modifier,
) {
    val message = messageUiState.message
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = with(density) { 100.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > swipeThreshold -> {
                                // Right swipe - star/important
                                onToggleImportant(!isImportant)
                            }
                            offsetX < -swipeThreshold -> {
                                // Left swipe - delete
                                onToggleDeleted(true)
                            }
                        }
                        offsetX = 0f
                    },
                ) { change, dragAmount ->
                    offsetX += dragAmount.x
                    // Limit the offset
                    offsetX = offsetX.coerceIn(-swipeThreshold * 1.5f, swipeThreshold * 1.5f)
                }
            },
    ) {
        // Background actions
        if (abs(offsetX) > 20f) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = if (offsetX > 0) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (offsetX > 0) {
                    // Right swipe - star action
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isImportant) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isImportant) "Remove star" else "Add star",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer(alpha = (abs(offsetX) / swipeThreshold).coerceAtMost(1f)),
                        )
                    }
                } else {
                    // Left swipe - delete action
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete message",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer(alpha = (abs(offsetX) / swipeThreshold).coerceAtMost(1f)),
                        )
                    }
                }
            }
        }

        // Message content
        MessageBubble(
            message = message,
            isImportant = isImportant,
            onLongClick = onShowReactionPicker,
            modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) },
        )

        // Reaction picker feature removed

        // Reaction display removed
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: DomainMessage,
    isImportant: Boolean,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Themed message bubble colors
    val bubbleColor = if (message.isFromCurrentUser) {
        if (isImportant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    } else {
        if (isImportant) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (message.isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        horizontalArrangement = if (message.isFromCurrentUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = { },
                    onLongClick = onLongClick,
                ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromCurrentUser) 4.dp else 16.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            border = if (isImportant) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                if (!message.isFromCurrentUser) {
                    Text(
                        text = message.senderUsername,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Constants.Colors.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 14.sp,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isImportant) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Important",
                            tint = Constants.Colors.starYellow,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = message.createdAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f),
                    )

                    if (message.isFromCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReactionPickerOverlay(
    onReactionClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        LazyRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MessageEmojis.ALL) { emoji ->
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .clickable { onReactionClick(emoji) }
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
fun ReactionRow(
    reactions: List<MessageReaction>,
    userReaction: MessageReaction?,
    onReactionClick: (MessageReaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Group reactions by emoji
    val groupedReactions = reactions.groupBy { it.emoji }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(groupedReactions.entries.toList()) { (emoji, reactionList) ->
            ReactionChip(
                emoji = emoji,
                count = reactionList.size,
                isUserReaction = reactionList.any { it.userId == userReaction?.userId },
                onClick = { onReactionClick(reactionList.first()) },
            )
        }
    }
}

@Composable
private fun ReactionChip(
    emoji: String,
    count: Int,
    isUserReaction: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Circular reaction bubble
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (isUserReaction) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape = CircleShape,
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emoji,
                fontSize = 16.sp,
            )
        }
        if (count > 1) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ChatSummaryItem(
    chatSummary: ChatSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (chatSummary.isImportant) {
                Constants.Colors.lightBlue.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = if (chatSummary.isImportant) {
            BorderStroke(1.dp, Constants.Colors.lightBlue)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Profile picture placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Constants.Colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chatSummary.otherUserUsername.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = chatSummary.otherUserFullName ?: chatSummary.otherUserUsername,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                    )

                    if (chatSummary.isImportant) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Important conversation",
                            tint = Constants.Colors.starYellow,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = chatSummary.latestMessageTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 12.sp,
                        color = Constants.Colors.darkGray,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = chatSummary.latestMessage,
                        fontSize = 14.sp,
                        color = Constants.Colors.darkGray,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )

                    if (chatSummary.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = Constants.Colors.primary,
                        ) {
                            Text(
                                text = if (chatSummary.unreadCount > 99) "99+" else chatSummary.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
