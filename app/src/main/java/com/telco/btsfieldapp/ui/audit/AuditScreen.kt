@file:OptIn(ExperimentalMaterial3Api::class)

package com.telco.btsfieldapp.ui.audit

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telco.btsfieldapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onCapturePhoto: (String, String) -> Unit,
    viewModel: AuditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            snackbarHostState.showSnackbar("Audit saved successfully!")
            kotlinx.coroutines.delay(1000)
            onSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.auditType.label, fontWeight = FontWeight.Bold) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Audit type tabs
            AuditTypeTabRow(
                selectedType = uiState.auditType,
                onTypeSelected = viewModel::setAuditType,
                enabled = !uiState.isSubmitting
            )

            // Form content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState.auditType) {
                    AuditType.GROUND -> GroundAuditForm(uiState, viewModel)
                    AuditType.DCDB -> DcdbAuditForm(uiState, viewModel)
                    AuditType.TOWER -> TowerAuditForm(uiState, viewModel)
                    AuditType.EQUIPMENT -> EquipmentAuditForm(uiState, viewModel)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bottom submit bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Engineer info
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = OnSurfaceVariantLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Engineer: ${uiState.engineerName ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantLight
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::submit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit Audit", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditTypeTabRow(
    selectedType: AuditType,
    onTypeSelected: (AuditType) -> Unit,
    enabled: Boolean
) {
    ScrollableTabRow(
        selectedTabIndex = AuditType.entries.indexOf(selectedType),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = PrimaryGreen,
        edgePadding = 12.dp,
        divider = {}
    ) {
        AuditType.entries.forEach { type ->
            val selected = type == selectedType
            Tab(
                selected = selected,
                onClick = { if (enabled) onTypeSelected(type) },
                text = {
                    Text(
                        text = type.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) PrimaryGreen else OnSurfaceVariantLight
                    )
                },
                icon = {
                    Icon(
                        imageVector = auditTypeIcon(type),
                        contentDescription = null,
                        tint = if (selected) PrimaryGreen else OnSurfaceVariantLight.copy(alpha = 0.6f)
                    )
                }
            )
        }
    }
}

private fun auditTypeIcon(type: AuditType): ImageVector = when (type) {
    AuditType.GROUND -> Icons.Default.Landscape
    AuditType.DCDB -> Icons.Default.ElectricalServices
    AuditType.TOWER -> Icons.Default.Architecture
    AuditType.EQUIPMENT -> Icons.Default.DevicesOther
}

// ── Ground Audit Form ──────────────────────────────────────────────

@Composable
private fun GroundAuditForm(
    state: AuditUiState,
    viewModel: AuditViewModel
) {
    SectionHeader(title = "Site Perimeter", icon = Icons.Default.Landscape)
    SelectField(
        label = "Fence Condition",
        value = state.fenceCondition,
        options = listOf("Excellent", "Good", "Fair", "Poor", "Damaged"),
        onValueChange = viewModel::updateGroundFence
    )
    SelectField(
        label = "Gate & Lock Condition",
        value = state.gateLock,
        options = listOf("Secure", "Loose", "Damaged", "Missing Lock"),
        onValueChange = viewModel::updateGateLock
    )
    SelectField(
        label = "Drainage Condition",
        value = state.drainageCondition,
        options = listOf("Clear", "Partial Blockage", "Blocked", "Flooded"),
        onValueChange = viewModel::updateDrainage
    )
    SectionHeader(title = "Electrical Grounding", icon = Icons.Default.Bolt)
    TextFieldWithUnit(
        label = "Ground Resistance (Ω)",
        value = state.groundResistance,
        onValueChange = viewModel::updateGroundResistance,
        placeholder = "e.g. 5.2"
    )
    NotesField(value = state.groundNotes, onValueChange = viewModel::updateGroundNotes)
}

// ── DCDB Audit Form ────────────────────────────────────────────────

@Composable
private fun DcdbAuditForm(
    state: AuditUiState,
    viewModel: AuditViewModel
) {
    SectionHeader(title = "DCDB Details", icon = Icons.Default.ElectricalServices)
    SelectField(
        label = "DCDB Type",
        value = state.dcdbType,
        options = listOf("Main Panel", "Sub Panel", "Distribution Box", "Smart Panel"),
        onValueChange = viewModel::updateDcdbType
    )
    TextFieldWithUnit(
        label = "Capacity (A)",
        value = state.dcdbCapacity,
        onValueChange = viewModel::updateDcdbCapacity,
        placeholder = "e.g. 100"
    )
    SectionHeader(title = "Condition", icon = Icons.Default.Checklist)
    SelectField(
        label = "Cable Condition",
        value = state.cablesCondition,
        options = listOf("Excellent", "Good", "Worn", "Damaged", "Exposed"),
        onValueChange = viewModel::updateCablesCondition
    )
    SelectField(
        label = "Surge Protection",
        value = state.surgeProtection,
        options = listOf("Installed & Working", "Installed - Faulty", "Missing", "Expired"),
        onValueChange = viewModel::updateSurgeProtection
    )
    SelectField(
        label = "Cable Entry Sealed",
        value = state.cableEntrySealed,
        options = listOf("Yes - Sealed", "Partial Seal", "Open", "Damaged"),
        onValueChange = viewModel::updateCableEntrySealed
    )
    NotesField(value = state.dcdbNotes, onValueChange = viewModel::updateDcdbNotes)
}

