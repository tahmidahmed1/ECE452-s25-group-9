package com.example.gooddeedfeed.presentation.ui.screens.institution

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gooddeedfeed.domain.model.ActivityReview
import com.example.gooddeedfeed.domain.model.ReviewActionType
import com.example.gooddeedfeed.presentation.common.UiState
import com.example.gooddeedfeed.presentation.viewmodel.institution.ReviewViewModel
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel<ReviewViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadReviews()
    }

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
                imageVector = Icons.Default.Star,
                contentDescription = "Review",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reviews to Approve",
                style = MaterialTheme.typography.headlineMedium,
            )
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
                val reviewData = currentState.data

                if (reviewData.pendingReviews.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "No Reviews",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No reviews pending",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "All reviews have been processed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(reviewData.pendingReviews) { review ->
                            ReviewCard(
                                review = review,
                                onAction = { action ->
                                    when (action) {
                                        ReviewActionType.APPROVE -> viewModel.approveReview(review.id, null)
                                        ReviewActionType.REJECT -> viewModel.rejectReview(review.id, null)
                                        ReviewActionType.REQUEST_MORE_INFO -> viewModel.requestMoreInfo(review.id, null)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadReviews() },
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewCard(
    review: ActivityReview,
    onAction: (ReviewActionType) -> Unit,
) {
    var showEvidenceDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header with volunteer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.volunteerName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = review.eventTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                FilterChip(
                    selected = false,
                    onClick = { },
                    label = {
                        Text(
                            text = review.status.name,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Activity details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Hours: ${review.hoursCompleted}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Date: ${review.completionDate}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (review.evidenceUrls.isNotEmpty()) {
                    AssistChip(
                        onClick = {
                            showEvidenceDialog = true
                        },
                        label = { Text("View Evidence") },
                    )
                }
            }

            if (review.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = review.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onAction(ReviewActionType.APPROVE) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Approve",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Approve",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                OutlinedButton(
                    onClick = { onAction(ReviewActionType.REJECT) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reject",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            // Mocked dialog for evidence
            if (showEvidenceDialog) {
                AlertDialog(
                    onDismissRequest = { showEvidenceDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showEvidenceDialog = false }) {
                            Text("Close")
                        }
                    },
                    title = { Text("Evidence") },
                    text = { Text("Here we would display the evidence attached to this review.") },
                )
            }
        }
    }
}
