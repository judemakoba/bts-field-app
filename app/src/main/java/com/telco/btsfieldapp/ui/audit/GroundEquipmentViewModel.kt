package com.telco.btsfieldapp.ui.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.AuditRepository
import com.telco.btsfieldapp.data.repository.AuthRepository
import com.telco.btsfieldapp.data.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class GroundEquipmentUiState(
    // Section 1: Site Identification
    val atcId: String = "",
    val siteNamePlatePhotoPath: String? = null,

    // Section 2: Survey Details (auto-filled)
    val surveyDate: String = "",
    val technicianName: String = "",
    val technicianContacts: String = "",
    val contractorName: String = "Innovis",

    // Section 3: Tower & Site Info
    val latitude: String = "",
    val longitude: String = "",
    val gpsAccuracy: String = "",
    val altitude: String = "",
    val gpsScreenshotPath: String? = null,
    val towerType: String = "",        // GBT | RTT | RTP
    val towerHeight: String = "",
    val buildingHeight: String = "0",
    val totalHeight: String = "0",
    val siteIndoorOutdoor: String = "", // Indoor | Outdoor
    val noOfTenants: String = "",
    val otherTenants: List<String> = emptyList(), // Lyca | MTN | UTL | Savanna | Other
    val sitePhotoPath: String? = null,

    // Section 4: Power Infrastructure
    val hasGrid: Boolean = false,
    val hasDG: Boolean = false,
    val hasSolar: Boolean = false,
    val gridDistanceTo3Phase: String = "",

    // Section 5: RRU & Cabinets
    val guardAtSite: Boolean = false,
    val rruType: String = "",
    val rruPhotoPaths: List<String> = emptyList(),    // up to 4
    val rruCount: String = "",
    val cabinetTypes: String = "",
    val cabinetPhotoPaths: List<String> = emptyList(), // up to 20
    val cabinetCount: String = "",
    val equipmentLabelled: Boolean = false,
    val cabinetComments: String = "",
    val cabinetDimensionsLxW: String = "",
    val cabinetDimensionPhotoPaths: List<String> = emptyList(), // L, W, H — 3
    val activeIduTypes: String = "",
    val nonActiveIduTypes: String = "",
    val nonActiveIduCount: String = "",
    val nonActiveIduPhotoPaths: List<String> = emptyList(), // up to 5

    // Section 6: Slab
    val slabDimensions: String = "",
    val slabPhotoPaths: List<String> = emptyList(), // overview + 2 dims — 3

    // Section 7: Redundant
    val redundantEquipmentCount: String = "",
    val redundantItemName: String = "",
    val redundantPhotoPaths: List<String> = emptyList(), // up to 3

    // Section 8: Media & Remarks
    val isOnFiber: Boolean? = null,
    val overallRemarks: String = "",

    // Form state
    val isSubmitting: Boolean = false,
    val isSavingDraft: Boolean = false,
    val submitError: String? = null,
    val expandedSections: Set<Int> = setOf(0), // section 0 open by default
    val siteId: String = "",
    val siteName: String = "",
    val locationSummary: String = "Kampala"
)

sealed class GroundEquipmentEvent {
    data object SubmitSuccess : GroundEquipmentEvent()
    data object SaveDraftSuccess : GroundEquipmentEvent()
}