// ── Tower Audit Form ────────────────────────────────────────────────

@Composable
private fun TowerAuditForm(
    state: AuditUiState,
    viewModel: AuditViewModel
) {
    SectionHeader(title = "Tower Structure", icon = Icons.Default.Architecture)
    SelectField(
        label = "Tower Type",
        value = state.towerType,
        options = listOf("Self-Supporting", "Guyed Mast", "Monopole", "Rooftop", "Hidden Tower"),
        onValueChange = viewModel::updateTowerType
    )
    TextFieldWithUnit(
        label = "Tower Height (m)",
        value = state.towerHeight,
        onValueChange = viewModel::updateTowerHeight,
        placeholder = "e.g. 45"
    )
    SectionHeader(title = "Structural Integrity", icon = Icons.Default.Construction)
    SelectField(
        label = "Structural Integrity",
        value = state.structuralIntegrity,
        options = listOf("Excellent", "Good", "Minor Issues", "Significant Damage", "Unsafe"),
        onValueChange = viewModel::updateStructuralIntegrity
    )
    SelectField(
        label = "Rust & Corrosion",
        value = state.rustCorrosion,
        options = listOf("None", "Surface Rust", "Moderate", "Severe"),
        onValueChange = viewModel::updateRustCorrosion
    )
    SelectField(
        label = "Bolt Condition",
        value = state.boltCondition,
        options = listOf("All Tight", "Minor Looseness", "Several Loose", "Missing Bolts"),
        onValueChange = viewModel::updateBoltCondition
    )
    SectionHeader(title = "Safety Equipment", icon = Icons.Default.HealthAndSafety)
    SelectField(
        label = "Lightning Rod",
        value = state.lightningRod,
        options = listOf("Present & Functional", "Present - Damaged", "Missing"),
        onValueChange = viewModel::updateLightningRod
    )
    SelectField(
        label = "Climb Safety",
        value = state.climbSafety,
        options = listOf("Safe", "Cable Worn", "Climbing Device Missing", "Unsafe"),
        onValueChange = viewModel::updateClimbSafety
    )
    SelectField(
        label = "Antenna Mounting",
        value = state.antennaMounting,
        options = listOf("Secure", "Minor Movement", "Loose", "Damaged"),
        onValueChange = viewModel::updateAntennaMounting
    )
    NotesField(value = state.towerNotes, onValueChange = viewModel::updateTowerNotes)
}

// ── Equipment Audit Form ────────────────────────────────────────────

@Composable
private fun EquipmentAuditForm(
    state: AuditUiState,
    viewModel: AuditViewModel
) {
    SectionHeader(title = "Equipment Details", icon = Icons.Default.DevicesOther)
    SelectField(
        label = "Cabinet Condition",
        value = state.cabinetCondition,
        options = listOf("Excellent", "Good", "Minor Damage", "Dented", "Open/Corrupted"),
        onValueChange = viewModel::updateCabinetCondition
    )
    TextFieldWithUnit(
        label = "Equipment Model",
        value = state.equipmentModel,
        onValueChange = viewModel::updateEquipmentModel,
        placeholder = "e.g. Huawei BTS 3900"
    )
    SectionHeader(title = "Power & Status", icon = Icons.Default.Power)
    SelectField(
        label = "Power Supply Status",
        value = state.powerSupplyStatus,
        options = listOf("Normal", "Warning", "Critical", "Failed", "Unknown"),
        onValueChange = viewModel::updatePowerSupplyStatus
    )
    TextFieldWithUnit(
        label = "Temperature (°C)",
        value = state.temperature,
        onValueChange = viewModel::updateTemperature,
        placeholder = "e.g. 38"
    )
    TextFieldWithUnit(
        label = "Uptime (hours)",
        value = state.uptime,
        onValueChange = viewModel::updateUptime,
        placeholder = "e.g. 8760"
    )
    TextFieldWithUnit(
        label = "Signal Strength (dBm)",
        value = state.signalStrength,
        onValueChange = viewModel::updateSignalStrength,
        placeholder = "e.g. -85"
    )
    SectionHeader(title = "Visual Inspection", icon = Icons.Default.Visibility)
    SelectField(
        label = "Cable Management",
        value = state.cableManagement,
        options = listOf("Neat", "Acceptable", "Messy", "Critical"),
        onValueChange = viewModel::updateCableManagement
    )
    SelectField(
        label = "LED Indicators",
        value = state.ledIndicators,
        options = listOf("All Normal", "Some Warning", "Critical Alerts", "All Off"),
        onValueChange = viewModel::updateLedIndicators
    )
    NotesField(value = state.equipmentNotes, onValueChange = viewModel::updateEquipmentNotes)
}

// ── Reusable Form Components ───────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = PrimaryGreen
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryGreen
        )
    }
}

@Composable
private fun SelectField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariantLight,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = value.ifEmpty { "Select..." },
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = PrimaryGreen.copy(alpha = 0.03f)
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = value == option,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PrimaryGreen
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextFieldWithUnit(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryGreen,
            focusedLabelColor = PrimaryGreen,
            focusedContainerColor = PrimaryGreen.copy(alpha = 0.03f)
        )
    )
}

@Composable
private fun NotesField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Notes & Observations") },
        placeholder = { Text("Add any additional observations...") },
        minLines = 3,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryGreen,
            focusedLabelColor = PrimaryGreen,
            focusedContainerColor = PrimaryGreen.copy(alpha = 0.03f)
        )
    )
}
