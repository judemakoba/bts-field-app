package com.telco.btsfieldapp.ui.audit

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.telco.btsfieldapp.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SECTION_COLORS = listOf(
    Color(0xFF22C55E), // green
    Color(0xFF3B82F6), // blue
    Color(0xFFF59E0B), // amber
    Color(0xFF8B5CF6), // purple
    Color(0xFFEC4899), // pink
    Color(0xFFEF4444), // red
    Color(0xFF06B6D4), // cyan
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
    viewModel: GroundEquipmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle success
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GroundEquipmentEvent.SubmitSuccess -> onSuccess()
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
                title = { Text("Ground Equipment Scope", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryGreen,
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
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick = viewModel::submit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        enabled = !uiState.isSubmitting
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Submit")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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
                        maxPhotos = 1
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
                        hint = "Auto-filled from your account",
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
                    // GPS Coordinates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormTextField(
                            value = uiState.latitude,
                            onValueChange = viewModel::onLatitudeChange,
                            label = "Latitude",
                            hint = "e.g. 0.314626",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        FormTextField(
                            value = uiState.longitude,
                            onValueChange = viewModel::onLongitudeChange,
                            label = "Longitude",
                            hint = "e.g. 32.622251",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormTextField(
                            value = uiState.gpsAccuracy,
                            onValueChange = viewModel::onGpsAccuracyChange,
                            label = "GPS Accuracy (m)",
                            hint = "<4m",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        FormTextField(
                            value = uiState.altitude,
                            onValueChange = viewModel::onAltitudeChange,
                            label = "Altitude (m)",
                            hint = "Altitude",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    PhotoCaptureRow(
                        label = "GPS Screenshot (Accuracy < 4m)",
                        photos = uiState.gpsScreenshotPath?.let { listOf(it) } ?: emptyList(),
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "gps_screenshot") },
                        onRemovePhoto = { viewModel.onGpsScreenshot("") },
                        maxPhotos = 1
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
                        label = "Site Photo",
                        photos = uiState.sitePhotoPath?.let { listOf(it) } ?: emptyList(),
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "site_photo") },
                        onRemovePhoto = { viewModel.onSitePhoto("") },
                        maxPhotos = 1
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
                        maxPhotos = 4
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
                        maxPhotos = 20
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
                        maxPhotos = 3
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
                        maxPhotos = 5
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
                        maxPhotos = 3
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
                        maxPhotos = 3
                    )
                }
            }

            // Section 7: Media & Remarks
            item {
                FormSectionCard(
                    index = 7,
                    title = "Media & Remarks",
                    icon = Icons.Default.Comment,
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
                                selectedContainerColor = PrimaryGreen,
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
                                selectedContainerColor = PrimaryGreen,
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
// SECTION CARD
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
    val color = SECTION_COLORS.getOrElse(index) { PrimaryGreen }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Section header
            Surface(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "$index. $title",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = color
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
// FORM FIELD COMPONENTS
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
        placeholder = { Text(hint, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryGreen,
            focusedLabelColor = PrimaryGreen
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
        placeholder = { Text(hint, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
        readOnly = true,
        enabled = false,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = PrimaryGreen.copy(alpha = 0.3f),
            disabledTextColor = PrimaryGreen,
            disabledLabelColor = PrimaryGreen.copy(alpha = 0.7f)
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
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                focusedLabelColor = PrimaryGreen
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
                        { Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGreen) }
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
                    selectedContainerColor = PrimaryGreen,
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
                        selectedContainerColor = PrimaryGreen,
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
// PHOTO CAPTURE ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhotoCaptureRow(
    label: String,
    photos: List<String>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    maxPhotos: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${photos.size}/$maxPhotos",
                style = MaterialTheme.typography.labelSmall,
                color = if (photos.size >= maxPhotos) ErrorColor else PrimaryGreen
            )
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos) { path ->
                PhotoThumbnail(
                    path = path,
                    onRemove = { onRemovePhoto(path) }
                )
            }
            if (photos.size < maxPhotos) {
                item {
                    AddPhotoButton(onClick = onAddPhoto)
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(
    path: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        // Show file name as placeholder if no image preview
        val fileName = path.substringAfterLast("/").substringAfterLast("\\").take(10)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryGreen.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryGreen,
                    maxLines = 1
                )
            }
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(ErrorColor, RoundedCornerShape(4.dp))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(containerColor = PrimaryGreen.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AddAPhoto,
                contentDescription = "Add photo",
                tint = PrimaryGreen,
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
