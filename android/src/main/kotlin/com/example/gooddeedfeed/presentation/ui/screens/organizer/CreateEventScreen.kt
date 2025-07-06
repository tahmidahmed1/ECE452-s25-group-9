package com.example.gooddeedfeed.presentation.ui.screens.organizer

import android.app.Activity
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gooddeedfeed.domain.model.CreateEventData
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.viewmodel.organizer.EventManagementViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    viewModel: EventManagementViewModel = hiltViewModel<EventManagementViewModel>(),
    currentLocation: Location? = null,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var maxVolunteersText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(OpportunityCategory.OTHER) }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }
    var selectedImageFile by remember { mutableStateOf<File?>(null) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Launcher for Google Places Autocomplete
    val placeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val place = Autocomplete.getPlaceFromIntent(intent)
                locationText = place.address ?: ""
                place.latLng?.let {
                    latitudeText = it.latitude.toString()
                    longitudeText = it.longitude.toString()
                }
            }
        }
    }

    // Launcher for gallery image (simple)
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            ImageUtils.saveUriToFile(context, it)?.let { file -> selectedImageFile = file }
        }
    }

    // --- Location autocomplete state & logic ---
    val placesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    var locationDropdownExpanded by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    // Debounce location query and fetch predictions
    LaunchedEffect(locationText) {
        if (locationText.length >= 3) {
            delay(300)
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(sessionToken)
                    .setQuery(locationText)
                    .build()
                val response = placesClient.findAutocompletePredictions(request).await()
                predictions = response.autocompletePredictions
                locationDropdownExpanded = predictions.isNotEmpty()
            } catch (_: Exception) {
                predictions = emptyList()
                locationDropdownExpanded = false
            }
        } else {
            predictions = emptyList()
            locationDropdownExpanded = false
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title row (aligned with Manage Events style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Event",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
            )

            // Location input with autocomplete
            ExposedDropdownMenuBox(
                expanded = locationDropdownExpanded,
                onExpandedChange = { locationDropdownExpanded = it },
            ) {
                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text("Location") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val fields = listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(context)
                                placeLauncher.launch(intent)
                            },
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search location")
                        }
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true,
                )

                ExposedDropdownMenu(
                    expanded = locationDropdownExpanded,
                    onDismissRequest = { locationDropdownExpanded = false },
                ) {
                    predictions.forEach { prediction ->
                        DropdownMenuItem(
                            text = { Text(prediction.getFullText(null).toString()) },
                            onClick = {
                                locationText = prediction.getFullText(null).toString()
                                locationDropdownExpanded = false
                                scope.launch {
                                    try {
                                        val placeRequest = FetchPlaceRequest.builder(
                                            prediction.placeId,
                                            listOf(Place.Field.LAT_LNG),
                                        ).build()
                                        val placeResult = placesClient.fetchPlace(placeRequest).await()
                                        placeResult.place.latLng?.let {
                                            latitudeText = it.latitude.toString()
                                            longitudeText = it.longitude.toString()
                                        }
                                    } catch (_: Exception) {
                                        // Ignore errors fetching place details
                                    }
                                }
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            // Map picker button
            OutlinedButton(
                onClick = { showMapPicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Select Location on Map")
            }

            // Image picker
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (selectedImageFile == null) "Select Event Image (optional)" else "Image Selected: Tap to Change")
            }

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time (HH:MM)") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End Time (HH:MM)") },
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = maxVolunteersText,
                onValueChange = { maxVolunteersText = it.filter { c -> c.isDigit() } },
                label = { Text("Max Volunteers") },
                modifier = Modifier.fillMaxWidth(),
            )

            // Category dropdown
            var expanded by remember { mutableStateOf(false) }
            val displayName: (OpportunityCategory) -> String = { cat ->
                cat.name.split("_").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { ch -> ch.titlecase() }
                }
            }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = displayName(category),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    OpportunityCategory.values().forEach { cat ->
                        DropdownMenuItem(text = { Text(displayName(cat)) }, onClick = {
                            category = cat
                            expanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Create Event",
                onClick = {
                    scope.launch {
                        val data = CreateEventData(
                            title = title,
                            description = description,
                            location = locationText,
                            date = date,
                            startTime = startTime,
                            endTime = endTime,
                            maxVolunteers = maxVolunteersText.toIntOrNull() ?: 0,
                            category = category,
                            requirements = emptyList(),
                            latitude = latitudeText.toDoubleOrNull(),
                            longitude = longitudeText.toDoubleOrNull(),
                        )
                        val created = viewModel.createEvent(data)

                        // Upload image if selected
                        selectedImageFile?.let { file ->
                            viewModel.uploadEventImage(created.id, file)
                        }

                        onBack()
                    }
                },
                enabled = title.isNotBlank() && date.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showMapPicker) {
        MapPickerDialog(
            initialLat = latitudeText.toDoubleOrNull(),
            initialLon = longitudeText.toDoubleOrNull(),
            onLocationSelected = { lat, lon ->
                latitudeText = lat.toString()
                longitudeText = lon.toString()
                scope.launch {
                    val addr = withContext(Dispatchers.IO) {
                        try {
                            Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    locationText = addr?.firstOrNull()?.getAddressLine(0) ?: "$lat,$lon"
                }
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false },
        )
    }
}

@Composable
private fun MapPickerDialog(
    initialLat: Double?,
    initialLon: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth()) {
                val defaultLocation = LatLng(initialLat ?: 43.6532, initialLon ?: -79.3832)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
                }

                var selectedLatLng by remember { mutableStateOf<LatLng?>(if (initialLat != null && initialLon != null) defaultLocation else null) }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true),
                    onMapClick = { latLng ->
                        selectedLatLng = latLng
                    },
                ) {
                    selectedLatLng?.let { latLng ->
                        Marker(state = MarkerState(position = latLng))
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    PrimaryButton(
                        text = "Confirm",
                        onClick = {
                            selectedLatLng?.let { onLocationSelected(it.latitude, it.longitude) }
                        },
                        enabled = selectedLatLng != null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
} 
