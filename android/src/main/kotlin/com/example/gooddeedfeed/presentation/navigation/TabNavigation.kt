package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.theme.BorderRadius
import com.example.gooddeedfeed.presentation.theme.Elevation
import com.example.gooddeedfeed.presentation.theme.Spacing
import com.example.gooddeedfeed.presentation.ui.components.BadgeManager
import com.example.gooddeedfeed.presentation.ui.screens.organizer.CreateEventScreen
import com.example.gooddeedfeed.presentation.ui.screens.volunteer.LostAndFoundScreen
import com.example.gooddeedfeed.presentation.viewmodel.BadgeViewModel
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthViewModel
import com.example.gooddeedfeed.presentation.viewmodel.common.HomeAction
import com.example.gooddeedfeed.presentation.viewmodel.common.HomeViewModel
import com.example.gooddeedfeed.presentation.viewmodel.common.NotificationViewModel
import com.example.gooddeedfeed.domain.util.MessageNotificationEvent

@Composable
fun FloatingNavBarItem(
    icon: ImageVector,
    isSelected: Boolean,
    hasUnread: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = tween(200),
        label = "nav_item_scale",
    )

    val iconColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(BorderRadius.lg))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )

        if (hasUnread) {
            // Small dot in top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.error),
            )
        }
    }
}

@Composable
fun FloatingNavigationBar(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    chatTabIndex: Int? = null,
    hasUnreadChat: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 90% of screen width
                .height(64.dp)
                .shadow(
                    elevation = Elevation.lg,
                    shape = RoundedCornerShape(BorderRadius.xxl),
                    clip = false,
                )
                .clip(RoundedCornerShape(BorderRadius.xxl))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val showUnreadDot = when {
                    // Show unread indicator on Chat tab ONLY if there are unread chat messages
                    // General notifications have their own indicator in the top app bar
                    chatTabIndex != null && index == chatTabIndex && hasUnreadChat -> true
                    else -> false
                }
                FloatingNavBarItem(
                    icon = tab.icon,
                    isSelected = selectedTabIndex == index,
                    hasUnread = showUnreadDot,
                    onClick = { onTabSelected(index) },
                )
            }
        }
    }
}

@Composable
fun TabNavigationScreen(
    user: DomainUser,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit = {},
) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val badgeViewModel: BadgeViewModel = hiltViewModel()
    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val chatViewModel: com.example.gooddeedfeed.presentation.viewmodel.ChatViewModel = hiltViewModel()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showPreviewProfile by remember { mutableStateOf(false) }
    var showPrivacySettings by remember { mutableStateOf(false) }
    var showLostAndFound by remember { mutableStateOf(false) }
    var showCreateEvent by remember { mutableStateOf(false) }
    var showChatProfile by remember { mutableStateOf(false) }
    var chatProfileUser by remember { mutableStateOf<DomainUser?>(null) }

    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val currentUser = (authState as? AuthUiState.Success)?.user ?: user

    LaunchedEffect(Unit) {
        authViewModel.refreshUser()
    }

    // Listen for real-time message notifications to update chat badge immediately
    // This ensures nav bar updates when messages arrive, separate from ChatViewModel's internal handling
    LaunchedEffect(currentUser.id) {
        notificationViewModel.eventBus.messageNotificationEvents.collect { event ->
            when (event) {
                is MessageNotificationEvent.NewMessage -> {
                    // Only refresh conversations for nav bar updates, don't interfere with ChatViewModel
                    if (event.receiverId == currentUser.id) {
                        // Use a small delay to let WebSocket processing complete first
                        kotlinx.coroutines.delay(200L)
                        chatViewModel.loadConversations(currentUser)
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedTabIndex) {
        authViewModel.refreshUser()
    }

    val tabs = getTabsForUserType(currentUser.userType ?: DomainUserType.VOLUNTEER)

    // Load conversations once user info is ready
    LaunchedEffect(currentUser.id) {
        chatViewModel.loadConversations(currentUser)
    }

    val chatConversationsState by chatViewModel.conversationsState.collectAsStateWithLifecycle()
    val hasUnreadMessages = (chatConversationsState as? com.example.gooddeedfeed.presentation.common.UiState.Success)?.data?.any { it.unreadCount > 0 } == true

    // Notification unread count is handled by AppTopBar, not navigation bar

    val chatTabIndex = tabs.indexOfFirst { it.title == "Chat" }.takeIf { it >= 0 }

    LaunchedEffect(homeViewModel) {
        homeViewModel.navigationEvent.collect { action ->
            selectedTabIndex = when (action) {
                HomeAction.BrowseOpportunities -> {
                    val index = tabs.indexOfFirst { tab: TabItem -> tab.title.contains("Opp") }
                    if (index >= 0) index else selectedTabIndex
                }
                HomeAction.ViewMyActivities -> {
                    val index = tabs.indexOfFirst { tab: TabItem -> tab.title.contains("My Activities") }
                    if (index >= 0) index else selectedTabIndex
                }
                HomeAction.LostAndFound -> {
                    showLostAndFound = true
                    selectedTabIndex
                }
                HomeAction.CreateEvent -> {
                    showCreateEvent = true
                    selectedTabIndex
                }
                HomeAction.ManageEvents -> {
                    val index = tabs.indexOfFirst { tab: TabItem -> tab.title == "Manage Events" }
                    if (index >= 0) index else selectedTabIndex
                }
                HomeAction.Chat -> {
                    val index = tabs.indexOfFirst { tab: TabItem -> tab.title == "Chat" }
                    if (index >= 0) index else selectedTabIndex
                }
                HomeAction.ViewDashboard, HomeAction.ManagePrograms -> {
                    val index = tabs.indexOfFirst { tab: TabItem -> tab.title.contains("Review") || tab.title.contains("Programs") }
                    if (index >= 0) index else selectedTabIndex
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AppTopBar(
                    user = currentUser,
                    onEditProfile = onEditProfile,
                    onPreviewProfile = { showPreviewProfile = true },
                    onEditPrivacy = { showPrivacySettings = true },
                    onLogout = onLogout,
                )
            },
            bottomBar = {
                if (!showLostAndFound) {
                    FloatingNavigationBar(
                        tabs = tabs,
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = { selectedTabIndex = it },
                        chatTabIndex = chatTabIndex,
                        hasUnreadChat = hasUnreadMessages,
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                tabs[selectedTabIndex].screen(
                    currentUser,
                    onLogout,
                    { profileUser ->
                        chatProfileUser = profileUser
                        showChatProfile = true
                    },
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showLostAndFound,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        LostAndFoundScreen(
                            user = currentUser,
                            onBack = { showLostAndFound = false },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showCreateEvent,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        CreateEventScreen(
                            onBack = { showCreateEvent = false },
                        )
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showPreviewProfile,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
        ) {
            com.example.gooddeedfeed.presentation.ui.screens.PreviewProfileScreen(
                user = currentUser,
                onBack = { showPreviewProfile = false },
                modifier = Modifier.fillMaxSize(),
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showPrivacySettings,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
        ) {
            com.example.gooddeedfeed.presentation.ui.screens.PrivacySettingsScreen(
                user = currentUser,
                onClose = { showPrivacySettings = false },
                modifier = Modifier.fillMaxSize(),
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showChatProfile,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
        ) {
            chatProfileUser?.let { profileUser ->
                com.example.gooddeedfeed.presentation.ui.screens.PreviewProfileScreen(
                    user = profileUser,
                    onBack = { showChatProfile = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        BadgeManager(
            badgeViewModel = badgeViewModel,
            userKarmaPoints = currentUser.karmaPoints,
        )
    }
}
