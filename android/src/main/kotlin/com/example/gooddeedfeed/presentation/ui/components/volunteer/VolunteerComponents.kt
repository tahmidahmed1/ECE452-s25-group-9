package com.example.gooddeedfeed.presentation.ui.components.volunteer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.DateFilter
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.OpportunityFilters
import com.example.gooddeedfeed.domain.model.VolunteerOpportunity

/**
 * Card component for displaying volunteer opportunities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunityCard(
    opportunity: VolunteerOpportunity,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { /* Navigate to details */ },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = opportunity.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = opportunity.organizationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OpportunityDetailRow(
                icon = Icons.Default.LocationOn,
                text = opportunity.location,
            )

            OpportunityDetailRow(
                icon = Icons.Default.DateRange,
                text = opportunity.date,
            )

            OpportunityDetailRow(
                icon = Icons.Default.Person,
                text = "${opportunity.currentVolunteers}/${opportunity.requiredVolunteers} volunteers",
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = opportunity.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryChip(category = opportunity.category)

                Button(
                    onClick = onJoinClick,
                    enabled = opportunity.currentVolunteers < opportunity.requiredVolunteers,
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
fun CategoryChip(
    category: OpportunityCategory,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = category.name.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() },
            )
        },
        modifier = modifier,
    )
}

/**
 * Lazy column for displaying list of opportunities
 */
@Composable
fun OpportunitiesList(
    opportunities: List<VolunteerOpportunity>,
    onJoinOpportunity: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (opportunities.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "No opportunities",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No volunteer opportunities available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Check back later or adjust your filters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(opportunities) { opportunity ->
                OpportunityCard(
                    opportunity = opportunity,
                    onJoinClick = { onJoinOpportunity(opportunity.id) },
                )
            }
        }
    }
}

@Composable
fun FiltersDrawer(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    filters: OpportunityFilters,
    onFiltersChange: (OpportunityFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header with toggle button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse filters" else "Expand filters",
                    )
                }
            }

            // Filter content (only show when expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Category Filter
                    FilterSection(
                        title = "Category",
                        icon = Icons.Default.Category,
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(120.dp),
                        ) {
                            items(OpportunityCategory.values()) { category ->
                                FilterChip(
                                    selected = filters.selectedCategories.contains(category),
                                    onClick = {
                                        val newCategories = if (filters.selectedCategories.contains(category)) {
                                            filters.selectedCategories - category
                                        } else {
                                            filters.selectedCategories + category
                                        }
                                        onFiltersChange(filters.copy(selectedCategories = newCategories))
                                    },
                                    label = {
                                        Text(
                                            text = category.name.replace("_", " ").lowercase()
                                                .split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } },
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    // Available Spots Filter
                    FilterSection(
                        title = "Availability",
                        icon = Icons.Default.Group,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = filters.onlyAvailable,
                                onClick = {
                                    onFiltersChange(filters.copy(onlyAvailable = !filters.onlyAvailable))
                                },
                                label = { Text("Only Available") },
                            )
                            FilterChip(
                                selected = filters.almostFull,
                                onClick = {
                                    onFiltersChange(filters.copy(almostFull = !filters.almostFull))
                                },
                                label = { Text("Almost Full") },
                            )
                        }
                    }

                    // Karma Points Range Filter
                    FilterSection(
                        title = "Karma Points",
                        icon = Icons.Default.Star,
                    ) {
                        Column {
                            Text(
                                text = "${filters.minKarmaPoints} - ${filters.maxKarmaPoints} points",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            RangeSlider(
                                value = filters.minKarmaPoints.toFloat()..filters.maxKarmaPoints.toFloat(),
                                onValueChange = { range ->
                                    onFiltersChange(
                                        filters.copy(
                                            minKarmaPoints = range.start.toInt(),
                                            maxKarmaPoints = range.endInclusive.toInt(),
                                        ),
                                    )
                                },
                                valueRange = 1f..200f,
                                steps = 19,
                            )
                        }
                    }

                    // Date Range Filter
                    FilterSection(
                        title = "Date Range",
                        icon = Icons.Default.DateRange,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = filters.dateFilter == DateFilter.TODAY,
                                onClick = {
                                    onFiltersChange(filters.copy(dateFilter = DateFilter.TODAY))
                                },
                                label = { Text("Today") },
                            )
                            FilterChip(
                                selected = filters.dateFilter == DateFilter.THIS_WEEK,
                                onClick = {
                                    onFiltersChange(filters.copy(dateFilter = DateFilter.THIS_WEEK))
                                },
                                label = { Text("This Week") },
                            )
                            FilterChip(
                                selected = filters.dateFilter == DateFilter.THIS_MONTH,
                                onClick = {
                                    onFiltersChange(filters.copy(dateFilter = DateFilter.THIS_MONTH))
                                },
                                label = { Text("This Month") },
                            )
                        }
                    }

                    // Radius Filter
                    FilterSection(
                        title = "Distance",
                        icon = Icons.Default.LocationOn,
                    ) {
                        Column {
                            Text(
                                text = "${filters.radiusKm.toInt()} km radius",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            Slider(
                                value = filters.radiusKm,
                                onValueChange = { newRadius ->
                                    onFiltersChange(filters.copy(radiusKm = newRadius))
                                },
                                valueRange = 1f..200f,
                                steps = 19,
                            )
                        }
                    }

                    // Clear Filters Button
                    OutlinedButton(
                        onClick = {
                            onFiltersChange(OpportunityFilters())
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Filters")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
        }
        content()
    }
}
