package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gooddeedfeed.domain.model.DomainUser

@Composable
fun AppTopBar(
    user: DomainUser,
    onEditProfile: () -> Unit = {},
    onPreviewProfile: () -> Unit = {},
    onEditPrivacy: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showNotifMenu by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp + statusBarHeight),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
        ),
    ) {
        Column {
            Spacer(modifier = Modifier.height(statusBarHeight))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Only show karma for volunteers
                if (user.userType?.name == "VOLUNTEER") {
                    Surface(
                        modifier = Modifier,
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Karma",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (user.karmaPoints ?: 0).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { showNotifMenu = true },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shadowElevation = 2.dp,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset((-2).dp, 2.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error,
                        ) {}

                        ModernDropdownMenu(
                            expanded = showNotifMenu,
                            onDismissRequest = { showNotifMenu = false },
                            modifier = Modifier.width(260.dp),
                        ) {
                            ModernDropdownMenuItem(
                                text = "No new notifications",
                                icon = Icons.Default.Notifications,
                                onClick = { showNotifMenu = false },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            )
                            ModernDropdownMenuItem(
                                text = "Clear All",
                                icon = null,
                                onClick = { showNotifMenu = false },
                            )
                        }
                    }

                    Box {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { showProfileMenu = true },
                            shape = if (user.profilePictureUrl != null) CircleShape else RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shadowElevation = 2.dp,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (user.profilePictureUrl != null) {
                                    AsyncImage(
                                        model = user.profilePictureUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }

                        ModernDropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false },
                            modifier = Modifier.width(180.dp),
                        ) {
                            ModernDropdownMenuItem(
                                text = "Preview Profile",
                                icon = Icons.Default.Person,
                                onClick = {
                                    onPreviewProfile()
                                    showProfileMenu = false
                                },
                            )
                            ModernDropdownMenuItem(
                                text = "Edit Profile",
                                icon = Icons.Default.Edit,
                                onClick = {
                                    onEditProfile()
                                    showProfileMenu = false
                                },
                            )
                            ModernDropdownMenuItem(
                                text = "Privacy & Notifications",
                                icon = Icons.Default.Notifications,
                                onClick = {
                                    onEditPrivacy()
                                    showProfileMenu = false
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            )
                            ModernDropdownMenuItem(
                                text = "Log Out",
                                icon = null,
                                onClick = {
                                    onLogout()
                                    showProfileMenu = false
                                },
                                isDestructive = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern dropdown menu with translucent background and rounded corners
 */
@Composable
fun ModernDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                RoundedCornerShape(12.dp),
            ),
        content = content,
    )
}

/**
 * Modern dropdown menu item with icon support and better styling
 */
@Composable
fun ModernDropdownMenuItem(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isDestructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        },
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    )
}
