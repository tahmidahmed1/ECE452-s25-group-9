package com.example.gooddeedfeed.presentation.ui.screens.organizer

import android.app.Activity
import android.location.Geocoder
import android.location.Location
import android.util.Log
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
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.gooddeedfeed.data.remote.dto.EventImageDto
import com.example.gooddeedfeed.domain.model.CreateEventData
import com.example.gooddeedfeed.domain.model.OpportunityCategory
import com.example.gooddeedfeed.domain.model.VolunteerEvent
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
import com.google.maps.android.compose.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.CircularProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    viewModel: EventManagementViewModel = hiltViewModel<EventManagementViewModel>(),
    currentLocation: Location? = null,
    eventToEdit: VolunteerEvent? = null,
) {
    val isEditing = eventToEdit != null
    var title by remember { mutableStateOf(eventToEdit?.title ?: "") }
    var description by remember { mutableStateOf(eventToEdit?.description ?: "") }
    var locationText by remember { mutableStateOf(eventToEdit?.location ?: "") }
    var date by remember { mutableStateOf(eventToEdit?.date ?: "") }
    fun formatExistingTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            SimpleDateFormat("h:mm a", Locale.getDefault()).parse(raw)
            raw // parsed successfully, already correct format
        } catch (_: Exception) {
            try {
                val parsed = SimpleDateFormat("H:mm", Locale.getDefault()).parse(raw)
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed!!)
            } catch (_: Exception) {
                raw // fallback – leave as-is
            }
        }
    }

    var startTime by remember { mutableStateOf(formatExistingTime(eventToEdit?.startTime)) }
    var endTime by remember { mutableStateOf(formatExistingTime(eventToEdit?.endTime)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var maxVolunteersText by remember { mutableStateOf(eventToEdit?.maxVolunteers?.toString() ?: "") }
    var category by remember { mutableStateOf(eventToEdit?.category ?: OpportunityCategory.OTHER) }
    var karmaPoints by remember { mutableStateOf(eventToEdit?.karmaPoints ?: 10) }
    var latitudeText by remember { mutableStateOf(eventToEdit?.latitude?.toString() ?: "") }
    var longitudeText by remember { mutableStateOf(eventToEdit?.longitude?.toString() ?: "") }
    var showMapPicker by remember { mutableStateOf(false) }
    var existingImages by remember { mutableStateOf<List<EventImageDto>>(eventToEdit?.images ?: emptyList()) }
    var newImageFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    val allImages: List<Any> = remember(existingImages, newImageFiles) {
        existingImages.map { it.image_url } + newImageFiles
    }

    var mainImageIndex by remember {
        mutableStateOf(
            existingImages.indexOfFirst { it.is_main }.takeIf { it >= 0 } ?: 0,
        )
    }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    fun parseEventDateTime(dateStr: String, timeStr: String): Date? {
        return try {
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val parsedDate = dateFormat.parse(dateStr)
            val parsedTime = timeFormat.parse(timeStr)

            if (parsedDate != null && parsedTime != null) {
                val calendar = Calendar.getInstance()
                calendar.time = parsedDate

                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = parsedTime

                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                calendar.time
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isEventDateTimeValid(dateStr: String, timeStr: String): Boolean {
        if (dateStr.isBlank() || timeStr.isBlank()) return false

        val eventDateTime = parseEventDateTime(dateStr, timeStr) ?: return false
        
        // Always allow any valid date/time - no minimum time restriction
        return true
    }

    fun isEndTimeAfterStartTime(dateStr: String, startTimeStr: String, endTimeStr: String): Boolean {
        if (dateStr.isBlank() || startTimeStr.isBlank() || endTimeStr.isBlank()) return false

        val startDateTime = parseEventDateTime(dateStr, startTimeStr) ?: return false
        val endDateTime = parseEventDateTime(dateStr, endTimeStr) ?: return false

        return endDateTime.after(startDateTime)
    }

    val isStartDateTimeValid = isEventDateTimeValid(date, startTime)
    val isEndTimeValid = isEndTimeAfterStartTime(date, startTime, endTime)

    val isLocationSelected = latitudeText.isNotBlank() && longitudeText.isNotBlank()

    val isFormValid = title.isNotBlank() &&
        description.isNotBlank() &&
        isLocationSelected &&
        date.isNotBlank() &&
        startTime.isNotBlank() &&
        endTime.isNotBlank() &&
        maxVolunteersText.isNotBlank() &&
        (maxVolunteersText.toIntOrNull() ?: 0) > 0 &&
        (!isEditing || eventToEdit == null || (maxVolunteersText.toIntOrNull() ?: 0) >= eventToEdit.currentVolunteers) &&
        isStartDateTimeValid &&
        isEndTimeValid

    val titleError = hasAttemptedSubmit && title.isBlank()
    val descriptionError = hasAttemptedSubmit && description.isBlank()
    val locationError = hasAttemptedSubmit && !isLocationSelected
    val dateError = hasAttemptedSubmit && date.isBlank()
    val startTimeError = hasAttemptedSubmit && (startTime.isBlank() || (date.isNotBlank() && startTime.isNotBlank() && !isStartDateTimeValid))
    val endTimeError = hasAttemptedSubmit && (endTime.isBlank() || (date.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank() && !isEndTimeValid))
    val maxVolunteersError = hasAttemptedSubmit && (maxVolunteersText.isBlank() || (maxVolunteersText.toIntOrNull() ?: 0) <= 0 || (isEditing && eventToEdit != null && (maxVolunteersText.toIntOrNull() ?: 0) < eventToEdit.currentVolunteers))

    val formErrorMessage: String? = when {
        title.isBlank() -> "Title is required"
        description.isBlank() -> "Description is required"
        !isLocationSelected -> "Select a location from the dropdown or map"
        date.isBlank() -> "Date is required"
        startTime.isBlank() -> "Start time is required"
        !isStartDateTimeValid -> "Invalid start time"
        endTime.isBlank() -> "End time is required"
        !isEndTimeValid -> "End time must be after start time"
        maxVolunteersText.isBlank() || (maxVolunteersText.toIntOrNull() ?: 0) <= 0 -> "Enter a valid max volunteers (> 0)"
        isEditing && eventToEdit != null && (maxVolunteersText.toIntOrNull() ?: 0) < eventToEdit.currentVolunteers -> "Max volunteers cannot be less than current volunteers (${eventToEdit.currentVolunteers})"
        else -> null
    }

    val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
        .atStartOfDay(java.time.ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= today
            }
        },
    )
    val startTimePickerState = rememberTimePickerState()
    val endTimePickerState = rememberTimePickerState()

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

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            ImageUtils.saveUriToFile(context, it)?.let { file ->
                if (existingImages.size + newImageFiles.size < 10) {
                    newImageFiles = newImageFiles + file
                }
            }
        }
    }

    val placesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    var locationDropdownExpanded by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    LaunchedEffect(locationText) {
        if (locationText.length >= 2) { // Reduced from 3 to 2 characters
            delay(500) // 500 ms debounce to limit API calls during typing
            try {
                Log.d("CreateEventScreen", "🔍 Querying Places API for: $locationText")
                val request = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(sessionToken)
                    .setQuery(locationText)
                    .build()
                val response = placesClient.findAutocompletePredictions(request).await()
                predictions = response.autocompletePredictions.take(3)
                Log.d("CreateEventScreen", "✅ Received ${predictions.size} predictions")
                locationDropdownExpanded = predictions.isNotEmpty()
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                Log.e("CreateEventScreen", "❌ Places API error", e)
                predictions = emptyList()
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
                        text = if (isEditing) "Edit Event" else "Create Event",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = titleError,
                supportingText = if (titleError) { { Text("Title is required", color = MaterialTheme.colorScheme.error) } } else null,
            )

            // Suggestion Button
            val descriptionSuggestion by viewModel.descriptionSuggestion.collectAsState()
            val isGeneratingDescription by viewModel.isGeneratingDescription.collectAsState()

            LaunchedEffect(descriptionSuggestion) {
                descriptionSuggestion?.let { generated ->
                    if (generated.isNotBlank()) {
                        description = generated
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { viewModel.generateDescriptionSuggestion(title) },
                    enabled = title.isNotBlank() && !isGeneratingDescription,
                ) {
                    if (isGeneratingDescription) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "Suggest Description")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suggest Description")
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = descriptionError,
                supportingText = if (descriptionError) { { Text("Description is required", color = MaterialTheme.colorScheme.error) } } else null,
                minLines = 3,
                maxLines = 5,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                ExposedDropdownMenuBox(
                    expanded = locationDropdownExpanded,
                    onExpandedChange = { expanded ->
                        if (!expanded) {
                            locationDropdownExpanded = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        label = { Text("Location *") },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = locationError,
                        supportingText = if (locationError) { { Text("Location is required", color = MaterialTheme.colorScheme.error) } } else null,
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
                                                Log.d("CreateEventScreen", "📍 Selected place lat=${it.latitude}, lon=${it.longitude}")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("CreateEventScreen", "❌ Failed to fetch place details", e)
                                        }
                                    }
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showMapPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Select on Map",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            EventImageCarousel(
                selectedImages = allImages,
                mainImageIndex = mainImageIndex,
                onAddImage = {
                    if (allImages.size < 10) {
                        imagePickerLauncher.launch("image/*")
                    }
                },
                onRemoveImage = { index ->
                    if (index < existingImages.size) {
                        existingImages = existingImages.filterIndexed { i, _ -> i != index }
                    } else {
                        val localIndex = index - existingImages.size
                        newImageFiles = newImageFiles.filterIndexed { i, _ -> i != localIndex }
                    }

                    val newSize = existingImages.size + newImageFiles.size
                    if (mainImageIndex >= newSize && newSize > 0) {
                        mainImageIndex = newSize - 1
                    } else if (newSize == 0) {
                        mainImageIndex = 0
                    }
                },
                onSetMainImage = { index ->
                    mainImageIndex = index
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = date,
                onValueChange = { },
                label = { Text("Date *") },
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
                isError = dateError,
                supportingText = if (dateError) { { Text("Date is required", color = MaterialTheme.colorScheme.error) } } else null,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { },
                    label = { Text("Start Time *") },
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
                    isError = startTimeError,
                    supportingText = if (startTimeError) {
                        {
                            Text(
                                text = when {
                                    startTime.isBlank() -> "Start time is required"
                                    !isStartDateTimeValid -> "Invalid start time"
                                    else -> "Invalid start time"
                                },
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        null
                    },
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { },
                    label = { Text("End Time *") },
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
                    isError = endTimeError,
                    supportingText = if (endTimeError) {
                        {
                            Text(
                                text = when {
                                    endTime.isBlank() -> "End time is required"
                                    !isEndTimeValid -> "End time must be after start time"
                                    else -> "Invalid end time"
                                },
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        null
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
                label = { Text("Max Volunteers (1-100) *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = maxVolunteersError,
                supportingText = if (maxVolunteersError) { 
                    { 
                        Text(
                            text = when {
                                maxVolunteersText.isBlank() -> "Max volunteers is required (1-100)"
                                (maxVolunteersText.toIntOrNull() ?: 0) <= 0 -> "Max volunteers must be greater than 0"
                                isEditing && eventToEdit != null && (maxVolunteersText.toIntOrNull() ?: 0) < eventToEdit.currentVolunteers -> "Cannot be less than current volunteers (${eventToEdit.currentVolunteers})"
                                else -> "Max volunteers is required (1-100)"
                            },
                            color = MaterialTheme.colorScheme.error
                        ) 
                    } 
                } else null,
            )

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
                    OpportunityCategory.ANIMAL_WELFARE -> Icons.Default.Pets
                    OpportunityCategory.ARTS_CULTURE -> Icons.Default.Palette
                    OpportunityCategory.YOUTH_MENTORING -> Icons.Default.School
                    OpportunityCategory.ELDERLY_CARE -> Icons.Default.SentimentSatisfied
                    OpportunityCategory.TECHNOLOGY -> Icons.Default.Computer
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

            errorMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { errorMessage = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            formErrorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }

            PrimaryButton(
                text = if (isSubmitting) {
                    if (isEditing) "Updating..." else "Creating..."
                } else {
                    if (isEditing) "Update Event" else "Create Event"
                },
                onClick = {
                    if (isFormValid) {
                        scope.launch {
                            try {
                                isSubmitting = true
                                errorMessage = null

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
                                if (isEditing && eventToEdit != null) {
                                    viewModel.updateEvent(eventToEdit.id, data)

                                    newImageFiles.forEachIndexed { nIdx, file ->
                                        val absoluteIndex = existingImages.size + nIdx
                                        val isMain = absoluteIndex == mainImageIndex
                                        viewModel.uploadEventImageToCarousel(eventToEdit.id, file, isMain)
                                    }

                                    if (mainImageIndex < existingImages.size) {
                                        val chosenRemote = existingImages[mainImageIndex]
                                        if (!chosenRemote.is_main) {
                                            viewModel.setMainEventImage(eventToEdit.id, chosenRemote.id)
                                        }
                                    }
                                } else {
                                    val created = viewModel.createEvent(data)

                                    newImageFiles.forEachIndexed { nIdx, file ->
                                        val isMain = nIdx == mainImageIndex // existingImages empty in create mode
                                        viewModel.uploadEventImageToCarousel(created.id, file, isMain)
                                    }
                                }

                                onBack()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: if (isEditing) "Failed to update event. Please try again." else "Failed to create event. Please try again."
                            } finally {
                                isSubmitting = false
                            }
                        }
                    } else {
                        hasAttemptedSubmit = true
                    }
                },
                enabled = isFormValid && !isSubmitting,
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val utcDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
                            date = utcDate.format(formatter)
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
    selectedImages: List<Any>,
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        if (selectedImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAddImage() },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Image",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Add Event Images",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to select up to 10 images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(8.dp),
                ) {
                    AsyncImage(
                        model = selectedImages[mainImageIndex],
                        contentDescription = "Main Event Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "MAIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    IconButton(
                        onClick = { onRemoveImage(mainImageIndex) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                CircleShape,
                            )
                            .size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                            RoundedCornerShape(8.dp),
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            AsyncImage(
                                model = selectedImages[index],
                                contentDescription = "Event Image ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }

                    if (selectedImages.size < 10) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAddImage() }
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add More Images",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
