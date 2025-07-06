package com.example.gooddeedfeed.presentation.ui.screens

import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.presentation.ui.components.ToastUtils
import com.example.gooddeedfeed.presentation.ui.components.base.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.theme.AppConstants
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.io.FileOutputStream

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        // Header with icon and title (copied from ChatScreen)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp),
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
            AppConstants.MOCK_LEADERS.forEachIndexed { index, pair ->
                Text(
                    text = "${index + 1}. ${pair.first} – ${pair.second} pts",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Section: Badges
        SectionCard(title = "Badges") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(AppConstants.BADGES) { badge ->
                    BadgeCard(badge)
                }
            }
        }

        // Section: Subscriptions
        SectionCard(title = "Your Subscriptions") {
            val subscriptions = listOf("Green Earth Org", "TeachTech", "Food For All")
            subscriptions.forEach { name ->
                Text(
                    text = "• $name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Section: Volunteer History
        SectionCard(title = "Volunteer History (Institution: Waterloo U.)", showExport = true) {
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BadgeCard(badge: AppConstants.Badge) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .height(120.dp)
            .clip(MaterialTheme.shapes.medium),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp),
        ) {
            Icon(
                imageVector = badge.icon,
                contentDescription = badge.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp),
            )
            VerticalSpacer()
            Text(
                text = badge.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${badge.requiredPoints} pts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalSpacer()
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
