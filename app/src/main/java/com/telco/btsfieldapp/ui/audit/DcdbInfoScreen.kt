package com.telco.btsfieldapp.ui.audit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telco.btsfieldapp.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

private val SECTION_COLORS = listOf(
    Color(0xFFF59E0B), // amber  — DCDB
    Color(0xFFEF4444), // red    — Non-Priority
    Color(0xFF22C55E), // green  — Priority
    Color(0xFF3B82F6), // blue   — DCDU Connections
    Color(0xFF8B5CF6), // purple — RRU
    Color(0xFFEC4899), // pink   — AAU
    Color(0xFF06B6D4), // cyan   — BTS Earthing
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

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DcdbInfoEvent.SubmitSuccess -> onSuccess()
            }
        }
    }
    LaunchedEffect(uiState.submitError) {
        uiState.submitError?.let { snackbarHostState.showSnackbar(it) }
    }

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
                    containerColor = Color(0xFFF59E0B),
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
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }
                    Button(
                        onClick = viewModel::submit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
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
                    SubSectionHeader("Non-Priority Cable", Color(0xFFEF4444))
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
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = slot.breakerRating,
                                onValueChange = { viewModel.onNpDcduBreakerChange(idx, it) },
                                label = { Text("MCB (A)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
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
                        maxPhotos = 1
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
                        maxPhotos = 1
                    )
                    if (uiState.npLoadMeasuredTime.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        FormReadOnlyField(
                            label = "Time When Load Was Measured",
                            value = uiState.npLoadMeasuredTime
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))

                    // ── Priority ────────────────────────────────────────────
                    SubSectionHeader("Priority Cable", Color(0xFF22C55E))
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
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = slot.breakerRating,
                                onValueChange = { viewModel.onPDcduBreakerChange(idx, it) },
                                label = { Text("MCB (A)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
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
                        maxPhotos = 1
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
                        maxPhotos = 1
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
                    color = SECTION_COLORS[3],
                    isExpanded = uiState.expandedSections.contains(1),
                    onToggle = { viewModel.toggleSection(1) }
                ) {
                    SubSectionHeader("Non-Priority Connections", Color(0xFFEF4444))
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
                                shape = RoundedCornerShape(12.dp)
                            )
                            val hasPhoto = conn.photoPath != null
                            OutlinedButton(
                                onClick = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "dcdb_np_conn_$idx") },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (hasPhoto) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (hasPhoto) Color(0xFF22C55E) else PrimaryGreen
                                )
                            }
                            if (uiState.npDcdusConnections.size > 1) {
                                IconButton(onClick = { viewModel.removeNpDcduConnection(idx) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = ErrorColor)
                                }
                            }
                        }
                        if (conn.photoPath != null) {
                            Text(
                                text = "✓ Photo captured",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF22C55E),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    TextButton(onClick = viewModel::addNpDcduConnection) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Non-Priority Breaker DCDU")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))

                    SubSectionHeader("Priority Connections", Color(0xFF22C55E))
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
                                shape = RoundedCornerShape(12.dp)
                            )
                            val hasPhoto = conn.photoPath != null
                            OutlinedButton(
                                onClick = { onCapturePhoto(uiState.siteId, uiState.siteName, uiState.locationSummary, "dcdb_p_conn_$idx") },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (hasPhoto) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (hasPhoto) Color(0xFF22C55E) else PrimaryGreen
                                )
                            }
                            if (uiState.pDcdusConnections.size > 1) {
                                IconButton(onClick = { viewModel.removePDcduConnection(idx) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = ErrorColor)
                                }
                            }
                        }
                        if (conn.photoPath != null) {
                            Text(
                                text = "✓ Photo captured",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF22C55E),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
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
                    color = SECTION_COLORS[4],
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

                    SubSectionHeader("Power Cables", Color(0xFF8B5CF6))
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruPowerCableCount, onValueChange = viewModel::onRruPowerCableCountChange, label = "RRU Power Cable Count", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruPowerCableMissing, onValueChange = viewModel::onRruPowerCableMissingChange, label = "RRU Power Cable Missing", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruPowerCableLengthPerRun, onValueChange = viewModel::onRruPowerCableLengthPerRunChange, label = "RRU Power Cable Length per Run (m)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.rruPowerCableTotalMissing, onValueChange = viewModel::onRruPowerCableTotalMissingChange, label = "RRU Power Cable Total Length Missing (m)", keyboardType = KeyboardType.Number)

                    Spacer(Modifier.height(12.dp))
                    SubSectionHeader("Earthing Cables", Color(0xFF8B5CF6))
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
                    color = SECTION_COLORS[5],
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

                    SubSectionHeader("Power Cables", Color(0xFFEC4899))
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.aauPowerCableCount, onValueChange = viewModel::onAauPowerCableCountChange, label = "AAU Power Cable Count", keyboardType = KeyboardType.Number)
                    Spacer( Modifier.height(8.dp))
                    FormTextField(value = uiState.aauPowerCableMissing, onValueChange = viewModel::onAauPowerCableMissingChange, label = "AAU Power Cable Missing", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.aauPowerCableLengthPerRun, onValueChange = viewModel::onAauPowerCableLengthPerRunChange, label = "AAU Power Cable Length per Run (m)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.aauPowerCableTotalMissing, onValueChange = viewModel::onAauPowerCableTotalMissingChange, label = "AAU Power Cable Total Length Missing (m)", keyboardType = KeyboardType.Number)

                    Spacer(Modifier.height(12.dp))
                    SubSectionHeader("Earthing Cables", Color(0xFFEC4899))
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
                    color = SECTION_COLORS[6],
                    isExpanded = uiState.expandedSections.contains(4),
                    onToggle = { viewModel.toggleSection(4) }
                ) {
                    FormTextField(value = uiState.btsEarthingCableCount, onValueChange = viewModel::onBtsEarthingCableCountChange, label = "BTS Earthing Cable Count (pcs)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.btsEarthingCableMissing, onValueChange = viewModel::onBtsEarthingCableMissingChange, label = "BTS Earthing Cable Missing (pcs)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.btsEarthingLengthPerRun, onValueChange = viewModel::onBtsEarthingLengthPerRunChange, label = "BTS Earthing Length per Run (m)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    FormTextField(value = uiState.btsEarthingTotalMissing, onValueChange = viewModel::onBtsEarthingTotalMissingChange, label = "BTS Earthing Total Length Missing (m)", keyboardType = KeyboardType.Number)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column { }
    }
    LaunchedEffect(isExpanded) {}
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header
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
private fun SubSectionHeader(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
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
        placeholder = { Text(hint, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryGreen,
            focusedLabelColor = PrimaryGreen
        )
    )
}

@Composable
private fun FormReadOnlyField(label: String, value: String, hint: String = "") {
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
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${photos.size}/$maxPhotos",
                style = MaterialTheme.typography.labelSmall,
                color = if (photos.size >= maxPhotos) ErrorColor else PrimaryGreen
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            photos.forEach { path ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .background(PrimaryGreen.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    val fileName = path.substringAfterLast("/").substringAfterLast("\\").take(10)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                        Text(text = fileName, style = MaterialTheme.typography.labelSmall, color = PrimaryGreen, maxLines = 1)
                    }
                    IconButton(
                        onClick = { onRemovePhoto(path) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(ErrorColor, RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (photos.size < maxPhotos) {
                OutlinedCard(
                    onClick = onAddPhoto,
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.5f)),
                    colors = CardDefaults.outlinedCardColors(containerColor = PrimaryGreen.copy(alpha = 0.05f))
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AddAPhoto, "Add photo", tint = PrimaryGreen, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}
