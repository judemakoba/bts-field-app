package com.telco.btsfieldapp.ui.audit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import com.telco.btsfieldapp.ui.components.PhotoViewerDialog
import com.telco.btsfieldapp.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// DailyMe Warm Palette — section-specific colors
private val SECTION_COLORS = listOf(
    Color(0xFFF59E0B), // amber   — DCDB
    Color(0xFF8B5CF6), // purple — DCDU Connections
    Color(0xFF8B5CF6), // purple — RRU
    Color(0xFFEC4899), // pink   — AAU
    Color(0xFFF97316), // coral  — BTS Earthing
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DcdbInfoScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onCapturePhoto: (String, String, String, String) -> Unit,
    viewModel: DcdbInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Photo viewer state for DCDU connection photos
    var viewerConnPhoto by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DcdbInfoEvent.SubmitSuccess -> onSuccess()
                is DcdbInfoEvent.SaveDraftSuccess -> snackbarHostState.showSnackbar("Draft saved successfully")
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
                title = { Text("DCDB Information", fontWeight = FontWeight.Bold) },
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
                color = SurfaceLight,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isBusy = uiState.isSubmitting || uiState.isSavingDraft || uiState.isUploadingPhotos

                    // Save Draft — outline coral
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryCoral)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Draft")
                        }
                    }
                    // Submit for Review — filled coral
                    Button(
                        onClick = viewModel::submit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
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
                            Spacer(Modifier.width(6.dp))
                            Text("Submit for Review")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight
    ) { padding ->
        // Upload progress overlay — warm cream with coral loading
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
            // ── Section 0: DCDB ────────────────────────────────────────────────
            item {
                SectionCard(
                    index = 0,
                    title = "DCDB",
                    icon = Icons.Default.ElectricalServices,
                    color = SECTION_COLORS[0],
                    isExpanded = uiState.expandedSections.contains(0),
                    onToggle = { viewModel.toggleSection(0) }
                ) {
                    FormTextField(
                        value = uiState.gridDistanceTo3Phase,
                        onValueChange = viewModel::onGridDistanceChange,
                        label = "Grid Distance to 3-Phase Line",
                        hint = "Enter metres (pulled from Ground Equipment)",
                        keyboardType = KeyboardType.Number
                    )

                    // ── Non-Priority ─────────────────────────────────────────
                    SubSectionHeader("Non-Priority Cable", ErrorColor)
                    Spacer(Modifier.height(8.dp))

                    FormTextField(
                        value = uiState.npCableSizeDcdb,
                        onValueChange = viewModel::onNpCableSizeDcdbChange,
                        label = "Supply Cable Size to DCDB (mm²)",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = uiState.npBreaker1Mcb,
                        onValueChange = viewModel::onNpBreaker1McbChange,
                        label = "Breaker 1 — Supply Cable MCB (A)",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))

                    // DCDU slots
                    uiState.npDcdus.forEachIndexed { idx, slot ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = slot.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(60.dp)
                            )
                            OutlinedTextField(
                                value = slot.cableSize,
                                onValueChange = { viewModel.onNpDcduCableSizeChange(idx, it) },
                                label = { Text("Cable mm²") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            OutlinedTextField(
                                value = slot.breakerRating,
                                onValueChange = { viewModel.onNpDcduBreakerChange(idx, it) },
                                label = { Text("MCB (A)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            if (uiState.npDcdus.size > 1) {
                                IconButton(onClick = { viewModel.removeNpDcdu(idx) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = ErrorColor)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    TextButton(onClick = viewModel::addNpDcdu) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add DCDU Slot")
                    }

                    Spacer(Modifier.height(8.dp))
                    PhotoCaptureRow(
                        label = "Non-Priority DCDB Section Photo",
                        photos = listOfNotNull(uiState.npSectionPhotoPath),
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "dcdb_np_section") },
                        onRemovePhoto = { viewModel.onNpSectionPhoto("") },
                        maxPhotos = 1,
                        uploadedPhotoPaths = uiState.uploadedPhotoPaths
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = uiState.npLoadMeasurement,
                        onValueChange = viewModel::onNpLoadMeasurementChange,
                        label = "Supply Load Measurement (A) — DCDB Non-Priority",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(8.dp))
                    PhotoCaptureRow(
                        label = "Load Measurement Photo (Meter Reading)",
                        photos = listOfNotNull(uiState.npLoadPhotoPath),
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "dcdb_np_load") },
                        onRemovePhoto = { viewModel.onNpLoadPhoto("") },
                        maxPhotos = 1,
                        uploadedPhotoPaths = uiState.uploadedPhotoPaths
                    )
                    if (uiState.npLoadMeasuredTime.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        FormReadOnlyField(
                            label = "Time When Load Was Measured",
                            value = uiState.npLoadMeasuredTime
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = PrimaryCoralLight)

                    // ── Priority ────────────────────────────────────────────
                    SubSectionHeader("Priority Cable", SuccessColor)
                    Spacer(Modifier.height(8.dp))

                    FormTextField(
                        value = uiState.pCableSizeDcdb,
                        onValueChange = viewModel::onPCableSizeDcdbChange,
                        label = "Supply Cable Size to DCDB (mm²)",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = uiState.pBreaker1Mcb,
                        onValueChange = viewModel::onPBreaker1McbChange,
                        label = "Breaker 1 — Supply Cable MCB (A)",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))

                    uiState.pDcdus.forEachIndexed { idx, slot ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = slot.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(60.dp)
                            )
                            OutlinedTextField(
                                value = slot.cableSize,
                                onValueChange = { viewModel.onPDcduCableSizeChange(idx, it) },
                                label = { Text("Cable mm²") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            OutlinedTextField(
                                value = slot.breakerRating,
                                onValueChange = { viewModel.onPDcduBreakerChange(idx, it) },
                                label = { Text("MCB (A)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            if (uiState.pDcdus.size > 1) {
                                IconButton(onClick = { viewModel.removePDcdu(idx) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = ErrorColor)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    TextButton(onClick = viewModel::addPDcdu) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add DCDU Slot")
                    }

                    Spacer(Modifier.height(8.dp))
                    PhotoCaptureRow(
                        label = "Priority DCDB Section Photo",
                        photos = listOfNotNull(uiState.pSectionPhotoPath),
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "dcdb_p_section") },
                        onRemovePhoto = { viewModel.onPSectionPhoto("") },
                        maxPhotos = 1,
                        uploadedPhotoPaths = uiState.uploadedPhotoPaths
                    )
                    Spacer(Modifier.height(8.dp))
                    FormTextField(
                        value = uiState.pLoadMeasurement,
                        onValueChange = viewModel::onPLoadMeasurementChange,
                        label = "Supply Load Measurement (A) — DCDB Priority",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(8.dp))
                    PhotoCaptureRow(
                        label = "Load Measurement Photo (Meter Reading)",
                        photos = listOfNotNull(uiState.pLoadPhotoPath),
                        onAddPhoto = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "dcdb_p_load") },
                        onRemovePhoto = { viewModel.onPLoadPhoto("") },
                        maxPhotos = 1,
                        uploadedPhotoPaths = uiState.uploadedPhotoPaths
                    )
                    if (uiState.pLoadMeasuredTime.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        FormReadOnlyField(
                            label = "Time When Load Was Measured",
                            value = uiState.pLoadMeasuredTime
                        )
                    }
                }
            }

            // ── Section 1: DCDU Connections ──────────────────────────────────
            item {
                SectionCard(
                    index = 1,
                    title = "DCDU Connections",
                    icon = Icons.Default.Hub,
                    color = SECTION_COLORS[1],
                    isExpanded = uiState.expandedSections.contains(1),
                    onToggle = { viewModel.toggleSection(1) }
                ) {
                    SubSectionHeader("Non-Priority Connections", ErrorColor)
                    Spacer(Modifier.height(8.dp))
                    uiState.npDcdusConnections.forEachIndexed { idx, conn ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = conn.breakerLabel,
                                onValueChange = { viewModel.onNpDcduConnectionLabelChange(idx, it) },
                                label = { Text("Breaker ${idx + 1} ID") },
                                placeholder = { Text("e.g. DCDB12A") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            DcdbConnPhotoThumbnail(
                                photoPath = conn.photoPath,
                                isUploaded = conn.photoPath in uiState.uploadedPhotoPaths,
                                onCapture = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "dcdb_np_conn_$idx") },
                                onRemove = { viewModel.onNpDcduConnectionPhotoChange(idx, "") },
                                onView = { conn.photoPath?.let { viewerConnPhoto = it } }
                            )
                            if (uiState.npDcdusConnections.size > 1) {
                                IconButton(onClick = { viewModel.removeNpDcduConnection(idx) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = ErrorColor)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    TextButton(onClick = viewModel::addNpDcduConnection) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Non-Priority Breaker DCDU")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = PrimaryCoralLight)

                    SubSectionHeader("Priority Connections", SuccessColor)
                    Spacer(Modifier.height(8.dp))
                    uiState.pDcdusConnections.forEachIndexed { idx, conn ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = conn.breakerLabel,
                                onValueChange = { viewModel.onPDcduConnectionLabelChange(idx, it) },
                                label = { Text("Breaker ${idx + 1} ID") },
                                placeholder = { Text("e.g. DCDB12A") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            DcdbConnPhotoThumbnail(
                                photoPath = conn.photoPath,
                                isUploaded = conn.photoPath in uiState.uploadedPhotoPaths,
                                onCapture = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "dcdb_p_conn_$idx") },
                                onRemove = { viewModel.onPDcduConnectionPhotoChange(idx, "") },
                                onView = { conn.photoPath?.let { viewerConnPhoto = it } }
                            )
                            if (uiState.pDcdusConnections.size > 1) {
                                IconButton(onClick = { viewModel.removePDcduConnection(idx) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = ErrorColor)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    TextButton(onClick = viewModel::addPDcduConnection) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Priority Breaker DCDU")
                    }

                    Spacer(Modifier.height(12.dp))
                    FormTextField(
                        value = uiState.totalDcdUCount,
                        onValueChange = viewModel::onTotalDcdUCountChange,
                        label = "Total DCDU Count",
                        hint = "Auto-tallied from entries above",
                        keyboardType = KeyboardType.Number
                    )
                    Text(
                        text = "Tally: Non-Priority ${uiState.npDcdusConnections.count { it.breakerLabel.isNotBlank() }} + Priority ${uiState.pDcdusConnections.count { it.breakerLabel.isNotBlank() }} = ${uiState.totalDcdUCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariantLight,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            // ── Section 2: RRU ────────────────────────────────────────────────
            item {
                SectionCard(
                    index = 2,
                    title = "RRU",
                    icon = Icons.Default.SignalCellularAlt,
                    color = SECTION_COLORS[2],
                    isExpanded = uiState.expandedSections.contains(2),
                    onToggle = { viewModel.toggleSection(2) }
                ) {
                    FormTextField(
                        value = uiState.rruCount,
                        onValueChange = viewModel::onRruCountChange,
                        label = "RRU Count",
                        hint = "Should tally with Tower Equipment Scope",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))

                    SubSectionHeader("Power Cables", SECTION_COLORS[2])
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruPowerCableCount, onValueChange = viewModel::onRruPowerCableCountChange, label = "RRU Power Cable Count", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruPowerCableMissing, onValueChange = viewModel::onRruPowerCableMissingChange, label = "RRU Power Cable Missing", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruPowerCableLengthPerRun, onValueChange = viewModel::onRruPowerCableLengthPerRunChange, label = "RRU Power Cable Length per Run (m)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruPowerCableTotalMissing, onValueChange = viewModel::onRruPowerCableTotalMissingChange, label = "RRU Power Cable Total Length Missing (m)", keyboardType = KeyboardType.Number)

                    Spacer(Modifier.height(12.dp))
                    SubSectionHeader("Earthing Cables", SECTION_COLORS[2])
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruEarthingCableCount, onValueChange = viewModel::onRruEarthingCableCountChange, label = "RRU Earthing Cable Count (pcs)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruEarthingCableMissing, onValueChange = viewModel::onRruEarthingCableMissingChange, label = "RRU Earthing Cable Missing (pcs)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruEarthingCableLengthPerRun, onValueChange = viewModel::onRruEarthingCableLengthPerRunChange, label = "RRU Earthing Cable Length per Run (m)", keyboardType = KeyboardType.Number)
                }
            }

            // ── Section 3: AAU ────────────────────────────────────────────────
            item {
                SectionCard(
                    index = 3,
                    title = "AAU",
                    icon = Icons.Default.CellTower,
                    color = SECTION_COLORS[3],
                    isExpanded = uiState.expandedSections.contains(3),
                    onToggle = { viewModel.toggleSection(3) }
                ) {
                    FormTextField(
                        value = uiState.aauCount,
                        onValueChange = viewModel::onAauCountChange,
                        label = "AAU Count",
                        hint = "Should tally with Tower Equipment Scope",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))

                    SubSectionHeader("Power Cables", SECTION_COLORS[3])
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.aauPowerCableCount, onValueChange = viewModel::onAauPowerCableCountChange, label = "AAU Power Cable Count", keyboardType = KeyboardType.Number)
                    Spacer( Modifier.height(8.dp))
                    FormTextField(value = uiState.aauPowerCableMissing, onValueChange = viewModel::onAauPowerCableMissingChange, label = "AAU Power Cable Missing", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.aauPowerCableLengthPerRun, onValueChange = viewModel::onAauPowerCableLengthPerRunChange, label = "AAU Power Cable Length per Run (m)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.aauPowerCableTotalMissing, onValueChange = viewModel::onAauPowerCableTotalMissingChange, label = "AAU Power Cable Total Length Missing (m)", keyboardType = KeyboardType.Number)

                    Spacer(Modifier.height(12.dp))
                    SubSectionHeader("Earthing Cables", SECTION_COLORS[3])
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.aauEarthingCableCount, onValueChange = viewModel::onAauEarthingCableCountChange, label = "AAU Earthing Cable Count (pcs)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.aauEarthingCableMissing, onValueChange = viewModel::onAauEarthingCableMissingChange, label = "AAU Earthing Cable Missing (pcs)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.aauEarthingCableLengthPerRun, onValueChange = viewModel::onAauEarthingCableLengthPerRunChange, label = "AAU Earthing Cable Length per Run (m)", keyboardType = KeyboardType.Number)
                }
            }

            // ── Section 4: BTS Earthing ──────────────────────────────────────
            item {
                SectionCard(
                    index = 4,
                    title = "BTS Earthing",
                    icon = Icons.Default.Bolt,
                    color = SECTION_COLORS[4],
                    isExpanded = uiState.expandedSections.contains(4),
                    onToggle = { viewModel.toggleSection(4) }
                ) {
                    FormTextField(value = uiState.btsEarthingTotal, onValueChange = viewModel::onBtsEarthingTotalChange, label = "BTS Earthing Total Length (m)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.btsEarthingCableCount, onValueChange = viewModel::onBtsEarthingCableCountChange, label = "BTS Earthing Cable Count (pcs)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.btsEarthingCableMissing, onValueChange = viewModel::onBtsEarthingCableMissingChange, label = "BTS Earthing Cable Missing (pcs)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.btsEarthingLengthPerRun, onValueChange = viewModel::onBtsEarthingLengthPerRunChange, label = "BTS Earthing Length per Run (m)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.btsEarthingConnection, onValueChange = viewModel::onBtsEarthingConnectionChange, label = "BTS Earthing Connection")
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.btsEarthingTotalMissing, onValueChange = viewModel::onBtsEarthingTotalMissingChange, label = "BTS Earthing Total Length Missing (m)", keyboardType = KeyboardType.Number)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Photo viewer dialog for DCDU connection photos
    viewerConnPhoto?.let { path ->
        PhotoViewerDialog(
            photoPath = path,
            onDismiss = { viewerConnPhoto = null },
            onDelete = {
                // Find and clear the photo path from the right connection
                val npIdx = uiState.npDcdusConnections.indexOfFirst { it.photoPath == path }
                if (npIdx >= 0) viewModel.onNpDcduConnectionPhotoChange(npIdx, "")
                val pIdx = uiState.pDcdusConnections.indexOfFirst { it.photoPath == path }
                if (pIdx >= 0) viewModel.onPDcduConnectionPhotoChange(pIdx, "")
                viewerConnPhoto = null
            }
        )
    }
    } // end Box
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED / REUSED COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    index: Int,
    title: String,
    icon: ImageVector,
    color: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Colored header strip at top
            Surface(
                onClick = onToggle,
                color = color,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Section number badge — coral circle with white number
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = Color.White
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(BackgroundLight.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SubSectionHeader(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(hint, color = OnSurfaceVariantLight.copy(alpha = 0.5f)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryCoral,
            unfocusedBorderColor = PrimaryCoralLight,
            focusedLabelColor = PrimaryCoral,
            unfocusedLabelColor = OnSurfaceVariantLight,
            focusedTextColor = OnSurfaceLight,
            unfocusedTextColor = OnSurfaceLight,
            cursorColor = PrimaryCoral,
            focusedContainerColor = SurfaceLight,
            unfocusedContainerColor = SurfaceLight
        )
    )
}

@Composable
private fun FormReadOnlyField(label: String, value: String, hint: String = "") {
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
            disabledBorderColor = PrimaryCoralLight,
            disabledTextColor = OnSurfaceLight,
            disabledLabelColor = OnSurfaceVariantLight,
            disabledContainerColor = PrimaryCoralLight.copy(alpha = 0.2f)
        )
    )
}

@Composable
private fun PhotoCaptureRow(
    label: String,
    photos: List<String>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    maxPhotos: Int,
    uploadedPhotoPaths: Set<String> = emptySet()
) {
    var viewerPhoto by remember { mutableStateOf<String?>(null) }

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
                color = if (photos.size >= maxPhotos) SuccessColor else SecondaryAmber
            )
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    OutlinedCard(
                        onClick = onAddPhoto,
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, PrimaryCoral.copy(alpha = 0.5f)),
                        colors = CardDefaults.outlinedCardColors(containerColor = PrimaryCoralLight.copy(alpha = 0.3f))
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddAPhoto, "Add photo", tint = PrimaryCoral, modifier = Modifier.size(28.dp))
                        }
                    }
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

