package com.example.gooddeedfeed.presentation.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainUserType
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.ui.components.NotificationPromptDialog
import com.example.gooddeedfeed.presentation.ui.components.base.ActionCard
import com.example.gooddeedfeed.presentation.ui.components.base.InfoCard
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: DomainUser,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel<HomeViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasNotificationPromptBeenShown by remember { mutableStateOf(true) }
    var showNotificationPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        viewModel.loadUserHome(user)
    }

    LaunchedEffect(user) {
        if (user.onboardingCompleted == true) {
            hasNotificationPromptBeenShown = viewModel.hasNotificationPromptBeenShown()
            if (!hasNotificationPromptBeenShown) {
                showNotificationPrompt = true
            }
        }
    }

    when (val currentState = uiState) {
        is UiState.Idle -> {
            Box(modifier = Modifier.fillMaxSize()) {}
        }
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
                homeViewModel = viewModel, // Pass viewModel here
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

    if (showNotificationPrompt && user.userType != null) {
        NotificationPromptDialog(
            userType = user.userType!!,
            onEnableNotifications = {
                viewModel.enableNotifications()
                viewModel.markNotificationPromptAsShown()
                showNotificationPrompt = false
            },
            onSkip = {
                viewModel.markNotificationPromptAsShown()
                viewModel.disableNotifications()
                showNotificationPrompt = false
            },
            onDismiss = {
                showNotificationPrompt = false
            },
        )
    }
}

