package com.example.gooddeedfeed.presentation.ui.screens.organizer

import android.app.Activity
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gooddeedfeed.domain.model.CreateEventData
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.presentation.ui.components.ImageUtils
import com.example.gooddeedfeed.presentation.ui.components.base.PrimaryButton
import com.example.gooddeedfeed.presentation.ui.theme.CornerRadius
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
import java.text.SimpleDateFormat
import java.util.Calendar
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
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var maxVolunteersText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(OpportunityCategory.OTHER) }
    var karmaPoints by remember { mutableStateOf(10) }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }
    var selectedImageFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var mainImageIndex by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Date and time picker states
    val datePickerState = rememberDatePickerState()
    val startTimePickerState = rememberTimePickerState()
    val endTimePickerState = rememberTimePickerState()

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

    // Launcher for multiple gallery images
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            ImageUtils.saveUriToFile(context, it)?.let { file -> 
                if (selectedImageFiles.size < 10) {
                    selectedImageFiles = selectedImageFiles + file
                }
            }
        }
    }

    // --- Location autocomplete state & logic ---
    val placesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    var locationDropdownExpanded by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    // Debounce location query and fetch predictions
    LaunchedEffect(locationText) {
        if (locationText.length >= 2) { // Reduced from 3 to 2 characters
            delay(300)
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(sessionToken)
                    .setQuery(locationText)
                    .build()
                val response = placesClient.findAutocompletePredictions(request).await()
                predictions = response.autocompletePredictions
                locationDropdownExpanded = predictions.isNotEmpty()
            } catch (e: Exception) {
                predictions = emptyList()
                locationDropdownExpanded = false
                // For debugging: you can add Log.e("PlacesAPI", "Error: ${e.message}")
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
                .padding(16.dp)
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title row (aligned with Manage Events style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Location input with autocomplete and map button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                ExposedDropdownMenuBox(
                    expanded = locationDropdownExpanded,
                    onExpandedChange = { expanded ->
                        // Only allow manual dismissal, not opening
                        if (!expanded) {
                            locationDropdownExpanded = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        label = { Text("Location") },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Select on Map",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Image carousel picker
            EventImageCarousel(
                selectedImages = selectedImageFiles,
                mainImageIndex = mainImageIndex,
                onAddImage = { 
                    if (selectedImageFiles.size < 10) {
                        imagePickerLauncher.launch("image/*")
                    }
                },
                onRemoveImage = { index ->
                    selectedImageFiles = selectedImageFiles.filterIndexed { i, _ -> i != index }
                    if (mainImageIndex >= selectedImageFiles.size && selectedImageFiles.isNotEmpty()) {
                        mainImageIndex = selectedImageFiles.size - 1
                    } else if (selectedImageFiles.isEmpty()) {
                        mainImageIndex = 0
                    }
                },
                onSetMainImage = { index ->
                    mainImageIndex = index
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = date,
                onValueChange = { },
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            showDatePicker = true
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { },
                    label = { Text("Start Time") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartTimePicker = true }
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                showStartTimePicker = true
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showStartTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Select Start Time")
                        }
                    },
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { },
                    label = { Text("End Time") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEndTimePicker = true }
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                showEndTimePicker = true
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showEndTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Select End Time")
                        }
                    },
                )
            }

            OutlinedTextField(
                value = maxVolunteersText,
                onValueChange = { input ->
                    val digits = input.filter { c -> c.isDigit() }
                    val number = digits.toIntOrNull()
                    maxVolunteersText = when {
                        digits.isEmpty() -> ""
                        number == null -> maxVolunteersText
                        number == 0 -> "1"
                        number > 100 -> "100"
                        else -> digits
                    }
                },
                label = { Text("Max Volunteers (1-100)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Category dropdown
            var expanded by remember { mutableStateOf(false) }
            val displayName: (OpportunityCategory) -> String = { cat ->
                cat.name.split("_").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { ch -> ch.titlecase() }
                }
            }

            val getCategoryIcon: (OpportunityCategory) -> androidx.compose.ui.graphics.vector.ImageVector = { cat ->
                when (cat) {
                    OpportunityCategory.COMMUNITY_SERVICE -> Icons.Default.Group
                    OpportunityCategory.EDUCATION -> Icons.Default.Book
                    OpportunityCategory.ENVIRONMENTAL -> Icons.Default.Nature
                    OpportunityCategory.HEALTHCARE -> Icons.Default.LocalHospital
                    OpportunityCategory.SOCIAL_SERVICES -> Icons.Default.VolunteerActivism
                    OpportunityCategory.DISASTER_RELIEF -> Icons.Default.Emergency
                    OpportunityCategory.FOOD_SECURITY -> Icons.Default.Fastfood
                    OpportunityCategory.OTHER -> Icons.Default.MoreHoriz
                }
            }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = displayName(category),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    OpportunityCategory.values().forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(displayName(cat))
                                }
                            },
                            onClick = {
                                category = cat
                                expanded = false
                            },
                        )
                    }
                }
            }

            // Karma Points Slider
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = "Karma Points: $karmaPoints",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Slider(
                    value = karmaPoints.toFloat(),
                    onValueChange = { karmaPoints = (it.toInt() / 10) * 10 }, // Round to nearest 10
                    valueRange = 10f..200f,
                    steps = 18, // (200-10)/10 - 1 = 18 steps
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Volunteers will earn $karmaPoints karma points for completing this event",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
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
                            karmaPoints = karmaPoints,
                        )
                        val created = viewModel.createEvent(data)

                        // Upload images if selected
                        selectedImageFiles.forEachIndexed { index, file ->
                            val isMain = index == mainImageIndex
                            viewModel.uploadEventImageToCarousel(created.id, file, isMain)
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

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                            date = formatter.format(millis)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(CornerRadius.medium),
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(CornerRadius.medium),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("Cancel")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurface,
                    subheadContentColor = MaterialTheme.colorScheme.onSurface,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = MaterialTheme.colorScheme.primary,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }

    // Start Time Picker Dialog
    if (showStartTimePicker) {
        CustomTimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                startTime = formatter.format(calendar.time)
                showStartTimePicker = false
            },
            timePickerState = startTimePickerState,
        )
    }

    // End Time Picker Dialog
    if (showEndTimePicker) {
        CustomTimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                endTime = formatter.format(calendar.time)
                showEndTimePicker = false
            },
            timePickerState = endTimePickerState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    timePickerState: TimePickerState,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(CornerRadius.medium),
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(CornerRadius.medium),
            ) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectorColor = MaterialTheme.colorScheme.primary,
                    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    )
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

@Composable
private fun EventImageCarousel(
    selectedImages: List<File>,
    mainImageIndex: Int,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSetMainImage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        if (selectedImages.isEmpty()) {
            // Empty state - entire tile clickable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAddImage() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Image",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Add Event Images",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to select up to 10 images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Show carousel with images
            Column(modifier = Modifier.fillMaxSize()) {
                // Main image display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    AsyncImage(
                        model = selectedImages[mainImageIndex],
                        contentDescription = "Main Event Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Main image badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "MAIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Remove button
                    IconButton(
                        onClick = { onRemoveImage(mainImageIndex) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                CircleShape
                            )
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Thumbnail row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetMainImage(index) }
                                .then(
                                    if (index == mainImageIndex) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(8.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            AsyncImage(
                                model = selectedImages[index],
                                contentDescription = "Event Image ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    // Add more button
                    if (selectedImages.size < 10) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAddImage() }
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add More Images",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
