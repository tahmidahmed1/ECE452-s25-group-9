package com.example.gooddeedfeed.presentation.ui.components.institution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.ActivityReview
import com.example.gooddeedfeed.domain.model.ReviewStatus

/**
 * Card component for displaying activity reviews
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCard(
    review: ActivityReview,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRequestMoreInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = review.eventTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )

                ReviewStatusChip(status = review.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.organizationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ReviewDetailRow(
                icon = Icons.Default.Person,
                text = review.volunteerName,
            )

            ReviewDetailRow(
                icon = Icons.Default.DateRange,
                text = review.completionDate,
            )

            ReviewDetailRow(
                icon = Icons.Default.Schedule,
                text = "${review.hoursCompleted} hours completed",
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
            )

            // Only show action buttons for pending reviews
            if (review.status == ReviewStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Approve",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reject",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            // Show reviewer notes for completed reviews
            if (review.reviewerNotes != null && review.status != ReviewStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = "Review Notes:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = review.reviewerNotes,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewDetailRow(
    icon: ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ReviewStatusChip(
    status: ReviewStatus,
    modifier: Modifier = Modifier,
) {
    val (color, text) = when (status) {
        ReviewStatus.PENDING -> MaterialTheme.colorScheme.outline to "Pending"
        ReviewStatus.APPROVED -> MaterialTheme.colorScheme.primary to "Approved"
        ReviewStatus.REJECTED -> MaterialTheme.colorScheme.error to "Rejected"
        ReviewStatus.REQUIRES_MORE_INFO -> MaterialTheme.colorScheme.secondary to "More Info Needed"
    }

    AssistChip(
        onClick = { },
        label = { Text(text) },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color,
        ),
    )
}

/**
 * Lazy column for displaying list of reviews
 */
@Composable
fun ReviewsList(
    reviews: List<ActivityReview>,
    onApproveReview: (Int) -> Unit,
    onRejectReview: (Int) -> Unit,
    onRequestMoreInfo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(reviews) { review ->
            ReviewCard(
                review = review,
                onApprove = { onApproveReview(review.id) },
                onReject = { onRejectReview(review.id) },
                onRequestMoreInfo = { onRequestMoreInfo(review.id) },
            )
        }
    }
} 
