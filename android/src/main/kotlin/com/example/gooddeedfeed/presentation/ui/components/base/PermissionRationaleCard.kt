package com.example.gooddeedfeed.presentation.ui.components.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Simple reusable card prompting the user to grant a given permission (default: location).
 * Pass the callback that triggers the permission launcher.
 */
@Composable
fun PermissionRationaleCard(
    modifier: Modifier = Modifier,
    title: String = "Location permission required",
    message: String = "Please grant location permission to show nearby items.",
    buttonText: String = "Grant permission",
    onRequestPermission: () -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.size(16.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.size(16.dp))
            Button(onClick = onRequestPermission) { Text(buttonText) }
        }
    }
} 
