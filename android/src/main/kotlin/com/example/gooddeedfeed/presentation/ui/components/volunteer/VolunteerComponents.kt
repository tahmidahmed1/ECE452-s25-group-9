package com.example.gooddeedfeed.presentation.ui.components.volunteer

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity

/**
 * Card component for displaying volunteer opportunities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunityCard(
    opportunity: VolunteerOpportunity,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { /* Navigate to details */ }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = opportunity.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = opportunity.organizationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OpportunityDetailRow(
                icon = Icons.Default.LocationOn,
                text = opportunity.location
            )
            
            OpportunityDetailRow(
                icon = Icons.Default.DateRange,
                text = opportunity.date
            )
            
            OpportunityDetailRow(
                icon = Icons.Default.Person,
                text = "${opportunity.currentVolunteers}/${opportunity.requiredVolunteers} volunteers"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = opportunity.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryChip(category = opportunity.category)
                
                Button(
                    onClick = onJoinClick,
                    enabled = opportunity.currentVolunteers < opportunity.requiredVolunteers
                ) {
                    Text("Join")
                }
            }
        }
    }
}

@Composable
private fun OpportunityDetailRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryChip(
    category: OpportunityCategory,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = category.name.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() }
            )
        },
        modifier = modifier
    )
}

/**
 * Lazy column for displaying list of opportunities
 */
@Composable
fun OpportunitiesList(
    opportunities: List<VolunteerOpportunity>,
    onJoinOpportunity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(opportunities) { opportunity ->
            OpportunityCard(
                opportunity = opportunity,
                onJoinClick = { onJoinOpportunity(opportunity.id) }
            )
        }
    }
} 