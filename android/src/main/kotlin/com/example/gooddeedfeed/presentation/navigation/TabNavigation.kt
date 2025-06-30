package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
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
import com.example.gooddeedfeed.presentation.theme.BorderRadius
import com.example.gooddeedfeed.presentation.theme.Elevation
import com.example.gooddeedfeed.presentation.theme.Spacing
import com.example.gooddeedfeed.presentation.navigation.AppTopBar
import com.example.gooddeedfeed.presentation.viewmodel.common.HomeAction
import com.example.gooddeedfeed.presentation.viewmodel.common.HomeViewModel

data class TabItem(
    val title: String,
    val icon: ImageVector,
    val screen: @Composable (DomainUser, () -> Unit) -> Unit,
)

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
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showPreviewProfile by remember { mutableStateOf(false) }

    // Generate tabs based on user type early
    val tabs = NavigationConfig.getTabsForUserType(user.userType)

    // Handle navigation events from HomeScreen using precomputed tabs
    LaunchedEffect(homeViewModel) {
        homeViewModel.navigationEvent.collect { action ->
            selectedTabIndex = when (action) {
                HomeAction.BrowseOpportunities -> tabs.indexOfFirst { it.title.contains("Opp") }.let { if (it >= 0) it else selectedTabIndex }
                HomeAction.ViewMyActivities -> tabs.indexOfFirst { it.title.contains("My Activities") }.let { if (it >= 0) it else selectedTabIndex }
                HomeAction.CreateEvent, HomeAction.ManageEvents -> tabs.indexOfFirst { it.title == "Events" }.let { if (it >= 0) it else selectedTabIndex }
                HomeAction.ViewDashboard, HomeAction.ManagePrograms -> tabs.indexOfFirst { it.title.contains("Review") || it.title.contains("Programs") }.let { if (it >= 0) it else selectedTabIndex }
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
    }
}
