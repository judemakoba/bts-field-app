package com.telco.btsfieldapp.ui.audit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import com.telco.btsfieldapp.domain.model.AntennaEntry
import com.telco.btsfieldapp.domain.model.RruEntry
import com.telco.btsfieldapp.ui.components.PhotoViewerDialog
import com.telco.btsfieldapp.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

private val SECTION_COLORS = listOf(
    Color(0xFF3B82F6), // blue  — Antenna
    Color(0xFF8B5CF6), // purple — RRU
)

private val EQUIPMENT_TYPES = listOf("RF Antenna", "MW Antenna", "MW ODU", "RRU", "AAU")
private val SECTORS = listOf("Sec A", "Sec B", "Sec C", "Sec D", "N/A")
private val ACTIVE_OPTIONS = listOf("Active", "Inactive")
private val LABEL_OPTIONS = listOf("Done", "Not Done")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TowerInfoScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onCapturePhoto: (String, String, String, String) -> Unit,
    viewModel: TowerInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Track which entry+field is waiting for a camera photo
    var pendingPhotoKey by remember { mutableStateOf<String?>(null) }
    var viewerPhoto by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is TowerInfoEvent.SubmitSuccess -> onSuccess()
                is TowerInfoEvent.SaveDraftSuccess -> snackbarHostState.showSnackbar("Draft saved successfully")
            }
        }
    }
    LaunchedEffect(uiState.submitError) {
        uiState.submitError?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tower Equipment Scope", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3B82F6),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
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
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isBusy
                    ) {
                        if (uiState.isSavingDraft || uiState.isUploadingPhotos) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Draft")
                        }
                    }
                    Button(
                        onClick = viewModel::submit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        enabled = !isBusy
                    ) {
                        if (uiState.isSubmitting || uiState.isUploadingPhotos) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Submit for Review")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            // ── Section 0: Antenna ──────────────────────────────────────
            item {
                SectionCard(
                    index = 0,
                    title = "Antenna",
                    icon = Icons.Default.CellTower,
                    color = SECTION_COLORS[0],
                    isExpanded = uiState.expandedSections.contains(0),
                    onToggle = { viewModel.toggleSection(0) }
                ) {
                    Text(
                        text = "Add all antenna entries for this site. Each entry captures RF/MW antenna details, dimensions, and photos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantLight,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            itemsIndexed(
                uiState.antennas,
                key = { _, a -> "ant_${a.id}" }
            ) { idx, antenna ->
                AntennaEntryCard(
                    antenna = antenna,
                    index = idx,
                    isLast = uiState.antennas.size == 1,
                    siteName = uiState.siteName,
                    locationSummary = uiState.locationSummary,
                    uploadedPhotoPaths = uiState.uploadedPhotoPaths,
                    onCapturePhoto = { entryId, photoType, siteName, locationSummary ->
                        onCapturePhoto(uiState.siteId, siteName, locationSummary, "tower_ant_${entryId}_$photoType")
                    },
                    onPhotoReceived = { entryId, photoType, path ->
                        viewModel.onAntennaPhoto(entryId, photoType, path)
                    },
                    onPhotoView = { path -> viewerPhoto = path },
                    onPhotoDelete = { entryId, photoType ->
                        viewModel.onAntennaPhoto(entryId, photoType, "")
                    },
                    onEquipmentTypeChange = { viewModel.onAntennaEquipmentType(antenna.id, it) },
                    onManufacturerChange = { viewModel.onAntennaManufacturer(antenna.id, it) },
                    onModelChange = { viewModel.onAntennaModel(antenna.id, it) },
                    onSectorChange = { viewModel.onAntennaSector(antenna.id, it) },
                    onAzimuthChange = { viewModel.onAntennaAzimuth(antenna.id, it) },
                    onHeightChange = { viewModel.onAntennaHeight(antenna.id, it) },
                    onLengthChange = { viewModel.onAntennaLength(antenna.id, it) },
                    onWidthChange = { viewModel.onAntennaWidth(antenna.id, it) },
                    onHeightDimChange = { viewModel.onAntennaHeightDim(antenna.id, it) },
                    onActiveChange = { viewModel.onAntennaActive(antenna.id, it) },
                    onLabellingChange = { viewModel.onAntennaLabelling(antenna.id, it) },
                    onRemove = { viewModel.removeAntenna(antenna.id) }
                )
            }

            item {
                OutlinedButton(
                    onClick = viewModel::addAntenna,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, SECTION_COLORS[0].copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SECTION_COLORS[0])
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Antenna")
                }
            }

            // ── Section 1: RRU ────────────────────────────────────────
            item {
                SectionCard(
                    index = 1,
                    title = "RRU",
                    icon = Icons.Default.SignalCellularAlt,
                    color = SECTION_COLORS[1],
                    isExpanded = uiState.expandedSections.contains(1),
                    onToggle = { viewModel.toggleSection(1) }
                ) {
                    Text(
                        text = "Add all RRU equipment entries. Captures RRU details, dimensions, and photos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantLight,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            itemsIndexed(
                uiState.rrus,
                key = { _, r -> "rru_${r.id}" }
            ) { idx, rru ->
                RruEntryCard(
                    rru = rru,
                    index = idx,
                    isLast = uiState.rrus.size == 1,
                    siteName = uiState.siteName,
                    locationSummary = uiState.locationSummary,
                    uploadedPhotoPaths = uiState.uploadedPhotoPaths,
                    onCapturePhoto = { entryId, photoType, siteName, locationSummary ->
                        onCapturePhoto(uiState.siteId, siteName, locationSummary, "tower_rru_${entryId}_$photoType")
                    },
                    onPhotoReceived = { entryId, photoType, path ->
                        viewModel.onRruPhoto(entryId, photoType, path)
                    },
                    onPhotoView = { path -> viewerPhoto = path },
                    onPhotoDelete = { entryId, photoType ->
                        viewModel.onRruPhoto(entryId, photoType, "")
                    },
                    onEquipmentTypeChange = { viewModel.onRruEquipmentType(rru.id, it) },
                    onManufacturerChange = { viewModel.onRruManufacturer(rru.id, it) },
                    onModelChange = { viewModel.onRruModel(rru.id, it) },
                    onSectorChange = { viewModel.onRruSector(rru.id, it) },
                    onLengthChange = { viewModel.onRruLength(rru.id, it) },
                    onWidthChange = { viewModel.onRruWidth(rru.id, it) },
                    onHeightChange = { viewModel.onRruHeight(rru.id, it) },
                    onActiveChange = { viewModel.onRruActive(rru.id, it) },
                    onLabellingChange = { viewModel.onRruLabelling(rru.id, it) },
                    onRemove = { viewModel.removeRru(rru.id) }
                )
            }

            item {
                OutlinedButton(
                    onClick = viewModel::addRru,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, SECTION_COLORS[1].copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SECTION_COLORS[1])
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add RRU")
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Photo viewer dialog
    viewerPhoto?.let { path ->
        PhotoViewerDialog(
            photoPath = path,
            onDismiss = { viewerPhoto = null },
            onDelete = {
                // Find and clear from the right antenna or RRU entry
                val antIdx = uiState.antennas.indexOfFirst { a ->
                    a.modelPlatePhoto == path || a.portsPhoto == path ||
                    a.dimensionsPhoto1 == path || a.dimensionsPhoto2 == path || a.dimensionsPhoto3 == path ||
                    a.azimuthPhoto == path || a.heightPhoto == path
                }
                if (antIdx >= 0) {
                    val a = uiState.antennas[antIdx]
                    val photoType = when (path) {
                        a.modelPlatePhoto -> "model_plate"
                        a.portsPhoto -> "ports"
                        a.dimensionsPhoto1 -> "dim1"
                        a.dimensionsPhoto2 -> "dim2"
                        a.dimensionsPhoto3 -> "dim3"
                        a.azimuthPhoto -> "azimuth"
                        a.heightPhoto -> "height"
                        else -> return@PhotoViewerDialog
                    }
                    viewModel.onAntennaPhoto(a.id, photoType, "")
                } else {
                    val rruIdx = uiState.rrus.indexOfFirst { r ->
                        r.modelPlatePhoto == path ||
                        r.dimensionsPhoto1 == path || r.dimensionsPhoto2 == path || r.dimensionsPhoto3 == path
                    }
                    if (rruIdx >= 0) {
                        val r = uiState.rrus[rruIdx]
                        val photoType = when (path) {
                            r.modelPlatePhoto -> "model_plate"
                            r.dimensionsPhoto1 -> "dim1"
                            r.dimensionsPhoto2 -> "dim2"
                            r.dimensionsPhoto3 -> "dim3"
                            else -> return@PhotoViewerDialog
                        }
                        viewModel.onRruPhoto(r.id, photoType, "")
                    }
                }
                viewerPhoto = null
            }
        )
    }
    } // end Box
}

// ─────────────────────────────────────────────────────────────────────────────
// ANTENNA ENTRY CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AntennaEntryCard(
    antenna: AntennaEntry,
    index: Int,
    isLast: Boolean,
    siteName: String,
    locationSummary: String,
    uploadedPhotoPaths: Set<String>,
    onCapturePhoto: (String, String, String, String) -> Unit,
    onPhotoReceived: (String, String, String) -> Unit,
    onPhotoView: (String) -> Unit,
    onPhotoDelete: (String, String) -> Unit,
    onEquipmentTypeChange: (String) -> Unit,
    onManufacturerChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSectorChange: (String) -> Unit,
    onAzimuthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onHeightDimChange: (String) -> Unit,
    onActiveChange: (String) -> Unit,
    onLabellingChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Antenna ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SECTION_COLORS[0]
                )
                Spacer(Modifier.weight(1f))
                if (!isLast) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.DeleteOutline, "Remove", tint = ErrorColor)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = SECTION_COLORS[0].copy(alpha = 0.2f)
            )

            // Equipment Type
            DropdownField(
                value = antenna.equipmentType,
                onValueChange = onEquipmentTypeChange,
                label = "RF/TRM Equipment Type",
                options = EQUIPMENT_TYPES,
                color = SECTION_COLORS[0]
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormTextField(
                    value = antenna.manufacturer,
                    onValueChange = onManufacturerChange,
                    label = "Manufacturer",
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    value = antenna.modelNumber,
                    onValueChange = onModelChange,
                    label = "Model Number",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            // Tenant Owner — readonly
            FormReadOnlyField(label = "Tenant Owner", value = "Airtel")
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    value = antenna.sector,
                    onValueChange = onSectorChange,
                    label = "Sector",
                    options = SECTORS,
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    value = antenna.azimuth,
                    onValueChange = onAzimuthChange,
                    label = "Azimuth (°)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            FormTextField(
                value = antenna.heightToCentre,
                onValueChange = onHeightChange,
                label = "Height to Centre of Antenna (mm)",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(8.dp))

            // Dimensions
            RowTitle("Antenna Size", SECTION_COLORS[0])
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormTextField(
                    value = antenna.lengthDia,
                    onValueChange = onLengthChange,
                    label = "Length/Dia (mm)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    value = antenna.width,
                    onValueChange = onWidthChange,
                    label = "Width (mm)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    value = antenna.height,
                    onValueChange = onHeightDimChange,
                    label = "Height (mm)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            // Status
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    value = antenna.activeInactive,
                    onValueChange = onActiveChange,
                    label = "Active / Inactive",
                    options = ACTIVE_OPTIONS,
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
                DropdownField(
                    value = antenna.equipmentLabelling,
                    onValueChange = onLabellingChange,
                    label = "Equipment Labelling",
                    options = LABEL_OPTIONS,
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))

            // Photos
            RowTitle("Photos", SECTION_COLORS[0])
            Spacer(Modifier.height(6.dp))

            // Model plate + ports row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TowerPhotoThumbnail(
                    label = "Model Plate",
                    photoPath = antenna.modelPlatePhoto,
                    isUploaded = antenna.modelPlatePhoto in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(antenna.id, siteName, locationSummary, "tower_ant_${antenna.id}_model_plate") },
                    onRemove = { onPhotoDelete(antenna.id, "model_plate") },
                    onView = { antenna.modelPlatePhoto?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
                TowerPhotoThumbnail(
                    label = "Ports",
                    photoPath = antenna.portsPhoto,
                    isUploaded = antenna.portsPhoto in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(antenna.id, siteName, locationSummary, "tower_ant_${antenna.id}_ports") },
                    onRemove = { onPhotoDelete(antenna.id, "ports") },
                    onView = { antenna.portsPhoto?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            // Dimensions photos (3)
            RowTitle("Antenna Dimensions (3 Photos)", SECTION_COLORS[0], small = true)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TowerPhotoThumbnail(
                    label = "Dim 1",
                    photoPath = antenna.dimensionsPhoto1,
                    isUploaded = antenna.dimensionsPhoto1 in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(antenna.id, siteName, locationSummary, "tower_ant_${antenna.id}_dim1") },
                    onRemove = { onPhotoDelete(antenna.id, "dim1") },
                    onView = { antenna.dimensionsPhoto1?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
                TowerPhotoThumbnail(
                    label = "Dim 2",
                    photoPath = antenna.dimensionsPhoto2,
                    isUploaded = antenna.dimensionsPhoto2 in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(antenna.id, siteName, locationSummary, "tower_ant_${antenna.id}_dim2") },
                    onRemove = { onPhotoDelete(antenna.id, "dim2") },
                    onView = { antenna.dimensionsPhoto2?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
                TowerPhotoThumbnail(
                    label = "Dim 3",
                    photoPath = antenna.dimensionsPhoto3,
                    isUploaded = antenna.dimensionsPhoto3 in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(antenna.id, siteName, locationSummary, "tower_ant_${antenna.id}_dim3") },
                    onRemove = { onPhotoDelete(antenna.id, "dim3") },
                    onView = { antenna.dimensionsPhoto3?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TowerPhotoThumbnail(
                    label = "Azimuth",
                    photoPath = antenna.azimuthPhoto,
                    isUploaded = antenna.azimuthPhoto in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(antenna.id, siteName, locationSummary, "tower_ant_${antenna.id}_azimuth") },
                    onRemove = { onPhotoDelete(antenna.id, "azimuth") },
                    onView = { antenna.azimuthPhoto?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
                TowerPhotoThumbnail(
                    label = "Height",
                    photoPath = antenna.heightPhoto,
                    isUploaded = antenna.heightPhoto in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(antenna.id, siteName, locationSummary, "tower_ant_${antenna.id}_height") },
                    onRemove = { onPhotoDelete(antenna.id, "height") },
                    onView = { antenna.heightPhoto?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[0],
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RRU ENTRY CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RruEntryCard(
    rru: RruEntry,
    index: Int,
    isLast: Boolean,
    siteName: String,
    locationSummary: String,
    uploadedPhotoPaths: Set<String>,
    onCapturePhoto: (String, String, String, String) -> Unit,
    onPhotoReceived: (String, String, String) -> Unit,
    onPhotoView: (String) -> Unit,
    onPhotoDelete: (String, String) -> Unit,
    onEquipmentTypeChange: (String) -> Unit,
    onManufacturerChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSectorChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onActiveChange: (String) -> Unit,
    onLabellingChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RRU ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SECTION_COLORS[1]
                )
                Spacer(Modifier.weight(1f))
                if (!isLast) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.DeleteOutline, "Remove", tint = ErrorColor)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = SECTION_COLORS[1].copy(alpha = 0.2f)
            )

            DropdownField(
                value = rru.equipmentType,
                onValueChange = onEquipmentTypeChange,
                label = "RF/TRM Equipment Type",
                options = EQUIPMENT_TYPES,
                color = SECTION_COLORS[1]
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormTextField(
                    value = rru.manufacturer,
                    onValueChange = onManufacturerChange,
                    label = "RRU Manufacturer",
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    value = rru.modelNumber,
                    onValueChange = onModelChange,
                    label = "RRU Model Number",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            FormReadOnlyField(label = "Tenant Owner", value = "Airtel")
            Spacer(Modifier.height(8.dp))

            DropdownField(
                value = rru.sector,
                onValueChange = onSectorChange,
                label = "Sector",
                options = SECTORS,
                color = SECTION_COLORS[1]
            )
            Spacer(Modifier.height(8.dp))

            // Dimensions
            RowTitle("RRU Size", SECTION_COLORS[1])
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormTextField(
                    value = rru.lengthDia,
                    onValueChange = onLengthChange,
                    label = "Length/Dia (mm)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    value = rru.width,
                    onValueChange = onWidthChange,
                    label = "Width (mm)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                FormTextField(
                    value = rru.height,
                    onValueChange = onHeightChange,
                    label = "Height (mm)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    value = rru.activeInactive,
                    onValueChange = onActiveChange,
                    label = "Active / Inactive",
                    options = ACTIVE_OPTIONS,
                    color = SECTION_COLORS[1],
                    modifier = Modifier.weight(1f)
                )
                DropdownField(
                    value = rru.equipmentLabelling,
                    onValueChange = onLabellingChange,
                    label = "Equipment Labelling",
                    options = LABEL_OPTIONS,
                    color = SECTION_COLORS[1],
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))

            RowTitle("Photos", SECTION_COLORS[1])
            Spacer(Modifier.height(6.dp))

            TowerPhotoThumbnail(
                label = "Model Plate",
                photoPath = rru.modelPlatePhoto,
                isUploaded = rru.modelPlatePhoto in uploadedPhotoPaths,
                onCapture = { onCapturePhoto(rru.id, siteName, locationSummary, "tower_rru_${rru.id}_model_plate") },
                onRemove = { onPhotoDelete(rru.id, "model_plate") },
                onView = { rru.modelPlatePhoto?.let { onPhotoView(it) } },
                color = SECTION_COLORS[1],
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            RowTitle("RRU Dimensions (3 Photos)", SECTION_COLORS[1], small = true)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TowerPhotoThumbnail(
                    label = "Dim 1",
                    photoPath = rru.dimensionsPhoto1,
                    isUploaded = rru.dimensionsPhoto1 in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(rru.id, siteName, locationSummary, "tower_rru_${rru.id}_dim1") },
                    onRemove = { onPhotoDelete(rru.id, "dim1") },
                    onView = { rru.dimensionsPhoto1?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[1],
                    modifier = Modifier.weight(1f)
                )
                TowerPhotoThumbnail(
                    label = "Dim 2",
                    photoPath = rru.dimensionsPhoto2,
                    isUploaded = rru.dimensionsPhoto2 in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(rru.id, siteName, locationSummary, "tower_rru_${rru.id}_dim2") },
                    onRemove = { onPhotoDelete(rru.id, "dim2") },
                    onView = { rru.dimensionsPhoto2?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[1],
                    modifier = Modifier.weight(1f)
                )
                TowerPhotoThumbnail(
                    label = "Dim 3",
                    photoPath = rru.dimensionsPhoto3,
                    isUploaded = rru.dimensionsPhoto3 in uploadedPhotoPaths,
                    onCapture = { onCapturePhoto(rru.id, siteName, locationSummary, "tower_rru_${rru.id}_dim3") },
                    onRemove = { onPhotoDelete(rru.id, "dim3") },
                    onView = { rru.dimensionsPhoto3?.let { onPhotoView(it) } },
                    color = SECTION_COLORS[1],
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    index: Int,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Surface(
                onClick = onToggle,
                color = color.copy(alpha = 0.12f),
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
                            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Section ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = color
                    )
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun RowTitle(text: String, color: Color, small: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, RoundedCornerShape(2.5.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text("Select...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = color,
                focusedLabelColor = color
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
                        { Icon(Icons.Default.Check, null, tint = color) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryGreen,
            focusedLabelColor = PrimaryGreen
        )
    )
}

@Composable
private fun FormReadOnlyField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
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

@Composable
private fun TowerPhotoThumbnail(
    label: String,
    photoPath: String?,
    isUploaded: Boolean,
    onCapture: () -> Unit,
    onRemove: () -> Unit,
    onView: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember(photoPath) { photoPath?.let { File(it) } }

    if (photoPath != null && file != null && file.exists()) {
        // Show thumbnail with amber border/badge if not uploaded
        Box(
            modifier = modifier
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.5.dp,
                    color = if (isUploaded) color else Color(0xFFF59E0B),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onView() }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = label,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            // Upload badge
            if (!isUploaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(18.dp)
                        .background(Color(0xFFF59E0B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Label overlay at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .background(ErrorColor, CircleShape)
            ) {
                Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    } else {
        // No photo — show capture button
        OutlinedButton(
            onClick = onCapture,
            modifier = modifier.height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, color.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = color
            )
        ) {
            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
