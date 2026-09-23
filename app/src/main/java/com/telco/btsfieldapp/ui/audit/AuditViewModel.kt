package com.telco.btsfieldapp.ui.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.AuditRepository
import com.telco.btsfieldapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuditType(val label: String, val key: String) {
    GROUND("Ground Audit", "ground"),
    DCDB("DCDB Audit", "dcdb"),
    TOWER("Tower Audit", "tower"),
    EQUIPMENT("Equipment Audit", "equipment")
}

data class AuditUiState(
    val auditType: AuditType = AuditType.GROUND,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null,
    val engineerName: String? = null,
    // Ground
    val fenceCondition: String = "",
    val gateLock: String = "",
    val groundResistance: String = "",
    val drainageCondition: String = "",
    val groundNotes: String = "",
    // DCDB
    val dcdbType: String = "",
    val dcdbCapacity: String = "",
    val cablesCondition: String = "",
    val surgeProtection: String = "",
    val cableEntrySealed: String = "",
    val dcdbNotes: String = "",
    // Tower
    val towerType: String = "",
    val towerHeight: String = "",
    val structuralIntegrity: String = "",
    val rustCorrosion: String = "",
    val boltCondition: String = "",
    val lightningRod: String = "",
    val climbSafety: String = "",
    val antennaMounting: String = "",
    val towerNotes: String = "",
    // Equipment
    val cabinetCondition: String = "",
    val equipmentModel: String = "",
    val powerSupplyStatus: String = "",
    val cableManagement: String = "",
    val ledIndicators: String = "",
    val temperature: String = "",
    val uptime: String = "",
    val signalStrength: String = "",
    val equipmentNotes: String = ""
)

@HiltViewModel
class AuditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val siteId: Long = savedStateHandle.get<Long>("siteId") ?: 0L

    private val auditTypeKey: String = savedStateHandle.get<String>("auditType") ?: "ground"

    private val _uiState = MutableStateFlow(
        AuditUiState(
            auditType = AuditType.entries.find { it.key == auditTypeKey } ?: AuditType.GROUND
        )
    )
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.userName.collect { name ->
                _uiState.update { it.copy(engineerName = name) }
            }
        }
    }

    fun updateGroundFence(v: String) = _uiState.update { it.copy(fenceCondition = v) }
    fun updateGateLock(v: String) = _uiState.update { it.copy(gateLock = v) }
    fun updateGroundResistance(v: String) = _uiState.update { it.copy(groundResistance = v) }
    fun updateDrainage(v: String) = _uiState.update { it.copy(drainageCondition = v) }
    fun updateGroundNotes(v: String) = _uiState.update { it.copy(groundNotes = v) }

    fun updateDcdbType(v: String) = _uiState.update { it.copy(dcdbType = v) }
    fun updateDcdbCapacity(v: String) = _uiState.update { it.copy(dcdbCapacity = v) }
    fun updateCablesCondition(v: String) = _uiState.update { it.copy(cablesCondition = v) }
    fun updateSurgeProtection(v: String) = _uiState.update { it.copy(surgeProtection = v) }
    fun updateCableEntrySealed(v: String) = _uiState.update { it.copy(cableEntrySealed = v) }
    fun updateDcdbNotes(v: String) = _uiState.update { it.copy(dcdbNotes = v) }

    fun updateTowerType(v: String) = _uiState.update { it.copy(towerType = v) }
    fun updateTowerHeight(v: String) = _uiState.update { it.copy(towerHeight = v) }
    fun updateStructuralIntegrity(v: String) = _uiState.update { it.copy(structuralIntegrity = v) }
    fun updateRustCorrosion(v: String) = _uiState.update { it.copy(rustCorrosion = v) }
    fun updateBoltCondition(v: String) = _uiState.update { it.copy(boltCondition = v) }
    fun updateLightningRod(v: String) = _uiState.update { it.copy(lightningRod = v) }
    fun updateClimbSafety(v: String) = _uiState.update { it.copy(climbSafety = v) }
    fun updateAntennaMounting(v: String) = _uiState.update { it.copy(antennaMounting = v) }
    fun updateTowerNotes(v: String) = _uiState.update { it.copy(towerNotes = v) }

    fun updateCabinetCondition(v: String) = _uiState.update { it.copy(cabinetCondition = v) }
    fun updateEquipmentModel(v: String) = _uiState.update { it.copy(equipmentModel = v) }
    fun updatePowerSupplyStatus(v: String) = _uiState.update { it.copy(powerSupplyStatus = v) }
    fun updateCableManagement(v: String) = _uiState.update { it.copy(cableManagement = v) }
    fun updateLedIndicators(v: String) = _uiState.update { it.copy(ledIndicators = v) }
    fun updateTemperature(v: String) = _uiState.update { it.copy(temperature = v) }
    fun updateUptime(v: String) = _uiState.update { it.copy(uptime = v) }
    fun updateSignalStrength(v: String) = _uiState.update { it.copy(signalStrength = v) }
    fun updateEquipmentNotes(v: String) = _uiState.update { it.copy(equipmentNotes = v) }

    fun setAuditType(type: AuditType) {
        _uiState.update { it.copy(auditType = type) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val data = buildAuditData(state)
            val result = auditRepository.createAudit(
                siteId = siteId,
                type = state.auditType.key,
                engineerName = state.engineerName ?: "Unknown",
                data = data
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, submitSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSubmitting = false, error = e.message) }
                }
            )
        }
    }

    private fun buildAuditData(state: AuditUiState): Map<String, Any> = when (state.auditType) {
        AuditType.GROUND -> mapOf(
            "fence_condition" to state.fenceCondition,
            "gate_lock" to state.gateLock,
            "ground_resistance" to state.groundResistance,
            "drainage_condition" to state.drainageCondition,
            "notes" to state.groundNotes
        )
        AuditType.DCDB -> mapOf(
            "dcdb_type" to state.dcdbType,
            "dcdb_capacity" to state.dcdbCapacity,
            "cables_condition" to state.cablesCondition,
            "surge_protection" to state.surgeProtection,
            "cable_entry_sealed" to state.cableEntrySealed,
            "notes" to state.dcdbNotes
        )
        AuditType.TOWER -> mapOf(
            "tower_type" to state.towerType,
            "tower_height" to state.towerHeight,
            "structural_integrity" to state.structuralIntegrity,
            "rust_corrosion" to state.rustCorrosion,
            "bolt_condition" to state.boltCondition,
            "lightning_rod" to state.lightningRod,
            "climb_safety" to state.climbSafety,
            "antenna_mounting" to state.antennaMounting,
            "notes" to state.towerNotes
        )
        AuditType.EQUIPMENT -> mapOf(
            "cabinet_condition" to state.cabinetCondition,
            "equipment_model" to state.equipmentModel,
            "power_supply_status" to state.powerSupplyStatus,
            "cable_management" to state.cableManagement,
            "led_indicators" to state.ledIndicators,
            "temperature" to state.temperature,
            "uptime" to state.uptime,
            "signal_strength" to state.signalStrength,
            "notes" to state.equipmentNotes
        )
    }
}
