package com.example.gooddeedfeed.presentation.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.ui.components.base.ActionCard
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.base.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.base.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.viewmodel.common.HomeAction
import com.example.gooddeedfeed.presentation.viewmodel.common.HomeViewModel
import com.example.gooddeedfeed.presentation.viewmodel.common.UserTypeDisplay
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: DomainUser,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel<HomeViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(user) {
        viewModel.loadUserHome(user)
    }

    when (val currentState = uiState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Success -> {
            val homeData = currentState.data
            HomeContent(
                user = homeData.user,
                userTypeDisplay = homeData.userTypeDisplay,
                onActionClick = { action -> viewModel.handleAction(action) },
                onLogout = onLogout,
            )
        }
        is UiState.Error -> {
            ScreenContainer {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Retry",
                        onClick = { viewModel.loadUserHome(user) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    user: DomainUser,
    userTypeDisplay: UserTypeDisplay,
    onActionClick: (HomeAction) -> Unit,
    onLogout: () -> Unit,
) {
    ScreenContainer {
        // Welcome header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = when (user.userType) {
                    DomainUserType.VOLUNTEER -> Icons.Default.Person
                    DomainUserType.ORGANIZER -> Icons.Default.Star
                    DomainUserType.INSTITUTION -> Icons.Default.Home
                    null -> Icons.Default.Person
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Welcome, ${user.fullName ?: user.username}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = userTypeDisplay.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // User type specific content
        Text(
            text = userTypeDisplay.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        VerticalSpacer(SpacingSize.Small)

        // Action items
        userTypeDisplay.actionItems.forEach { actionItem ->
            ActionCard(
                icon = when (actionItem.iconName) {
                    "list" -> Icons.AutoMirrored.Filled.List
                    else -> getIconForAction(actionItem.iconName)
                },
                title = actionItem.title,
                description = actionItem.description,
                onClick = { onActionClick(actionItem.action) },
            )
            VerticalSpacer(SpacingSize.Small)
        }

        // Volunteer-specific calendar
        if (user.userType == DomainUserType.VOLUNTEER) {
            VolunteerCalendarView()
            VerticalSpacer(SpacingSize.Large)
        }

        VerticalSpacer()
    }
}

@Composable
private fun getIconForAction(iconName: String) = when (iconName) {
    "favorite" -> Icons.Default.Favorite
    "star" -> Icons.Default.Star
    "info" -> Icons.Default.Info
    else -> Icons.AutoMirrored.Filled.List
}

@SuppressLint("NewApi")
@Composable
private fun VolunteerCalendarView() {
    val startMonth = YearMonth.now().minusMonths(12)
    val endMonth = YearMonth.now().plusMonths(12)
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = YearMonth.now(),
        firstDayOfWeek = firstDayOfWeekFromLocale(),
    )

    VerticalCalendar(
        state = calendarState,
        monthHeader = { month ->
            Text(
                text = month.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        dayContent = { day ->
            val dayColor = if (day.position == DayPosition.MonthDate) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = dayColor,
                )
            }
        },
    )
}