@Composable
private fun PhotoThumbnail(
    path: String,
    isUploaded: Boolean,
    onRemove: () -> Unit,
    onView: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(path) { File(path) }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.5.dp,
                color = if (isUploaded) PrimaryCoral else SecondaryAmber,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isUploaded) MaterialTheme.colorScheme.surface else SecondaryAmberLight.copy(alpha = 0.5f))
    ) {
        // Actual image thumbnail
        if (file.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "Photo thumbnail",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onView() },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(PrimaryCoralLight.copy(alpha = 0.3f)),
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

        // Upload status badge — amber exclamation if not uploaded
        if (!isUploaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(SecondaryAmber, CircleShape),
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
private fun DcdbConnPhotoThumbnail(
    photoPath: String?,
    isUploaded: Boolean,
    onCapture: () -> Unit,
    onRemove: () -> Unit,
    onView: () -> Unit
) {
    if (photoPath != null) {
        val context = LocalContext.current
        val file = remember(photoPath) { File(photoPath) }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = 1.5.dp,
                    color = if (isUploaded) PrimaryCoral else SecondaryAmber,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onView() }
        ) {
            if (file.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Photo thumbnail",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(PrimaryCoralLight.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, null, tint = PrimaryCoral, modifier = Modifier.size(24.dp))
                }
            }

            // Upload badge
            if (!isUploaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                        .size(16.dp)
                        .background(SecondaryAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(ErrorColor, CircleShape)
            ) {
                Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    } else {
        OutlinedButton(
            onClick = onCapture,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, PrimaryCoral.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCoral)
        ) {
            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp), tint = PrimaryCoral)
            Spacer(Modifier.width(4.dp))
            Text("Photo", style = MaterialTheme.typography.labelMedium)
        }
    }
}
