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
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.theme.BorderRadius
import com.example.gooddeedfeed.presentation.theme.Elevation
import com.example.gooddeedfeed.presentation.theme.Spacing
import com.example.gooddeedfeed.presentation.ui.components.BadgeManager
import com.example.gooddeedfeed.presentation.ui.screens.volunteer.LostAndFoundScreen
import com.example.gooddeedfeed.presentation.viewmodel.BadgeViewModel
import com.example.gooddeedfeed.presentation.viewmodel.common.HomeAction
import com.example.gooddeedfeed.presentation.viewmodel.common.HomeViewModel

@Composable
fun FloatingNavBarItem(
    icon: ImageVector,
    isSelected: Boolean,
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
    }
}

@Composable
fun FloatingNavigationBar(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
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
                FloatingNavBarItem(
                    icon = tab.icon,
                    isSelected = selectedTabIndex == index,
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
) {
    // Observe home navigation events to switch bottom bar tabs
    val homeViewModel: HomeViewModel = hiltViewModel()
    val badgeViewModel: BadgeViewModel = hiltViewModel()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showPreviewProfile by remember { mutableStateOf(false) }
    var showPrivacySettings by remember { mutableStateOf(false) }
    var showLostAndFound by remember { mutableStateOf(false) }

    // Generate tabs based on user type early
    val tabs = getTabsForUserType(user.userType ?: DomainUserType.VOLUNTEER)

    // Handle navigation events from HomeScreen using precomputed tabs
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
                HomeAction.CreateEvent, HomeAction.ManageEvents -> {
                    val index = tabs.indexOfFirst { tab: TabItem -> tab.title == "Events" }
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
                    user = user,
                    onEditProfile = { showEditProfile = true },
                    onPreviewProfile = { showPreviewProfile = true },
                    onEditPrivacy = { showPrivacySettings = true },
                    onLogout = onLogout,
                )
            },
            bottomBar = {
                FloatingNavigationBar(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    modifier = Modifier.padding(bottom = Spacing.md),
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                tabs[selectedTabIndex].screen(user, onLogout)
            }
        }

        // Toast overlay is handled at the app level in AppNavHost

        // Edit Profile Overlay - appears over everything including bottom bar
        androidx.compose.animation.AnimatedVisibility(
            visible = showEditProfile,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
        ) {
            com.example.gooddeedfeed.presentation.ui.screens.EditProfileScreen(
                user = user,
                onCancel = { showEditProfile = false },
                onSave = { showEditProfile = false },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Preview Profile Overlay - appears over everything including bottom bar
        androidx.compose.animation.AnimatedVisibility(
            visible = showPreviewProfile,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
        ) {
            com.example.gooddeedfeed.presentation.ui.screens.PreviewProfileScreen(
                user = user,
                onBack = { showPreviewProfile = false },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Privacy & Notifications Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = showPrivacySettings,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
        ) {
            com.example.gooddeedfeed.presentation.ui.screens.PrivacySettingsScreen(
                user = user,
                onClose = { showPrivacySettings = false },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Lost & Found Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = showLostAndFound,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
        ) {
            LostAndFoundScreen(
                user = user,
                onBack = { showLostAndFound = false },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Badge achievement manager for showing badge popups
        BadgeManager(
            badgeViewModel = badgeViewModel,
            userKarmaPoints = user.karmaPoints,
        )
    }
}
