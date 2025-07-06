package com.example.gooddeedfeed.presentation.ui.screens.volunteer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.presentation.ui.components.base.FormTextField
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.components.base.ScreenContainer

@Composable
fun CreateDeedScreen(user: DomainUser) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Optimization 8: Use derivedStateOf for computed values to reduce recompositions
    val isButtonEnabled by remember {
        derivedStateOf { title.isNotBlank() && description.isNotBlank() }
    }

    ScreenContainer {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            FormTextField(
                value = title,
                onValueChange = { title = it },
                label = "Good Deed Title",
            )

            Spacer(modifier = Modifier.height(16.dp))

            FormTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Create",
                onClick = {},
                enabled = isButtonEnabled,
            )
        }
    }
} 
