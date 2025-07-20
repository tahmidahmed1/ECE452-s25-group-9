package com.example.gooddeedfeed.presentation.ui.screens

import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gooddeedfeed.BuildConfig
import com.example.gooddeedfeed.domain.model.DomainBadge
import com.example.gooddeedfeed.domain.model.DomainLeaderboardEntry
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.ui.components.ToastUtils
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.theme.AppConstants
import com.example.gooddeedfeed.presentation.viewmodel.BadgeViewModel
import com.example.gooddeedfeed.presentation.viewmodel.LeaderboardViewModel
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthUiState
import com.example.gooddeedfeed.presentation.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun StatsScreen(
    viewModel: LeaderboardViewModel = hiltViewModel(),
    badgeViewModel: BadgeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allBadgesState by badgeViewModel.allBadgesState.collectAsStateWithLifecycle()
    val userBadgesState by badgeViewModel.userBadgesState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    // Show error toast if there's an error
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            ToastUtils.showErrorToast(context, error)
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        // Header with icon and title (copied from ChatScreen)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Leaderboard,
                contentDescription = "Statistics",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        // Section: Karma Leaderboard
        SectionCard(title = "Karma Leaderboard") {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.entries.isEmpty()) {
                Text(
                    text = "No leaderboard entries available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Show current user position first if not in the current page
                    val currentUserId = when (val auth = authState) {
                        is AuthUiState.Success -> auth.user.id
                        else -> null
                    }
                    val userInList = uiState.entries.any { it.id == currentUserId }
                    if (!userInList && uiState.currentUserEntry != null) {
                        item {
                            LeaderboardEntryCard(entry = uiState.currentUserEntry, isCurrentUser = true)
                        }
                    }

                    itemsIndexed(uiState.entries) { index, entry ->
                        val isCurrent = entry.id == currentUserId
                        LeaderboardEntryCard(entry = entry, isCurrentUser = isCurrent)
                    }

                    // Loading indicator for pagination
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // Load more trigger
                    if (uiState.hasNextPage && !uiState.isLoadingMore) {
                        item {
                            LaunchedEffect(Unit) {
                                viewModel.loadNextPage()
                            }
                        }
                    }
                }
            }
        }

        VerticalSpacer(SpacingSize.Large)

        // Section: Badges
        SectionCard(title = "Badges") {
            when (val badgesState = allBadgesState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Success -> {
                    val allBadges = badgesState.data
                    val earnedBadgeIds = when (val userState = userBadgesState) {
                        is UiState.Success -> userState.data.map { it.badge.id }.toSet()
                        else -> emptySet()
                    }

                    val earnedBadges = allBadges.filter { earnedBadgeIds.contains(it.id) }
                    val unearnedBadges = allBadges.filter { !earnedBadgeIds.contains(it.id) }

                    if (earnedBadges.isEmpty()) {
                        // Show explanatory text when no badges are earned
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "No badges yet",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No badges earned yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Complete volunteer events to earn karma points and unlock badges!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Show preview of available badges
                            if (unearnedBadges.isNotEmpty()) {
                                Text(
                                    text = "Available badges:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    items(unearnedBadges.take(3)) { badge ->
                                        BadgeCard(badge = badge, isEarned = false)
                                    }
                                }
                            }
                        }
                    } else {
                        // Show earned badges first, then unearned badges
                        Column {
                            if (earnedBadges.isNotEmpty()) {
                                Text(
                                    text = "Your badges:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    items(earnedBadges) { badge ->
                                        BadgeCard(badge = badge, isEarned = true)
                                    }
                                }
                            }

                            if (unearnedBadges.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Available badges:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    items(unearnedBadges.take(3)) { badge ->
                                        BadgeCard(badge = badge, isEarned = false)
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = "Failed to load badges",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                is UiState.Idle -> {
                    // Idle state - show nothing or loading indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        VerticalSpacer(SpacingSize.Large)

        // Section: Subscriptions
        SubscriptionsSection()

        VerticalSpacer(SpacingSize.Large)

        // Section: Volunteer History
        SectionCard(title = "Volunteer History", showExport = true) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppConstants.VOLUNTEER_HISTORY_ITEMS.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(item.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (item.verified) Icons.Default.Star else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (item.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        // Development Mode Karma Increase Button (only for volunteers in dev mode)
        if (BuildConfig.DEV_MODE) {
            val currentAuthState = authState
            if (currentAuthState is AuthUiState.Success && currentAuthState.user.userType?.name == "VOLUNTEER") {
                VerticalSpacer(SpacingSize.Large)

                Button(
                    onClick = {
                        viewModel.increaseKarmaPointsDevOnly { updatedUser ->
                            // Update the user state in AuthViewModel to immediately reflect new karma points
                            authViewModel.updateUserState(updatedUser)
                            ToastUtils.showSuccessToast(context, "Karma points increased by 100!")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text("(Dev Mode) Increase Karma Points by 100")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BadgeCard(badge: DomainBadge, isEarned: Boolean) {
    val containerColor = if (isEarned) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val contentColor = if (isEarned) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .height(140.dp)
            .width(120.dp)
            .clip(MaterialTheme.shapes.medium),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxSize(),
        ) {
            // Badge icon
            Icon(
                imageVector = getIconForBadgeName(badge.iconName),
                contentDescription = badge.name,
                tint = if (isEarned) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Badge name
            Text(
                text = badge.name,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontWeight = if (isEarned) FontWeight.Bold else FontWeight.Normal,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Required points or earned indicator
            if (isEarned) {
                Text(
                    text = "✓ Earned",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = "${badge.requiredKarmaPoints} pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                )
            }
        }
    }
}

private fun exportHistoryPdf(context: android.content.Context, history: List<AppConstants.HistoryItem>) {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint()
    paint.textSize = 12f
    var y = 20f
    paint.isFakeBoldText = true
    canvas.drawText("Volunteer History", 10f, y, paint)
    paint.isFakeBoldText = false
    y += 20f
    history.forEach {
        canvas.drawText("${it.title} - ${it.date} - ${if (it.verified) "Verified" else "Unverified"}", 10f, y, paint)
        y += 16f
    }
    doc.finishPage(page)
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloads, "history.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()
}

// SectionCard composable wraps content in grey container and optional export button
@Composable
private fun SectionCard(
    title: String,
    showExport: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (showExport) {
                    FilledTonalButton(onClick = {
                        scope.launch {
                            exportHistoryPdf(context, AppConstants.VOLUNTEER_HISTORY_ITEMS)
                            ToastUtils.showSuccessToast(context, "History exported to Downloads")
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                }
            }
            VerticalSpacer()
            content()
        }
    }
}

@Composable
private fun LeaderboardEntryCard(
    entry: DomainLeaderboardEntry?,
    modifier: Modifier = Modifier,
    isCurrentUser: Boolean = false,
) {
    if (entry == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // No additional context needed

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (entry.rank) {
                1 -> MaterialTheme.colorScheme.primaryContainer
                2 -> MaterialTheme.colorScheme.secondaryContainer
                3 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Rank
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = when (entry.rank) {
                                1 -> MaterialTheme.colorScheme.primary
                                2 -> MaterialTheme.colorScheme.secondary
                                3 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = entry.rank.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = when (entry.rank) {
                            1, 2, 3 -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Profile picture or fallback
                if (entry.profilePictureUrl != null) {
                    AsyncImage(
                        model = entry.profilePictureUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and username
                Column {
                    Text(
                        text = entry.fullName ?: entry.username,
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = if (isCurrentUser) androidx.compose.ui.text.font.FontStyle.Italic else null),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (entry.fullName != null) {
                        Text(
                            text = "@${entry.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Karma points
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "${entry.karmaPoints}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "karma",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun getIconForBadgeName(iconName: String): ImageVector {
    return when (iconName) {
        "Star" -> Icons.Default.Star
        "LocalFireDepartment" -> Icons.Default.LocalFireDepartment
        "EmojiEvents" -> Icons.Default.EmojiEvents
        "WorkspacePremium" -> Icons.Default.WorkspacePremium
        "Check" -> Icons.Default.CheckCircle
        "Favorite" -> Icons.Default.Favorite
        "Shield" -> Icons.Default.Shield
        "Psychology" -> Icons.Default.Psychology
        else -> Icons.Default.Star
    }
}

@Composable
private fun SubscriptionsSection(
    subscriptionViewModel: com.example.gooddeedfeed.presentation.viewmodel.volunteer.SubscriptionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val subscriptionsState by subscriptionViewModel.subscriptionsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        subscriptionViewModel.getUserSubscriptions()
    }

    SectionCard(title = "Your Subscriptions") {
        when (val state = subscriptionsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Text(
                        text = "No subscriptions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.data.forEach { organizer ->
                            SubscriptionCard(
                                organizer = organizer,
                                onUnsubscribe = {
                                    coroutineScope.launch {
                                        subscriptionViewModel.unsubscribeFromOrganizer(organizer.id)
                                        com.example.gooddeedfeed.presentation.ui.components.ToastManager.showSuccess("Unsubscribed from ${organizer.organizationName ?: organizer.fullName ?: organizer.username}")
                                    }
                                },
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Text(
                    text = "Failed to load subscriptions: ${state.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            is UiState.Idle -> {
                // Initial state, show nothing or placeholder
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    organizer: DomainUser,
    onUnsubscribe: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = organizer.organizationName ?: organizer.fullName ?: organizer.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = organizer.organizationDescription ?: "No description available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            androidx.compose.material3.TextButton(
                onClick = onUnsubscribe,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Unsubscribe")
            }
        }
    }
} 
