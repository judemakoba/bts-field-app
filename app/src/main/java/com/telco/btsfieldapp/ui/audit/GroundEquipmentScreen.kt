package com.telco.btsfieldapp.ui.audit

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.telco.btsfieldapp.BuildConfig
import com.telco.btsfieldapp.ui.components.PhotoViewerDialog
import com.telco.btsfieldapp.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SECTION_COLORS = listOf(
    SectionGreen,    // Ground
    SectionBlue,    // GPS
    SectionAmber,   // Shelter
    SectionBlue,    // Battery
    SectionPurple,  // AC
    SectionRed,     // Critical
    SectionCyan,    // Media
    SectionPink,    // Remarks
)

private val TENANT_OPTIONS = listOf("Lyca", "MTN", "UTL", "Savanna", "Other")
private val TOWER_TYPE_OPTIONS = listOf("GBT", "RTT", "RTP")
private val INDOOR_OUTDOOR_OPTIONS = listOf("Indoor", "Outdoor")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroundEquipmentScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onCapturePhoto: (String, String, String, String) -> Unit,
    onOpenGpsCapture: () -> Unit,
    navController: NavController,
    viewModel: GroundEquipmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val navBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = navBackStackEntry?.savedStateHandle

    // Handle GPS capture result returned from GpsCaptureScreen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            val lat = handle.get<String>("gps_latitude")
            val lng = handle.get<String>("gps_longitude")
            val alt = handle.get<String>("gps_altitude")
            val acc = handle.get<String>("gps_accuracy")
            val path = handle.get<String>("gps_screenshot")
            if (lat != null && lat.isNotBlank()) {
                viewModel.onLatitudeChange(lat)
                viewModel.onLongitudeChange(lng ?: "")
                viewModel.onAltitudeChange(alt ?: "")
                viewModel.onGpsAccuracyChange(acc ?: "")
                viewModel.onGpsScreenshot(path ?: "")
                handle.remove<String>("gps_latitude")
                handle.remove<String>("gps_longitude")
                handle.remove<String>("gps_altitude")
                handle.remove<String>("gps_accuracy")
                handle.remove<String>("gps_screenshot")
                snackbarHostState.showSnackbar("GPS coordinates captured ✓")
            }
        }
    }

    // Handle success
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GroundEquipmentEvent.SubmitSuccess -> onSuccess()
                is GroundEquipmentEvent.SaveDraftSuccess -> snackbarHostState.showSnackbar("Draft saved successfully")
            }
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.submitError) {
        uiState.submitError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Check for pending photo result from camera
    LaunchedEffect(uiState.siteId) {
        if (uiState.siteId.isBlank()) return@LaunchedEffect
        val prefs = context.getSharedPreferences("photo_results", Context.MODE_PRIVATE)
        // Poll for any photo result (called from camera)
        listOf(
            "site_name_plate", "site_photo", "gps_screenshot",
            "rru_photo", "cabinet_photo", "cabinet_dim_photo",
            "slab_photo", "redundant_photo", "non_active_idu_photo"
        ).forEach { key ->
            val photoKey = "${uiState.siteId}__$key"
            prefs.getString(photoKey, null)?.let { path ->
                prefs.edit().remove(photoKey).apply()
                when (key) {
                    "site_name_plate" -> viewModel.onSiteNamePlatePhoto(path)
                    "site_photo" -> viewModel.onSitePhoto(path)
                    "gps_screenshot" -> viewModel.onGpsScreenshot(path)
                    "rru_photo" -> viewModel.addRruPhoto(path)
                    "cabinet_photo" -> viewModel.addCabinetPhoto(path)
                    "cabinet_dim_photo" -> viewModel.addCabinetDimensionPhoto(path)
                    "slab_photo" -> viewModel.addSlabPhoto(path)
                    "redundant_photo" -> viewModel.addRedundantPhoto(path)
                    "non_active_idu_photo" -> viewModel.addNonActiveIduPhoto(path)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ground Equipment", fontWeight = FontWeight.Bold)
                        if (uiState.isLoadingExisting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryCoral,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isBusy = uiState.isSubmitting || uiState.isSavingDraft || uiState.isUploadingPhotos

                    OutlinedButton(
                        onClick = viewModel::saveDraft,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, PrimaryCoral),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryCoral
                        ),
                        enabled = !isBusy
                    ) {
                        if (uiState.isSavingDraft || uiState.isUploadingPhotos) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryCoral
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Draft")
                        }
                    }

                    Button(
                        onClick = viewModel::submit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCoral,
                            contentColor = Color.White
                        ),
                        enabled = !isBusy
                    ) {
                        if (uiState.isSubmitting || uiState.isUploadingPhotos) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Submit")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        // Upload progress overlay
        UploadProgressOverlay(
            title = if (uiState.isSubmitting) "Submitting Report..." else "Saving Draft...",
            isVisible = uiState.isUploadingPhotos,
            total = uiState.uploadTotal,
            current = uiState.uploadCurrent,
            statuses = uiState.uploadPhotoStatuses
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 0: Site Identification
            item {
                FormSectionCard(
                    index = 0,
                    title = "Site Identification",
                    icon = Icons.Default.Badge,
                    isExpanded = uiState.expandedSections.contains(0),
                    onToggle = { viewModel.toggleSection(0) }
                ) {
                    FormTextField(
                        value = uiState.atcId,
                        onValueChange = viewModel::onAtcIdChange,
                        label = "ATC ID",
                        hint = "e.g. KA1108",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))
                    PhotoCaptureRow(
                        label = "Site Name Plate Photo",
                        photos = uiState.siteNamePlatePhotoPath?.let { listOf(it) } ?: emptyList(),
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "site_name_plate") },
                        onRemovePhoto = { viewModel.onSiteNamePlatePhoto("") },
                        maxPhotos = 1,
                        existingPhotos = uiState.existingPhotos.filter { it.fieldName == "site_name_plate" }
                    )
                }
            }

            // Section 1: Survey Details
            item {
                FormSectionCard(
                    index = 1,
                    title = "Survey Details",
                    icon = Icons.Default.Person,
                    isExpanded = uiState.expandedSections.contains(1),
                    onToggle = { viewModel.toggleSection(1) }
                ) {
                    FormReadOnlyField(
                        label = "Survey Date",
                        value = formatDateReadable(uiState.surveyDate),
                        hint = "Auto-filled"
                    )
                    Spacer(Modifier.height(12.dp))
                    FormTextField(
                        value = uiState.technicianName,
                        onValueChange = viewModel::onTechnicianNameChange,
                        label = "Technician Name",
                        hint = "Auto-filled from your account",
                        leadingIcon = Icons.Default.Person
                    )
                    Spacer(Modifier.height(12.dp))
                    FormTextField(
                        value = uiState.technicianContacts,
                        onValueChange = viewModel::onTechnicianContactsChange,
                        label = "Technician Contacts",
                        hint = "9-digit phone number",
                        keyboardType = KeyboardType.Phone,
                        leadingIcon = Icons.Default.Phone
                    )
                    Spacer(Modifier.height(12.dp))
                    FormReadOnlyField(
                        label = "Survey Contractor",
                        value = uiState.contractorName,
                        hint = "Fixed: Innovis"
                    )
                }
            }

            // Section 2: Tower & Site Info
            item {
                FormSectionCard(
                    index = 2,
                    title = "Tower & Site Info",
                    icon = Icons.Default.Architecture,
                    isExpanded = uiState.expandedSections.contains(2),
                    onToggle = { viewModel.toggleSection(2) }
                ) {
                    // GPS Coordinates — dedicated capture screen
                    GpsCaptureCard(
                        latitude = uiState.latitude,
                        longitude = uiState.longitude,
                        accuracy = uiState.gpsAccuracy,
                        altitude = uiState.altitude,
                        screenshotPath = uiState.gpsScreenshotPath,
                        onOpenCapture = onOpenGpsCapture,
                        onRemove = { viewModel.onGpsScreenshot("") }
                    )
                    Spacer(Modifier.height(12.dp))
                    FormDropdown(
                        value = uiState.towerType,
                        onValueChange = viewModel::onTowerTypeChange,
                        label = "Tower Type",
                        options = TOWER_TYPE_OPTIONS,
                        placeholder = "Select tower type"
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormTextField(
                            value = uiState.towerHeight,
                            onValueChange = viewModel::onTowerHeightChange,
                            label = "Tower Height (m)",
                            hint = "Integer",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        FormTextField(
                            value = uiState.buildingHeight,
                            onValueChange = viewModel::onBuildingHeightChange,
                            label = "Building Height (m)",
                            hint = "Default 0",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    FormReadOnlyField(
                        label = "Total Height (m)",
                        value = "${uiState.totalHeight} m",
                        hint = "Auto-calculated"
                    )
                    Spacer(Modifier.height(12.dp))
                    FormDropdown(
                        value = uiState.siteIndoorOutdoor,
                        onValueChange = viewModel::onSiteIndoorOutdoorChange,
                        label = "Site Indoor / Outdoor",
                        options = INDOOR_OUTDOOR_OPTIONS,
                        placeholder = "Select"
                    )
                    Spacer(Modifier.height(12.dp))
                    FormTextField(
                        value = uiState.noOfTenants,
                        onValueChange = viewModel::onNoOfTenantsChange,
                        label = "No. of Tenants",
                        hint = "Integer",
                        keyboardType = KeyboardType.Number
                    )
                    if ((uiState.noOfTenants.toIntOrNull() ?: 0) > 1) {
                        Spacer(Modifier.height(12.dp))
                        FormMultiSelect(
                            label = "Names of Other Tenants",
                            options = TENANT_OPTIONS,
                            selected = uiState.otherTenants,
                            onToggle = viewModel::onOtherTenantsToggle
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    PhotoCaptureRow(
                        label = "Site Photo (Tower Overview)",
                        photos = uiState.sitePhotoPath?.let { listOf(it) } ?: emptyList(),
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "site_photo") },
                        onRemovePhoto = { viewModel.onSitePhoto("") },
                        maxPhotos = 1,
                        existingPhotos = uiState.existingPhotos.filter { it.fieldName == "site_photo" }
                    )
                }
            }

            // Section 3: Power Infrastructure
            item {
                FormSectionCard(
                    index = 3,
                    title = "Power Infrastructure",
                    icon = Icons.Default.ElectricalServices,
                    isExpanded = uiState.expandedSections.contains(3),
                    onToggle = { viewModel.toggleSection(3) }
                ) {
                    FormYesNoRow(
                        label = "Grid Power",
                        checked = uiState.hasGrid,
                        onChange = viewModel::onHasGridChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    FormYesNoRow(
                        label = "DG (Diesel Generator)",
                        checked = uiState.hasDG,
                        onChange = viewModel::onHasDGChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    FormYesNoRow(
                        label = "Solar",
                        checked = uiState.hasSolar,
                        onChange = viewModel::onHasSolarChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    FormTextField(
                        value = uiState.gridDistanceTo3Phase,
                        onValueChange = viewModel::onGridDistanceChange,
                        label = "Grid Distance to 3-Phase Power Line (m)",
                        hint = "Integer in metres",
                        keyboardType = KeyboardType.Number
                    )
                }
            }

            // Section 4: RRU & Cabinets
            item {
                FormSectionCard(
                    index = 4,
                    title = "RRU & Cabinets",
                    icon = Icons.Default.DevicesOther,
                    isExpanded = uiState.expandedSections.contains(4),
                    onToggle = { viewModel.toggleSection(4) }
                ) {
                    FormYesNoRow(
                        label = "Guard at Site",
                        checked = uiState.guardAtSite,
                        onChange = viewModel::onGuardAtSiteChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    FormTextField(
                        value = uiState.rruType,
                        onValueChange = viewModel::onRruTypeChange,
                        label = "RRU Type (Model)",
                        hint = "e.g. Huawei RRU 3908"
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = uiState.rruCount,
                        onValueChange = viewModel::onRruCountChange,
                        label = "RRU Count on Ground",
                        hint = "Integer",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))
                    PhotoCaptureRow(
                        label = "RRU Photos (max 4)",
                        photos = uiState.rruPhotoPaths,
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "rru_photo") },
                        onRemovePhoto = viewModel::removeRruPhoto,
                        maxPhotos = 4,
                        existingPhotos = uiState.existingPhotos.filter { it.fieldName.startsWith("rru_photo") }
                    )
                    Spacer(Modifier.height(12.dp))
                    FormTextField(
                        value = uiState.cabinetTypes,
                        onValueChange = viewModel::onCabinetTypesChange,
                        label = "Cabinet Types (Models)",
                        hint = "e.g. Huawei BTS Cabinet"
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = uiState.cabinetCount,
                        onValueChange = viewModel::onCabinetCountChange,
                        label = "Cabinet Count",
                        hint = "Integer",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))
                    PhotoCaptureRow(
                        label = "Cabinet Photos (max 20)",
                        photos = uiState.cabinetPhotoPaths,
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "cabinet_photo") },
                        onRemovePhoto = viewModel::removeCabinetPhoto,
                        maxPhotos = 20,
                        existingPhotos = uiState.existingPhotos.filter { it.fieldName.startsWith("cabinet_photo") }
                    )
                    Spacer(Modifier.height(12.dp))
                    FormYesNoRow(
                        label = "All Equipment Labelled",
                        checked = uiState.equipmentLabelled,
                        onChange = viewModel::onEquipmentLabelledChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    FormTextField(
                        value = uiState.cabinetComments,
                        onValueChange = viewModel::onCabinetCommentsChange,
                        label = "Cabinet Comments",
                        hint = "e.g. Redundant or Active"
                    )
                    Spacer(Modifier.height(12.dp))
                    FormTextField(
                        value = uiState.cabinetDimensionsLxW,
                        onValueChange = viewModel::onCabinetDimensionsChange,
                        label = "BTS Cabinet Dimensions (L x W x H)",
                        hint = "e.g. 0.6 x 0.4 x 1.2"
                    )
                    Spacer(Modifier.height(12.dp))
                    PhotoCaptureRow(
                        label = "Cabinet Dimension Photos (L, W, H) — max 3",
                        photos = uiState.cabinetDimensionPhotoPaths,
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "cabinet_dim") },
                        onRemovePhoto = viewModel::removeCabinetDimensionPhoto,
                        maxPhotos = 3,
                        uploadedPhotoPaths = uiState.uploadedPhotoPaths,
                        existingPhotos = uiState.existingPhotos.filter { it.fieldName.startsWith("cabinet_dim_photo") }
                    )
                    Spacer(Modifier.height(12.dp))
                    FormTextField(
                        value = uiState.activeIduTypes,
                        onValueChange = viewModel::onActiveIduTypesChange,
                        label = "Active IDU Types (Models) in Cabinet",
                        hint = "e.g. ATN910C, NE8000, CX600, RTN910"
                    )
                    Spacer(Modifier.height(12.dp))
                    FormTextField(
                        value = uiState.nonActiveIduTypes,
                        onValueChange = viewModel::onNonActiveIduTypesChange,
                        label = "Non-Active IDU Types in Cabinet",
                        hint = "e.g. ATN910C, NE8000, CX600, RTN910"
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = uiState.nonActiveIduCount,
                        onValueChange = viewModel::onNonActiveIduCountChange,
                        label = "Non-Active IDU Count",
                        hint = "Integer",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))
                    PhotoCaptureRow(
                        label = "Non-Active IDU Photos (max 5)",
                        photos = uiState.nonActiveIduPhotoPaths,
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "nonactive_idu") },
                        onRemovePhoto = viewModel::removeNonActiveIduPhoto,
                        maxPhotos = 5,
                        uploadedPhotoPaths = uiState.uploadedPhotoPaths,
                        existingPhotos = uiState.existingPhotos.filter { it.fieldName.startsWith("non_active_idu_photo") }
                    )
                }
            }

            // Section 5: Slab
            item {
                FormSectionCard(
                    index = 5,
                    title = "Slab Information",
                    icon = Icons.Default.GridOn,
                    isExpanded = uiState.expandedSections.contains(5),
                    onToggle = { viewModel.toggleSection(5) }
                ) {
                    FormTextField(
                        value = uiState.slabDimensions,
                        onValueChange = viewModel::onSlabDimensionsChange,
                        label = "Slab Dimensions (L x W in m)",
                        hint = "e.g. 1.2 x 0.8"
                    )
                    Spacer(Modifier.height(12.dp))
                    PhotoCaptureRow(
                        label = "Slab Photos (Overview + 2 Dims) — max 3",
                        photos = uiState.slabPhotoPaths,
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "slab_photo") },
                        onRemovePhoto = viewModel::removeSlabPhoto,
                        maxPhotos = 3,
                        existingPhotos = uiState.existingPhotos.filter { it.fieldName.startsWith("slab_photo") }
                    )
                }
            }

            // Section 6: Redundant Equipment
            item {
                FormSectionCard(
                    index = 6,
                    title = "Redundant Equipment",
                    icon = Icons.Default.Warning,
                    isExpanded = uiState.expandedSections.contains(6),
                    onToggle = { viewModel.toggleSection(6) }
                ) {
                    FormTextField(
                        value = uiState.redundantEquipmentCount,
                        onValueChange = viewModel::onRedundantCountChange,
                        label = "Count of Redundant Equipment",
                        hint = "Integer",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))
                    FormTextField(
                        value = uiState.redundantItemName,
                        onValueChange = viewModel::onRedundantItemNameChange,
                        label = "Redundant Item (Name & Model)",
                        hint = "e.g. Huawei RRU 3908 — redundant"
                    )
                    Spacer(Modifier.height(12.dp))
                    PhotoCaptureRow(
                        label = "Redundant Equipment Photos (max 3)",
                        photos = uiState.redundantPhotoPaths,
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "redundant_photo") },
                        onRemovePhoto = viewModel::removeRedundantPhoto,
                        maxPhotos = 3,
                        existingPhotos = uiState.existingPhotos.filter { it.fieldName.startsWith("redundant_photo") }
                    )
                }
            }

            // Section 7: Media & Remarks
            item {
                FormSectionCard(
                    index = 7,
                    title = "Media & Remarks",
                    icon = Icons.AutoMirrored.Filled.Comment,
                    isExpanded = uiState.expandedSections.contains(7),
                    onToggle = { viewModel.toggleSection(7) }
                ) {
                    Text(
                        "Is Site on Fiber (TRM Media)?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = uiState.isOnFiber == true,
                            onClick = { viewModel.onIsOnFiberChange(true) },
                            label = { Text("Yes") },
                            leadingIcon = if (uiState.isOnFiber == true) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryCoral,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = uiState.isOnFiber == false,
                            onClick = { viewModel.onIsOnFiberChange(false) },
                            label = { Text("No") },
                            leadingIcon = if (uiState.isOnFiber == false) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryCoral,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    FormTextField(
                        value = uiState.overallRemarks,
                        onValueChange = viewModel::onOverallRemarksChange,
                        label = "Overall Remarks",
                        hint = "Any additional observations...",
                        singleLine = false,
                        minLines = 3
                    )
                }
            }

            // Bottom padding for the fixed submit bar
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION CARD — DailyMe Warm Style
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormSectionCard(
    index: Int,
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sectionColor = SECTION_COLORS.getOrElse(index) { PrimaryCoral }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x40000000),
                spotColor = Color(0x20000000)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Section header with colored left bar
            Surface(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Colored left bar accent
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(sectionColor, RoundedCornerShape(2.dp))
                    )

                    Spacer(Modifier.width(12.dp))

                    // Section number badge — coral circle with white number
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(PrimaryCoral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$index",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(sectionColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = sectionColor, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = sectionColor
                    )
                }
            }

            // Section content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    content = content
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FORM FIELD COMPONENTS — DailyMe Warm Style
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(hint, color = OnSurfaceVariantLight.copy(alpha = 0.5f)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp), tint = PrimaryCoral) }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryCoral,
            unfocusedBorderColor = Color(0xFFE0D6CC),
            focusedLabelColor = PrimaryCoral,
            unfocusedLabelColor = OnSurfaceVariantLight,
            focusedTextColor = OnSurfaceLight,
            unfocusedTextColor = OnSurfaceLight,
            cursorColor = PrimaryCoral,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun FormReadOnlyField(
    label: String,
    value: String,
    hint: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text(hint, color = OnSurfaceVariantLight.copy(alpha = 0.5f)) },
        readOnly = true,
        enabled = false,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = PrimaryCoral.copy(alpha = 0.3f),
            disabledTextColor = OnSurfaceLight,
            disabledLabelColor = PrimaryCoral.copy(alpha = 0.7f),
            disabledContainerColor = BackgroundLight.copy(alpha = 0.5f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    placeholder: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text(placeholder, color = OnSurfaceVariantLight.copy(alpha = 0.5f)) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryCoral,
                unfocusedBorderColor = Color(0xFFE0D6CC),
                focusedLabelColor = PrimaryCoral,
                unfocusedLabelColor = OnSurfaceVariantLight,
                focusedTextColor = OnSurfaceLight,
                unfocusedTextColor = OnSurfaceLight
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    leadingIcon = if (option == value) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryCoral) }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormYesNoRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceLight,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = checked == true,
                onClick = { onChange(true) },
                label = { Text("Yes") },
                leadingIcon = if (checked == true) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SuccessColor,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = checked == false,
                onClick = { onChange(false) },
                label = { Text("No") },
                leadingIcon = if (checked == false) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ErrorColor,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FormMultiSelect(
    label: String,
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selected.contains(option)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(option) },
                    label = { Text(option) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryCoral,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PHOTO CAPTURE ROW — DailyMe Warm Style
// ─────────────────────────────────────────────────────────────────────────────

/** Build full URL for a server-side photo path (e.g. "/uploads/..."). */
private fun serverPhotoUrl(relativePath: String): String {
    val base = BuildConfig.API_BASE.removeSuffix("/api").removeSuffix("/")
    return if (relativePath.startsWith("http")) relativePath else "$base$relativePath"
}

@Composable
private fun ExistingPhotoThumbnail(
    photo: ExistingPhoto,
    onDownload: (String) -> Unit
) {
    val context = LocalContext.current
    val fullUrl = remember(photo.serverUrl) { serverPhotoUrl(photo.serverUrl) }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, SuccessColor, RoundedCornerShape(12.dp))
            .background(Color(0xFFF0FDF4)) // light green tint
            .clickable {
                // Open browser to download
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Fallback: try to download directly
                    onDownload(fullUrl)
                }
            }
    ) {
        AsyncImage(
            model = fullUrl,
            contentDescription = "Saved photo: ${photo.fieldName}",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        // "Saved" badge — green checkmark
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(20.dp)
                .background(SuccessColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Saved",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }

        // Download hint on hover-like press
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(18.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = "Download",
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
private fun PhotoCaptureRow(
    label: String,
    photos: List<String>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    maxPhotos: Int,
    uploadedPhotoPaths: Set<String> = emptySet(),
    /** Photos already saved on the server (shown as read-only thumbnails). */
    existingPhotos: List<ExistingPhoto> = emptyList()
) {
    var viewerPhoto by remember { mutableStateOf<String?>(null) }

    Box {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceLight
                )
                Text(
                    text = "${photos.size + existingPhotos.size}/$maxPhotos",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (photos.size + existingPhotos.size >= maxPhotos) PrimaryCoral else SuccessColor
                )
            }
            Spacer(Modifier.height(8.dp))

            val rowContext = LocalContext.current
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Show existing (server) photos first
                items(existingPhotos) { photo ->
                    ExistingPhotoThumbnail(
                        photo = photo,
                        onDownload = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            rowContext.startActivity(intent)
                        }
                    )
                }
                // Then local (newly captured) photos
                items(photos) { path ->
                    PhotoThumbnail(
                        path = path,
                        isUploaded = path in uploadedPhotoPaths,
                        onRemove = { onRemovePhoto(path) },
                        onView = { viewerPhoto = path }
                    )
                }
                if (photos.size < maxPhotos) {
                    item {
                        AddPhotoButton(onClick = onAddPhoto)
                    }
                }
            }
        }

        // Photo viewer dialog
        viewerPhoto?.let { path ->
            PhotoViewerDialog(
                photoPath = path,
                onDismiss = { viewerPhoto = null },
                onDelete = {
                    onRemovePhoto(path)
                    viewerPhoto = null
                }
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(
    path: String,
    isUploaded: Boolean,
    onRemove: () -> Unit,
    onView: () -> Unit
) {
    val file = remember(path) { java.io.File(path) }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.5.dp,
                color = if (isUploaded) SuccessColor else WarningColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isUploaded) Color.Transparent else Color(0xFFFFFBEB))
    ) {
        // Actual image thumbnail
        if (file.exists()) {
            coil.compose.AsyncImage(
                model = file,
                contentDescription = "Photo thumbnail",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onView() },
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback placeholder
            Box(
                modifier = Modifier.fillMaxSize().background(PrimaryCoral.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = PrimaryCoral,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Upload status badge — amber exclamation mark if not uploaded
        if (!isUploaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(WarningColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "!",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(22.dp)
                .background(ErrorColor, CircleShape)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun GpsCaptureCard(
    latitude: String,
    longitude: String,
    accuracy: String,
    altitude: String,
    screenshotPath: String?,
    onOpenCapture: () -> Unit,
    onRemove: () -> Unit
) {
    val hasCoords = latitude.isNotBlank() && latitude != "Fetching..."
    val isAccurate = accuracy.removePrefix("<").removeSuffix("m").toIntOrNull()?.let { it <= 4 } == true

    if (screenshotPath != null && hasCoords) {
        // Show captured GPS summary card with screenshot — DailyMe warm style
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0x20000000),
                    spotColor = Color(0x15000000)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            border = BorderStroke(1.5.dp, if (isAccurate) SuccessColor else WarningColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coral accent bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .background(PrimaryCoral, RoundedCornerShape(2.dp))
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "GPS CAPTURED",
                            fontSize = 10.sp,
                            color = OnSurfaceVariantLight,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("LAT", fontSize = 10.sp, color = OnSurfaceVariantLight)
                                Text(latitude, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("LNG", fontSize = 10.sp, color = OnSurfaceVariantLight)
                                Text(longitude, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("ACC", fontSize = 10.sp, color = OnSurfaceVariantLight)
                                Text(
                                    accuracy, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = if (isAccurate) SuccessColor else WarningColor
                                )
                            }
                            Column {
                                Text("ALT", fontSize = 10.sp, color = OnSurfaceVariantLight)
                                Text(altitude.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, "Remove", tint = ErrorColor)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Screenshot thumbnail — works with local file paths AND server URLs
                val file = java.io.File(screenshotPath)
                val imageModel: Any = if (file.exists()) file else screenshotPath
                coil.compose.AsyncImage(
                    model = imageModel,
                    contentDescription = "GPS Screenshot",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenCapture,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, PrimaryCoral),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCoral)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Re-capture GPS", fontSize = 13.sp)
                }
            }
        }
    } else {
        // Show capture button — DailyMe warm style
        OutlinedCard(
            onClick = onOpenCapture,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0x20000000),
                    spotColor = Color(0x15000000)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = SurfaceLight),
            border = BorderStroke(1.5.dp, PrimaryCoral.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = PrimaryCoral,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Open GPS Capture Screen",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OnSurfaceLight
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap to open the map, wait for best accuracy, then capture",
                    fontSize = 12.sp,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center
                )
                if (hasCoords) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Lat: $latitude", fontSize = 11.sp, color = OnSurfaceVariantLight)
                        Text("Lng: $longitude", fontSize = 11.sp, color = OnSurfaceVariantLight)
                        Text("Acc: $accuracy", fontSize = 11.sp, color = OnSurfaceVariantLight)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, PrimaryCoral.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(containerColor = PrimaryCoral.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AddAPhoto,
                contentDescription = "Add photo",
                tint = PrimaryCoral,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

private fun formatDateReadable(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        iso.ifBlank { "—" }
    }
}