@HiltViewModel
class GroundEquipmentViewModel @Inject constructor(
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository,
    private val siteRepository: SiteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val siteId: String = savedStateHandle.get<String>("siteId") ?: ""

    private val _uiState = MutableStateFlow(GroundEquipmentUiState(siteId = siteId))
    val uiState: StateFlow<GroundEquipmentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GroundEquipmentEvent>()
    val events: SharedFlow<GroundEquipmentEvent> = _events.asSharedFlow()

    init {
        autoFillFromAccount()
        loadSiteMetadata()
    }

    private fun loadSiteMetadata() {
        viewModelScope.launch {
            try {
                val site = siteRepository.getSiteById(siteId)
                _uiState.update { s ->
                    s.copy(
                        siteName = site?.name ?: "",
                        locationSummary = site?.address?.takeIf { it.isNotBlank() } ?: "Kampala"
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun autoFillFromAccount() {
        viewModelScope.launch {
            val userName = authRepository.userName.first() ?: ""
            val userEmail = authRepository.userEmail.first() ?: ""
            val now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now())

            _uiState.update {
                it.copy(
                    technicianName = userName,
                    technicianContacts = userEmail,
                    surveyDate = now
                )
            }
        }
    }

    // ── Section 0: Site Identification ────────────────────────────────────────
    fun onAtcIdChange(value: String) = _uiState.update { it.copy(atcId = value) }
    fun onSiteNamePlatePhoto(path: String) = _uiState.update { it.copy(siteNamePlatePhotoPath = path) }

    // ── Section 2: Survey Details ──────────────────────────────────────────────
    fun onTechnicianNameChange(value: String) = _uiState.update { it.copy(technicianName = value) }
    fun onTechnicianContactsChange(value: String) = _uiState.update { it.copy(technicianContacts = value) }

    // ── Section 3: Tower & Site Info ───────────────────────────────────────────
    fun onLatitudeChange(value: String) = _uiState.update { it.copy(latitude = value) }
    fun onLongitudeChange(value: String) = _uiState.update { it.copy(longitude = value) }
    fun onGpsAccuracyChange(value: String) = _uiState.update { it.copy(gpsAccuracy = value) }
    fun onAltitudeChange(value: String) = _uiState.update { it.copy(altitude = value) }
    fun onGpsScreenshot(path: String) = _uiState.update { it.copy(gpsScreenshotPath = path) }
    fun onTowerTypeChange(value: String) = _uiState.update { it.copy(towerType = value) }
    fun onTowerHeightChange(value: String) {
        val th = value.toIntOrNull() ?: 0
        val bh = _uiState.value.buildingHeight.toIntOrNull() ?: 0
        _uiState.update { it.copy(towerHeight = value, totalHeight = (th + bh).toString()) }
    }
    fun onBuildingHeightChange(value: String) {
        val bh = value.toIntOrNull() ?: 0
        val th = _uiState.value.towerHeight.toIntOrNull() ?: 0
        _uiState.update { it.copy(buildingHeight = value, totalHeight = (th + bh).toString()) }
    }
    fun onSiteIndoorOutdoorChange(value: String) = _uiState.update { it.copy(siteIndoorOutdoor = value) }
    fun onNoOfTenantsChange(value: String) = _uiState.update { it.copy(noOfTenants = value) }
    fun onOtherTenantsToggle(tenant: String) {
        _uiState.update { state ->
            val current = state.otherTenants.toMutableList()
            if (current.contains(tenant)) current.remove(tenant) else current.add(tenant)
            state.copy(otherTenants = current)
        }
    }
    fun onSitePhoto(path: String) = _uiState.update { it.copy(sitePhotoPath = path) }

    // ── Section 4: Power Infrastructure ─────────────────────────────────────────
    fun onHasGridChange(value: Boolean) = _uiState.update { it.copy(hasGrid = value) }
    fun onHasDGChange(value: Boolean) = _uiState.update { it.copy(hasDG = value) }
    fun onHasSolarChange(value: Boolean) = _uiState.update { it.copy(hasSolar = value) }
    fun onGridDistanceChange(value: String) = _uiState.update { it.copy(gridDistanceTo3Phase = value) }

    // ── Section 5: RRU & Cabinets ─────────────────────────────────────────────
    fun onGuardAtSiteChange(value: Boolean) = _uiState.update { it.copy(guardAtSite = value) }
    fun onRruTypeChange(value: String) = _uiState.update { it.copy(rruType = value) }
    fun onRruCountChange(value: String) = _uiState.update { it.copy(rruCount = value) }
    fun addRruPhoto(path: String) {
        _uiState.update { state ->
            if (state.rruPhotoPaths.size < 4) state.copy(rruPhotoPaths = state.rruPhotoPaths + path)
            else state
        }
    }
    fun removeRruPhoto(path: String) = _uiState.update { s -> s.copy(rruPhotoPaths = s.rruPhotoPaths.filter { it != path }) }
    fun onCabinetTypesChange(value: String) = _uiState.update { it.copy(cabinetTypes = value) }
    fun onCabinetCountChange(value: String) = _uiState.update { it.copy(cabinetCount = value) }
    fun addCabinetPhoto(path: String) {
        _uiState.update { state ->
            if (state.cabinetPhotoPaths.size < 20) state.copy(cabinetPhotoPaths = state.cabinetPhotoPaths + path)
            else state
        }
    }
    fun removeCabinetPhoto(path: String) = _uiState.update { s -> s.copy(cabinetPhotoPaths = s.cabinetPhotoPaths.filter { it != path }) }
    fun onEquipmentLabelledChange(value: Boolean) = _uiState.update { it.copy(equipmentLabelled = value) }
    fun onCabinetCommentsChange(value: String) = _uiState.update { it.copy(cabinetComments = value) }
    fun onCabinetDimensionsChange(value: String) = _uiState.update { it.copy(cabinetDimensionsLxW = value) }
    fun addCabinetDimensionPhoto(path: String) {
        _uiState.update { state ->
            if (state.cabinetDimensionPhotoPaths.size < 3)
                state.copy(cabinetDimensionPhotoPaths = state.cabinetDimensionPhotoPaths + path)
            else state
        }
    }
    fun removeCabinetDimensionPhoto(path: String) =
        _uiState.update { s -> s.copy(cabinetDimensionPhotoPaths = s.cabinetDimensionPhotoPaths.filter { it != path }) }
    fun onActiveIduTypesChange(value: String) = _uiState.update { it.copy(activeIduTypes = value) }
    fun onNonActiveIduTypesChange(value: String) = _uiState.update { it.copy(nonActiveIduTypes = value) }
    fun onNonActiveIduCountChange(value: String) = _uiState.update { it.copy(nonActiveIduCount = value) }
    fun addNonActiveIduPhoto(path: String) {
        _uiState.update { state ->
            if (state.nonActiveIduPhotoPaths.size < 5)
                state.copy(nonActiveIduPhotoPaths = state.nonActiveIduPhotoPaths + path)
            else state
        }
    }
    fun removeNonActiveIduPhoto(path: String) =
        _uiState.update { s -> s.copy(nonActiveIduPhotoPaths = s.nonActiveIduPhotoPaths.filter { it != path }) }

    // ── Section 6: Slab ────────────────────────────────────────────────────────
    fun onSlabDimensionsChange(value: String) = _uiState.update { it.copy(slabDimensions = value) }
    fun addSlabPhoto(path: String) {
        _uiState.update { state ->
            if (state.slabPhotoPaths.size < 3) state.copy(slabPhotoPaths = state.slabPhotoPaths + path)
            else state
        }
    }
    fun removeSlabPhoto(path: String) = _uiState.update { s -> s.copy(slabPhotoPaths = s.slabPhotoPaths.filter { it != path }) }

    // ── Section 7: Redundant ───────────────────────────────────────────────────
    fun onRedundantCountChange(value: String) = _uiState.update { it.copy(redundantEquipmentCount = value) }
    fun onRedundantItemNameChange(value: String) = _uiState.update { it.copy(redundantItemName = value) }
    fun addRedundantPhoto(path: String) {
        _uiState.update { state ->
            if (state.redundantPhotoPaths.size < 3) state.copy(redundantPhotoPaths = state.redundantPhotoPaths + path)
            else state
        }
    }
    fun removeRedundantPhoto(path: String) = _uiState.update { s -> s.copy(redundantPhotoPaths = s.redundantPhotoPaths.filter { it != path }) }

    // ── Section 8: Media & Remarks ────────────────────────────────────────────
    fun onIsOnFiberChange(value: Boolean?) = _uiState.update { it.copy(isOnFiber = value) }
    fun onOverallRemarksChange(value: String) = _uiState.update { it.copy(overallRemarks = value) }

    // ── Section expand/collapse ────────────────────────────────────────────────
    fun toggleSection(index: Int) {
        _uiState.update { state ->
            val expanded = state.expandedSections.toMutableSet()
            if (expanded.contains(index)) expanded.remove(index) else expanded.add(index)
            state.copy(expandedSections = expanded)
        }
    }

    // ── Submit ────────────────────────────────────────────────────────────────
    fun submit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }

            val s = _uiState.value
            val engineerName = s.technicianName.ifBlank { "Unknown" }

            val payload = buildMap<String, Any> {
                put("type", "ground_equipment")

                // Section 1
                put("atc_id", s.atcId)
                s.siteNamePlatePhotoPath?.let { put("site_name_plate_photo", it) }

                // Section 2
                put("survey_date", s.surveyDate)
                put("technician_name", s.technicianName)
                put("technician_contacts", s.technicianContacts)
                put("contractor_name", s.contractorName)

                // Section 3
                put("latitude", s.latitude)
                put("longitude", s.longitude)
                put("gps_accuracy", s.gpsAccuracy)
                put("altitude", s.altitude)
                s.gpsScreenshotPath?.let { put("gps_screenshot", it) }
                put("tower_type", s.towerType)
                put("tower_height", s.towerHeight)
                put("building_height", s.buildingHeight)
                put("total_height", s.totalHeight)
                put("site_indoor_outdoor", s.siteIndoorOutdoor)
                put("no_of_tenants", s.noOfTenants)
                put("other_tenants", s.otherTenants.joinToString(", "))
                s.sitePhotoPath?.let { put("site_photo", it) }

                // Section 4
                put("has_grid", s.hasGrid)
                put("has_dg", s.hasDG)
                put("has_solar", s.hasSolar)
                put("grid_distance_to_3phase", s.gridDistanceTo3Phase)

                // Section 5
                put("guard_at_site", s.guardAtSite)
                put("rru_type", s.rruType)
                put("rru_count", s.rruCount)
                put("rru_photos", s.rruPhotoPaths.joinToString("|"))
                put("cabinet_types", s.cabinetTypes)
                put("cabinet_count", s.cabinetCount)
                put("cabinet_photos", s.cabinetPhotoPaths.joinToString("|"))
                put("equipment_labelled", s.equipmentLabelled)
                put("cabinet_comments", s.cabinetComments)
                put("cabinet_dimensions_lxwxh", s.cabinetDimensionsLxW)
                put("cabinet_dimension_photos", s.cabinetDimensionPhotoPaths.joinToString("|"))
                put("active_idu_types", s.activeIduTypes)
                put("non_active_idu_types", s.nonActiveIduTypes)
                put("non_active_idu_count", s.nonActiveIduCount)
                put("non_active_idu_photos", s.nonActiveIduPhotoPaths.joinToString("|"))

                // Section 6
                put("slab_dimensions", s.slabDimensions)
                put("slab_photos", s.slabPhotoPaths.joinToString("|"))

                // Section 7
                put("redundant_equipment_count", s.redundantEquipmentCount)
                put("redundant_item_name", s.redundantItemName)
                put("redundant_photos", s.redundantPhotoPaths.joinToString("|"))

                // Section 8
                put("trm_media_fiber", s.isOnFiber?.toString() ?: "")
                put("overall_remarks", s.overallRemarks)
            }

            auditRepository.submitAudit(
                siteId = siteId,
                type = "ground",
                engineerName = engineerName,
                data = payload,
                action = "submit"
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.emit(GroundEquipmentEvent.SubmitSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSubmitting = false, submitError = e.message ?: "Submission failed") }
                }
            )
        }
    }

    fun saveDraft() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingDraft = true, submitError = null) }
            val s = _uiState.value
            val engineerName = s.technicianName.ifBlank { "Unknown" }

            val payload = buildMap<String, Any> {
                put("type", "ground_equipment")
                put("atc_id", s.atcId)
                s.siteNamePlatePhotoPath?.let { put("site_name_plate_photo", it) }
                put("survey_date", s.surveyDate)
                put("technician_name", s.technicianName)
                put("technician_contacts", s.technicianContacts)
                put("contractor_name", s.contractorName)
                put("latitude", s.latitude)
                put("longitude", s.longitude)
                put("gps_accuracy", s.gpsAccuracy)
                put("altitude", s.altitude)
                s.gpsScreenshotPath?.let { put("gps_screenshot", it) }
                put("tower_type", s.towerType)
                put("tower_height", s.towerHeight)
                put("building_height", s.buildingHeight)
                put("total_height", s.totalHeight)
                put("site_indoor_outdoor", s.siteIndoorOutdoor)
                put("no_of_tenants", s.noOfTenants)
                put("other_tenants", s.otherTenants.joinToString(", "))
                s.sitePhotoPath?.let { put("site_photo", it) }
                put("has_grid", s.hasGrid)
                put("has_dg", s.hasDG)
                put("has_solar", s.hasSolar)
                put("grid_distance_to_3phase", s.gridDistanceTo3Phase)
                put("guard_at_site", s.guardAtSite)
                put("rru_type", s.rruType)
                put("rru_count", s.rruCount)
                put("rru_photos", s.rruPhotoPaths.joinToString("|"))
                put("cabinet_types", s.cabinetTypes)
                put("cabinet_count", s.cabinetCount)
                put("cabinet_photos", s.cabinetPhotoPaths.joinToString("|"))
                put("equipment_labelled", s.equipmentLabelled)
                put("cabinet_comments", s.cabinetComments)
                put("cabinet_dimensions_lxwxh", s.cabinetDimensionsLxW)
                put("cabinet_dimension_photos", s.cabinetDimensionPhotoPaths.joinToString("|"))
                put("active_idu_types", s.activeIduTypes)
                put("non_active_idu_types", s.nonActiveIduTypes)
                put("non_active_idu_count", s.nonActiveIduCount)
                put("non_active_idu_photos", s.nonActiveIduPhotoPaths.joinToString("|"))
                put("slab_dimensions", s.slabDimensions)
                put("slab_photos", s.slabPhotoPaths.joinToString("|"))
                put("redundant_equipment_count", s.redundantEquipmentCount)
                put("redundant_item_name", s.redundantItemName)
                put("redundant_photos", s.redundantPhotoPaths.joinToString("|"))
                put("trm_media_fiber", s.isOnFiber?.toString() ?: "")
                put("overall_remarks", s.overallRemarks)
            }

            auditRepository.submitAudit(
                siteId = siteId,
                type = "ground",
                engineerName = engineerName,
                data = payload,
                action = "save"
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSavingDraft = false) }
                    _events.emit(GroundEquipmentEvent.SaveDraftSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSavingDraft = false, submitError = e.message ?: "Save draft failed") }
                }
            )
        }
    }
}
