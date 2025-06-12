package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.presentation.theme.CORNER_RADIUS
import com.example.gooddeedfeed.presentation.theme.GLASS_BACKGROUND
import com.example.gooddeedfeed.presentation.theme.GLASS_OVERLAY
import com.example.gooddeedfeed.presentation.theme.PADDING_LARGE
import com.example.gooddeedfeed.presentation.theme.PADDING_MEDIUM
import com.example.gooddeedfeed.presentation.theme.TEXT_ON_GLASS

@Composable
fun profileScreen(
    user: User,
    onLogout: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GLASS_OVERLAY),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(PADDING_MEDIUM)
                .background(
                    color = GLASS_BACKGROUND,
                    shape = RoundedCornerShape(CORNER_RADIUS),
                )
                .padding(PADDING_LARGE),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = TEXT_ON_GLASS,
            )
            Spacer(Modifier.height(PADDING_MEDIUM))
            Text(
                "Username: ${user.username}",
                style = MaterialTheme.typography.bodyLarge,
                color = TEXT_ON_GLASS,
            )
            Spacer(Modifier.height(PADDING_MEDIUM))
            Text(
                "Email: ${user.email ?: "Not provided"}",
                style = MaterialTheme.typography.bodyLarge,
                color = TEXT_ON_GLASS,
            )
            Spacer(Modifier.height(PADDING_LARGE))
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Log Out")
            }
        }
    }
} 
