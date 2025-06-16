package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.domain.model.ActivityReview
import com.example.gooddeedfeed.domain.model.ReviewStatus
import com.example.gooddeedfeed.presentation.viewmodel.ReviewViewModel
import com.example.gooddeedfeed.presentation.viewmodel.ReviewUiState
import com.example.gooddeedfeed.presentation.viewmodel.ReviewTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    user: User,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Review",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Activity Reviews",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        when (uiState) {
            is ReviewUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ReviewUiState.Success -> {
                val successState = uiState as ReviewUiState.Success // Explicit cast
                
                // Filter tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    FilterChip(
                        selected = successState.selectedTab == ReviewTab.PENDING,
                        onClick = { viewModel.selectTab(ReviewTab.PENDING) },
                        label = { Text("Pending (${successState.pendingReviews.size})") }
                    )
                    FilterChip(
                        selected = successState.selectedTab == ReviewTab.COMPLETED,
                        onClick = { viewModel.selectTab(ReviewTab.COMPLETED) },
                        label = { Text("Completed (${successState.completedReviews.size})") }
                    )
                }

                val currentReviews = when (successState.selectedTab) {
                    ReviewTab.PENDING -> successState.pendingReviews
                    ReviewTab.COMPLETED -> successState.completedReviews
                }

                if (currentReviews.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (successState.selectedTab) {
                                    ReviewTab.PENDING -> Icons.Default.DateRange
                                    ReviewTab.COMPLETED -> Icons.Default.CheckCircle
                                },
                                contentDescription = "No Activities",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No ${successState.selectedTab.name.lowercase()} reviews",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentReviews) { review ->
                            ReviewCard(
                                review = review,
                                onApprove = { viewModel.approveReview(review.id, null) },
                                onReject = { viewModel.rejectReview(review.id, null) },
                                onRequestMoreInfo = { viewModel.requestMoreInfo(review.id, null) }
                            )
                        }
                    }
                }
            }
            is ReviewUiState.Error -> {
                val errorState = uiState as ReviewUiState.Error // Explicit cast
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = errorState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadReviews() }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: ActivityReview,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRequestMoreInfo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = review.activityTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                StatusChip(status = review.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Volunteer: ${review.volunteerName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Organization: ${review.organizationName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = review.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Date: ${review.dateCompleted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Hours: ${review.hoursCompleted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (review.status == ReviewStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Approve",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }
                    
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reject",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }
                }
            }
            
            // Show review notes if available
            review.notes?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: $notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: ReviewStatus) {
    val (color, icon) = when (status) {
        ReviewStatus.PENDING -> MaterialTheme.colorScheme.secondary to Icons.Default.DateRange
        ReviewStatus.APPROVED -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
        ReviewStatus.REJECTED -> MaterialTheme.colorScheme.error to Icons.Default.Close
        ReviewStatus.REQUIRES_MORE_INFO -> MaterialTheme.colorScheme.tertiary to Icons.Default.DateRange
    }
    
    AssistChip(
        onClick = { },
        label = { Text(status.name) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = status.name,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
    )
}