@Composable
private fun HomeContent(
    user: DomainUser,
    userTypeDisplay: UserTypeDisplay,
    onActionClick: (HomeAction) -> Unit,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel, // Add this parameter
) {
    ScreenContainer {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = when (user.userType) {
                        DomainUserType.VOLUNTEER -> Icons.Default.Person
                        DomainUserType.ORGANIZER -> Icons.Default.Star
                        null -> Icons.Default.Home
                        null -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = when (user.userType) {
                            DomainUserType.ORGANIZER -> "Welcome, ${user.organizationName ?: user.username}"
                            DomainUserType.VOLUNTEER -> "Welcome, ${user.fullName ?: user.username}"
                            null -> "Welcome, ${user.username}"
                        },
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

            val filteredItems = userTypeDisplay.actionItems.filterNot {
                it.title.contains("Browse Opportunities") || it.title.contains("My Activities")
            }

            filteredItems.forEach { actionItem ->
                ActionCard(
                    icon = when (actionItem.iconName) {
                        "list" -> Icons.AutoMirrored.Filled.List
                        "chat" -> Icons.Default.Chat
                        else -> getIconForAction(actionItem.iconName)
                    },
                    title = actionItem.title,
                    description = actionItem.description,
                    onClick = { onActionClick(actionItem.action) },
                    showBorder = actionItem.title == "Lost & Found",
                )
                VerticalSpacer(SpacingSize.Small)
            }

            // Opportunity Idea Generator for organizers
            if (user.userType == DomainUserType.ORGANIZER) {
                val ideas by homeViewModel.ideaSuggestions.collectAsStateWithLifecycle()
                val isGenerating by homeViewModel.isGeneratingIdeas.collectAsStateWithLifecycle()

                DisposableEffect(Unit) {
                    onDispose {
                        homeViewModel.resetOpportunityIdeas()
                    }
                }

                InfoCard(
                    title = "Opportunity Ideas",
                    content = if (ideas.isNotEmpty()) ideas.joinToString(separator = "\n• ", prefix = "• ") else "Tap below to generate ideas for your next event.",
                    icon = Icons.Default.Lightbulb,
                )
                VerticalSpacer(SpacingSize.Small)
                PrimaryButton(
                    text = if (isGenerating) "Generating Ideas..." else "Generate Ideas",
                    onClick = { homeViewModel.generateOpportunityIdeas() },
                    enabled = !isGenerating,
                )
                VerticalSpacer(SpacingSize.Large)
            }

            if (user.userType == DomainUserType.VOLUNTEER) {
                VolunteerCalendarView(homeViewModel = homeViewModel)
                VerticalSpacer(SpacingSize.Large)
            }

            VerticalSpacer()
        }
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
private fun VolunteerCalendarView(homeViewModel: HomeViewModel) {
    data class EventItem(val title: String, val time: String)

    val joinedEventsState by homeViewModel.joinedEventsState.collectAsStateWithLifecycle()

    // Load joined events when the component is created
    LaunchedEffect(Unit) {
        homeViewModel.loadJoinedEvents()
    }

    // Helper function to parse dates with multiple formats
    fun parseDate(dateStr: String): LocalDate? {
        val patterns = listOf("yyyy-MM-dd", "MMM d, yyyy", "MMMM d, yyyy")
        for (pattern in patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
            } catch (e: Exception) {
                // Continue to next pattern
            }
        }
        return null
    }

    val eventsMap = remember(joinedEventsState) {
        android.util.Log.i("HomeScreen", "🗓️ CALENDAR VIEW - Processing joinedEventsState: ${joinedEventsState::class.simpleName}")
        val data = (joinedEventsState as? UiState.Success)?.data
        if (data != null) {
            android.util.Log.i("HomeScreen", "🗓️ CALENDAR VIEW - Processing ${data.size} joined events for calendar display")
            data.forEach { event ->
                android.util.Log.i("HomeScreen", "  📅 Processing event: '${event.title}' on ${event.date} (${event.startTime} - ${event.endTime})")
            }
            
            val grouped: Map<LocalDate, List<EventItem>> = data.mapNotNull { event ->
                val parsedDate = parseDate(event.date)
                if (parsedDate != null) {
                    android.util.Log.i("HomeScreen", "  ✅ Successfully parsed date '${event.date}' for event '${event.title}'")
                    parsedDate to EventItem(
                        title = event.title,
                        time = formatEventTime(event.startTime, event.endTime),
                    )
                } else {
                    android.util.Log.w("HomeScreen", "  ⚠️ Failed to parse date '${event.date}' for event '${event.title}'")
                    null
                }
            }.groupBy({ it.first }, { it.second })
            
            android.util.Log.i("HomeScreen", "🗓️ CALENDAR VIEW - Created events map with ${grouped.size} dates")
            grouped.forEach { (date, events) ->
                android.util.Log.i("HomeScreen", "  📆 Date $date has ${events.size} events: ${events.map { it.title }}")
            }
            grouped
        } else {
            android.util.Log.w("HomeScreen", "🗓️ CALENDAR VIEW - No data available from joinedEventsState")
            emptyMap()
        }
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val startMonth = YearMonth.now().minusMonths(12)
    val endMonth = YearMonth.now().plusMonths(12)
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = YearMonth.now(),
        firstDayOfWeek = firstDayOfWeekFromLocale(),
    )

    Box(
        modifier = Modifier
            .height(300.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
    ) {
        VerticalCalendar(
            modifier = Modifier.fillMaxSize(),
            state = calendarState,
            monthHeader = { month ->
                Text(
                    text = month.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            dayContent = { day ->
                val isSelected = selectedDate == day.date
                val hasEvents = eventsMap.containsKey(day.date)
                if (hasEvents) {
                    android.util.Log.d("HomeScreen", "🗓️ CALENDAR DAY - ${day.date} has events: ${eventsMap[day.date]?.map { it.title }}")
                }
                val background = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (day.position == DayPosition.MonthDate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(background)
                        .clickable { selectedDate = day.date },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                        )
                        if (hasEvents) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                }
            },
        )
    }

    VerticalSpacer(SpacingSize.Medium)
    val events = eventsMap[selectedDate] ?: emptyList()
    InfoCard(
        title = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
        content = if (events.isEmpty()) "No events scheduled for this day" else "${events.size} event(s)",
        icon = Icons.Default.Event,
    )
    if (events.isNotEmpty()) {
        VerticalSpacer(SpacingSize.Small)
        val eventScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .heightIn(max = 200.dp)
                .verticalScroll(eventScroll),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            events.forEach {
                InfoCard(title = it.title, content = it.time, icon = Icons.Default.Event)
            }
        }
    }
}

/**
 * Formats event time display showing start and end times, or fallback to "All Day"
 */
private fun formatEventTime(startTime: String?, endTime: String?): String {
    return when {
        startTime != null && endTime != null && startTime.isNotBlank() && endTime.isNotBlank() -> {
            "$startTime - $endTime"
        }
        startTime != null && startTime.isNotBlank() -> {
            startTime
        }
        else -> "All Day"
    }
}
