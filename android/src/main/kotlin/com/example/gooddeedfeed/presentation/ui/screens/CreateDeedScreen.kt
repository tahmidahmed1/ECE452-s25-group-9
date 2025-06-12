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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.presentation.theme.CORNER_RADIUS
import com.example.gooddeedfeed.presentation.theme.GLASS_BACKGROUND
import com.example.gooddeedfeed.presentation.theme.GLASS_OVERLAY
import com.example.gooddeedfeed.presentation.theme.PADDING_LARGE
import com.example.gooddeedfeed.presentation.theme.PADDING_MEDIUM
import com.example.gooddeedfeed.presentation.theme.TEXT_ON_GLASS

@Composable
fun createDeedScreen(
    user: User,
) {
    val titleState = remember { mutableStateOf(TextFieldValue()) }
    val descriptionState = remember { mutableStateOf(TextFieldValue()) }

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
                "Create a Good Deed",
                style = MaterialTheme.typography.headlineMedium,
                color = TEXT_ON_GLASS,
            )

            Spacer(Modifier.height(PADDING_MEDIUM))

            OutlinedTextField(
                value = titleState.value,
                onValueChange = { titleState.value = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(PADDING_MEDIUM))

            OutlinedTextField(
                value = descriptionState.value,
                onValueChange = { descriptionState.value = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Spacer(Modifier.height(PADDING_LARGE))

            Button(
                onClick = { /* TODO: Implement deed creation */ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create")
            }
        }
    }
} 
