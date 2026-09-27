package com.telco.btsfieldapp.ui.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.AuditRepository
import com.telco.btsfieldapp.data.repository.AuthRepository
import com.telco.btsfieldapp.data.repository.PhotoToUpload
import com.telco.btsfieldapp.data.repository.SiteRepository
import com.telco.btsfieldapp.data.repository.UploadProgress
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
import java.util.UUID
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
    val locationSummary: String = "Kampala",

    // Photo upload state
    val isUploadingPhotos: Boolean = false,
    val uploadTotal: Int = 0,
    val uploadCurrent: Int = 0,
    val uploadCurrentName: String = "",
    val uploadPhotoStatuses: Map<String, PhotoUploadStatus> = emptyMap()  // fieldName → status
)

enum class PhotoUploadStatus {
    PENDING, UPLOADING, DONE, FAILED
}

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

    // ── Helpers to build photo upload lists ────────────────────────────────────
    private fun buildGroundPhotoList(s: GroundEquipmentUiState, recordId: String): List<PhotoToUpload> =
        buildList {
            s.siteNamePlatePhotoPath?.let { add(PhotoToUpload(it, "ground", "site_name_plate_photo", recordId)) }
            s.gpsScreenshotPath?.let { add(PhotoToUpload(it, "ground", "gps_screenshot", recordId)) }
            s.sitePhotoPath?.let { add(PhotoToUpload(it, "ground", "site_photo", recordId)) }
            s.rruPhotoPaths.forEachIndexed { idx, p -> add(PhotoToUpload(p, "ground", "rru_photo_$idx", recordId)) }
            s.cabinetPhotoPaths.forEachIndexed { idx, p -> add(PhotoToUpload(p, "ground", "cabinet_photo_$idx", recordId)) }
            s.cabinetDimensionPhotoPaths.forEachIndexed { idx, p -> add(PhotoToUpload(p, "ground", "cabinet_dim_photo_$idx", recordId)) }
            s.nonActiveIduPhotoPaths.forEachIndexed { idx, p -> add(PhotoToUpload(p, "ground", "non_active_idu_photo_$idx", recordId)) }
            s.slabPhotoPaths.forEachIndexed { idx, p -> add(PhotoToUpload(p, "ground", "slab_photo_$idx", recordId)) }
            s.redundantPhotoPaths.forEachIndexed { idx, p -> add(PhotoToUpload(p, "ground", "redundant_photo_$idx", recordId)) }
        }

    /** Collect photo upload flow, update UI state, return map of fieldName → serverUrl */
    private suspend fun collectPhotoUploads(
        photosToUpload: List<PhotoToUpload>,
        onStarted: () -> Unit,
        onPhotoStarted: (Int, String) -> Unit,
        onPhotoDone: (String) -> Unit,
        onPhotoFailed: (String) -> Unit,
        onDone: () -> Unit
    ): Map<String, String> {
        if (photosToUpload.isEmpty()) return emptyMap()

        onStarted()
        var photoUrls = emptyMap<String, String>()

        auditRepository.uploadPhotos(siteId, photosToUpload).collect { progress ->
            when (progress) {
                is UploadProgress.Started -> { /* overall count already set in onStarted */ }
                is UploadProgress.PhotoStarted -> {
                    onPhotoStarted(progress.index, progress.fieldName)
                }
                is UploadProgress.PhotoDone -> {
                    photoUrls = photoUrls + (progress.fieldName to progress.serverUrl)
                    onPhotoDone(progress.fieldName)
                }
                is UploadProgress.PhotoFailed -> {
                    onPhotoFailed(progress.fieldName)
                }
                is UploadProgress.Done -> {
                    photoUrls = progress.photoUrls
                    onDone()
                }
            }
        }
        return photoUrls
    }

    // ── Submit ────────────────────────────────────────────────────────────────
    fun submit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }

            val s = _uiState.value
            val engineerName = s.technicianName.ifBlank { "Unknown" }
            val recordId = UUID.randomUUID().toString()

            val photosToUpload = buildGroundPhotoList(s, recordId)

            // Upload photos
            var photoUrls = emptyMap<String, String>()
            if (photosToUpload.isNotEmpty()) {
                photoUrls = collectPhotoUploads(
                    photosToUpload = photosToUpload,
                    onStarted = {
                        _uiState.update { it.copy(
                            isUploadingPhotos = true,
                            uploadTotal = photosToUpload.size,
                            uploadCurrent = 0,
                            uploadCurrentName = "",
                            uploadPhotoStatuses = photosToUpload.associate { it.fieldName to PhotoUploadStatus.PENDING }
                        )}
                    },
                    onPhotoStarted = { idx, fieldName ->
                        _uiState.update { it.copy(
                            uploadCurrent = idx + 1,
                            uploadCurrentName = fieldName,
                            uploadPhotoStatuses = it.uploadPhotoStatuses + (fieldName to PhotoUploadStatus.UPLOADING)
                        )}
                    },
                    onPhotoDone = { fieldName ->
                        _uiState.update { it.copy(uploadPhotoStatuses = it.uploadPhotoStatuses + (fieldName to PhotoUploadStatus.DONE)) }
                    },
                    onPhotoFailed = { fieldName ->
                        _uiState.update { it.copy(uploadPhotoStatuses = it.uploadPhotoStatuses + (fieldName to PhotoUploadStatus.FAILED)) }
                    },
                    onDone = {
                        _uiState.update { it.copy(isUploadingPhotos = false) }
                    }
                )

                val allFailed = _uiState.value.uploadPhotoStatuses.values.all { it == PhotoUploadStatus.FAILED }
                if (allFailed && photoUrls.isEmpty()) {
                    _uiState.update { it.copy(isSubmitting = false, isUploadingPhotos = false, submitError = "All photo uploads failed. Check your connection.") }
                    return@launch
                }
            }

            // Submit with server photo URLs
            val payload = buildPayload(s, recordId, photoUrls)
            auditRepository.submitAudit(
                siteId = siteId,
                type = "ground",
                engineerName = engineerName,
                data = payload,
                action = "submit"
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, isUploadingPhotos = false) }
                    _events.emit(GroundEquipmentEvent.SubmitSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSubmitting = false, isUploadingPhotos = false, submitError = e.message ?: "Submission failed") }
                }
            )
        }
    }

    // ── Save Draft ─────────────────────────────────────────────────────────────
    fun saveDraft() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingDraft = true, submitError = null) }

            val s = _uiState.value
            val engineerName = s.technicianName.ifBlank { "Unknown" }
            val recordId = UUID.randomUUID().toString()

            val photosToUpload = buildGroundPhotoList(s, recordId)

            // Upload photos
            var photoUrls = emptyMap<String, String>()
            if (photosToUpload.isNotEmpty()) {
                photoUrls = collectPhotoUploads(
                    photosToUpload = photosToUpload,
                    onStarted = {
                        _uiState.update { it.copy(
                            isUploadingPhotos = true,
                            uploadTotal = photosToUpload.size,
                            uploadCurrent = 0,
                            uploadCurrentName = "",
                            uploadPhotoStatuses = photosToUpload.associate { it.fieldName to PhotoUploadStatus.PENDING }
                        )}
                    },
                    onPhotoStarted = { idx, fieldName ->
                        _uiState.update { it.copy(
                            uploadCurrent = idx + 1,
                            uploadCurrentName = fieldName,
                            uploadPhotoStatuses = it.uploadPhotoStatuses + (fieldName to PhotoUploadStatus.UPLOADING)
                        )}
                    },
                    onPhotoDone = { fieldName ->
                        _uiState.update { it.copy(uploadPhotoStatuses = it.uploadPhotoStatuses + (fieldName to PhotoUploadStatus.DONE)) }
                    },
                    onPhotoFailed = { fieldName ->
                        _uiState.update { it.copy(uploadPhotoStatuses = it.uploadPhotoStatuses + (fieldName to PhotoUploadStatus.FAILED)) }
                    },
                    onDone = {
                        _uiState.update { it.copy(isUploadingPhotos = false) }
                    }
                )

                val allFailed = _uiState.value.uploadPhotoStatuses.values.all { it == PhotoUploadStatus.FAILED }
                if (allFailed && photoUrls.isEmpty()) {
                    _uiState.update { it.copy(isSavingDraft = false, isUploadingPhotos = false, submitError = "All photo uploads failed. Check your connection.") }
                    return@launch
                }
            }

            // Save with server photo URLs
            val payload = buildPayload(s, recordId, photoUrls)
            auditRepository.submitAudit(
                siteId = siteId,
                type = "ground",
                engineerName = engineerName,
                data = payload,
                action = "save"
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSavingDraft = false, isUploadingPhotos = false) }
                    _events.emit(GroundEquipmentEvent.SaveDraftSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSavingDraft = false, isUploadingPhotos = false, submitError = e.message ?: "Save draft failed") }
                }
            )
        }
    }

    private fun buildPayload(s: GroundEquipmentUiState, recordId: String, photoUrls: Map<String, String>): Map<String, Any> = buildMap {
        put("type", "ground")
        put("id", recordId)
        put("atc_id", s.atcId)
        s.siteNamePlatePhotoPath?.let { put("site_name_plate_photo", photoUrls["site_name_plate_photo"] ?: it) }
        put("survey_date", s.surveyDate)
        put("technician_name", s.technicianName)
        put("technician_contacts", s.technicianContacts)
        put("contractor_name", s.contractorName)
        put("latitude", s.latitude)
        put("longitude", s.longitude)
        put("gps_accuracy", s.gpsAccuracy)
        put("altitude", s.altitude)
        s.gpsScreenshotPath?.let { put("gps_screenshot", photoUrls["gps_screenshot"] ?: it) }
        put("tower_type", s.towerType)
        put("tower_height", s.towerHeight)
        put("building_height", s.buildingHeight)
        put("total_height", s.totalHeight)
        put("site_indoor_outdoor", s.siteIndoorOutdoor)
        put("no_of_tenants", s.noOfTenants)
        put("other_tenants", s.otherTenants.joinToString(", "))
        s.sitePhotoPath?.let { put("site_photo", photoUrls["site_photo"] ?: it) }
        put("has_grid", s.hasGrid)
        put("has_dg", s.hasDG)
        put("has_solar", s.hasSolar)
        put("grid_distance_to_3phase", s.gridDistanceTo3Phase)
        put("guard_at_site", s.guardAtSite)
        put("rru_type", s.rruType)
        put("rru_count", s.rruCount)
        put("rru_photos", s.rruPhotoPaths.mapIndexed { idx, p -> photoUrls["rru_photo_$idx"] ?: p }.joinToString("|"))
        put("cabinet_types", s.cabinetTypes)
        put("cabinet_count", s.cabinetCount)
        put("cabinet_photos", s.cabinetPhotoPaths.mapIndexed { idx, p -> photoUrls["cabinet_photo_$idx"] ?: p }.joinToString("|"))
        put("equipment_labelled", s.equipmentLabelled)
        put("cabinet_comments", s.cabinetComments)
        put("cabinet_dimensions_lxwxh", s.cabinetDimensionsLxW)
        put("cabinet_dimension_photos", s.cabinetDimensionPhotoPaths.mapIndexed { idx, p -> photoUrls["cabinet_dim_photo_$idx"] ?: p }.joinToString("|"))
        put("active_idu_types", s.activeIduTypes)
        put("non_active_idu_types", s.nonActiveIduTypes)
        put("non_active_idu_count", s.nonActiveIduCount)
        put("non_active_idu_photos", s.nonActiveIduPhotoPaths.mapIndexed { idx, p -> photoUrls["non_active_idu_photo_$idx"] ?: p }.joinToString("|"))
        put("slab_dimensions", s.slabDimensions)
        put("slab_photos", s.slabPhotoPaths.mapIndexed { idx, p -> photoUrls["slab_photo_$idx"] ?: p }.joinToString("|"))
        put("redundant_equipment_count", s.redundantEquipmentCount)
        put("redundant_item_name", s.redundantItemName)
        put("redundant_photos", s.redundantPhotoPaths.mapIndexed { idx, p -> photoUrls["redundant_photo_$idx"] ?: p }.joinToString("|"))
        put("trm_media_fiber", s.isOnFiber?.toString() ?: "")
        put("overall_remarks", s.overallRemarks)
    }
}
