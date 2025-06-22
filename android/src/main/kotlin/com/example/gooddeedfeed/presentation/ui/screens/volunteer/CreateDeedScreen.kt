package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.presentation.ui.components.FormTextField
import com.example.gooddeedfeed.presentation.ui.components.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.ScreenContainer
import com.example.gooddeedfeed.presentation.ui.components.SpacingSize
import com.example.gooddeedfeed.presentation.ui.components.VerticalSpacer

@Composable
fun CreateDeedScreen(
    user: DomainUser,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    ScreenContainer {
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
            onClick = {},
            enabled = title.isNotBlank() && description.isNotBlank()
        )
    }
} 
