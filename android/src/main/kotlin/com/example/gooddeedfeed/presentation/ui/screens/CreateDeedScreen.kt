package com.example.gooddeedfeed.presentation.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.presentation.ui.components.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.ScreenTitle
import com.example.gooddeedfeed.presentation.ui.components.FormTextField
import com.example.gooddeedfeed.presentation.ui.components.VerticalSpacer
import com.example.gooddeedfeed.presentation.ui.components.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.SpacingSize

@Composable
fun CreateDeedScreen(
    user: User,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    ScreenContainer {
        ScreenTitle("Create a Good Deed")

        VerticalSpacer()

        FormTextField(
            value = title,
            onValueChange = { title = it },
            label = "Title"
        )

        VerticalSpacer()

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        VerticalSpacer(SpacingSize.Large)

        PrimaryButton(
            text = "Create",
            onClick = { /* TODO: Implement deed creation */ },
            enabled = title.isNotBlank() && description.isNotBlank()
        )
    }
} 
