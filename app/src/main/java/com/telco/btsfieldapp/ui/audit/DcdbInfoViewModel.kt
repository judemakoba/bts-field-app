package com.telco.btsfieldapp.ui.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.AuditRepository
import com.telco.btsfieldapp.data.repository.AuthRepository
import com.telco.btsfieldapp.data.repository.PhotoToUpload
import com.telco.btsfieldapp.data.repository.SiteRepository
import com.telco.btsfieldapp.data.repository.UploadProgress
import com.telco.btsfieldapp.domain.model.DcduConnection
import com.telco.btsfieldapp.domain.model.DcduSlot
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

data class DcdbInfoUiState(
    // ── DCDB Sub-section ──────────────────────────────────────────
    val gridDistanceTo3Phase: String = "",

    // Non-Priority
    val npCableSizeDcdb: String = "",
    val npBreaker1Mcb: String = "",
    val npDcdus: List<DcduSlot> = listOf(
        DcduSlot("DCDU1", "", ""),
        DcduSlot("DCDU2", "", ""),
        DcduSlot("DCDU3", "", "")
    ),
    val npSectionPhotoPath: String? = null,
    val npLoadMeasurement: String = "",
    val npLoadPhotoPath: String? = null,
    val npLoadMeasuredTime: String = "",

    // Priority
    val pCableSizeDcdb: String = "",
    val pBreaker1Mcb: String = "",
    val pDcdus: List<DcduSlot> = listOf(
        DcduSlot("DCDU1", "", ""),
        DcduSlot("DCDU2", "", ""),
        DcduSlot("DCDU3", "", "")
    ),
    val pSectionPhotoPath: String? = null,
    val pLoadMeasurement: String = "",
    val pLoadPhotoPath: String? = null,
    val pLoadMeasuredTime: String = "",

    // ── DCDU Sub-section ─────────────────────────────────────────
    val npDcdusConnections: List<DcduConnection> = listOf(
        DcduConnection("np1", "", null),
        DcduConnection("np2", "", null),
        DcduConnection("np3", "", null)
    ),
    val pDcdusConnections: List<DcduConnection> = listOf(
        DcduConnection("p1", "", null),
        DcduConnection("p2", "", null),
        DcduConnection("p3", "", null)
    ),
    val totalDcdUCount: String = "0",

    // ── RRU Sub-section ──────────────────────────────────────────
    val rruCount: String = "",
    val rruPowerCableCount: String = "",
    val rruPowerCableMissing: String = "",
    val rruPowerCableLengthPerRun: String = "",
    val rruPowerCableTotalMissing: String = "",
    val rruEarthingCableCount: String = "",
    val rruEarthingCableMissing: String = "",
    val rruEarthingCableLengthPerRun: String = "",

    // ── AAU Sub-section ──────────────────────────────────────────
    val aauCount: String = "",
    val aauPowerCableCount: String = "",
    val aauPowerCableMissing: String = "",
    val aauPowerCableLengthPerRun: String = "",
    val aauPowerCableTotalMissing: String = "",
    val aauEarthingCableCount: String = "",
    val aauEarthingCableMissing: String = "",
    val aauEarthingCableLengthPerRun: String = "",

    // ── BTS Earthing Sub-section ────────────────────────────────
    val btsEarthingTotal: String = "",
    val btsEarthingCableCount: String = "",
    val btsEarthingCableMissing: String = "",
    val btsEarthingLengthPerRun: String = "",
    val btsEarthingConnection: String = "",
    val btsEarthingTotalMissing: String = "",

    // ── Form state ───────────────────────────────────────────────
    val isSubmitting: Boolean = false,
    val isSavingDraft: Boolean = false,
    val submitError: String? = null,
    val expandedSections: Set<Int> = setOf(0),
    val siteId: String = "",
    val siteName: String = "",
    val locationSummary: String = "Kampala",

    // Photo upload state
    val isUploadingPhotos: Boolean = false,
    val uploadTotal: Int = 0,
    val uploadCurrent: Int = 0,
    val uploadCurrentName: String = "",
    val uploadPhotoStatuses: Map<String, PhotoUploadStatus> = emptyMap(),
    val uploadedPhotoPaths: Set<String> = emptySet()
)

sealed class DcdbInfoEvent {
    data object SubmitSuccess : DcdbInfoEvent()
    data object SaveDraftSuccess : DcdbInfoEvent()
}

@HiltViewModel
class DcdbInfoViewModel @Inject constructor(
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository,
    private val siteRepository: SiteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val siteId: String = savedStateHandle.get<String>("siteId") ?: ""

    private val _uiState = MutableStateFlow(DcdbInfoUiState(siteId = siteId))
    val uiState: StateFlow<DcdbInfoUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DcdbInfoEvent>()
    val events: SharedFlow<DcdbInfoEvent> = _events.asSharedFlow()

    init {
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
            } catch (_: Exception) { }
        }
    }

    // ── DCDB Non-Priority ─────────────────────────────────────────────────────
    fun onGridDistanceChange(v: String) = _uiState.update { it.copy(gridDistanceTo3Phase = v) }
    fun onNpCableSizeDcdbChange(v: String) = _uiState.update { it.copy(npCableSizeDcdb = v) }
    fun onNpBreaker1McbChange(v: String) = _uiState.update { it.copy(npBreaker1Mcb = v) }
    fun onNpSectionPhoto(path: String) = _uiState.update { it.copy(npSectionPhotoPath = path) }
    fun onNpLoadMeasurementChange(v: String) = _uiState.update { it.copy(npLoadMeasurement = v) }
    fun onNpLoadPhoto(path: String) {
        val time = DateTimeFormatter.ofPattern("hh:mm:ss a")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        _uiState.update { it.copy(npLoadPhotoPath = path, npLoadMeasuredTime = time) }
    }
    fun onNpDcduCableSizeChange(idx: Int, v: String) {
        _uiState.update { s ->
            val list = s.npDcdus.toMutableList()
            if (idx < list.size) list[idx] = list[idx].copy(cableSize = v)
            s.copy(npDcdus = list)
        }
    }
    fun onNpDcduBreakerChange(idx: Int, v: String) {
        _uiState.update { s ->
            val list = s.npDcdus.toMutableList()
            if (idx < list.size) list[idx] = list[idx].copy(breakerRating = v)
            s.copy(npDcdus = list)
        }
    }
    fun addNpDcdu() {
        _uiState.update { s ->
            val next = s.npDcdus.size + 1
            s.copy(npDcdus = s.npDcdus + DcduSlot("DCDU$next", "", ""))
        }
    }
    fun removeNpDcdu(idx: Int) {
        _uiState.update { s ->
            if (s.npDcdus.size <= 1) return@update s
            val list = s.npDcdus.toMutableList().apply { removeAt(idx) }
            s.copy(npDcdus = list)
        }
    }

    // ── DCDB Priority ───────────────────────────────────────────────────────
    fun onPCableSizeDcdbChange(v: String) = _uiState.update { it.copy(pCableSizeDcdb = v) }
    fun onPBreaker1McbChange(v: String) = _uiState.update { it.copy(pBreaker1Mcb = v) }
    fun onPSectionPhoto(path: String) = _uiState.update { it.copy(pSectionPhotoPath = path) }
    fun onPLoadMeasurementChange(v: String) = _uiState.update { it.copy(pLoadMeasurement = v) }
    fun onPLoadPhoto(path: String) {
        val time = DateTimeFormatter.ofPattern("hh:mm:ss a")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        _uiState.update { it.copy(pLoadPhotoPath = path, pLoadMeasuredTime = time) }
    }
    fun onPDcduCableSizeChange(idx: Int, v: String) {
        _uiState.update { s ->
            val list = s.pDcdus.toMutableList()
            if (idx < list.size) list[idx] = list[idx].copy(cableSize = v)
            s.copy(pDcdus = list)
        }
    }
    fun onPDcduBreakerChange(idx: Int, v: String) {
        _uiState.update { s ->
            val list = s.pDcdus.toMutableList()
            if (idx < list.size) list[idx] = list[idx].copy(breakerRating = v)
            s.copy(pDcdus = list)
        }
    }
    fun addPDcdu() {
        _uiState.update { s ->
            val next = s.pDcdus.size + 1
            s.copy(pDcdus = s.pDcdus + DcduSlot("DCDU$next", "", ""))
        }
    }
    fun removePDcdu(idx: Int) {
        _uiState.update { s ->
            if (s.pDcdus.size <= 1) return@update s
            val list = s.pDcdus.toMutableList().apply { removeAt(idx) }
            s.copy(pDcdus = list)
        }
    }

    // ── DCDU Sub-section ────────────────────────────────────────────────────
    fun onNpDcduConnectionLabelChange(idx: Int, v: String) {
        _uiState.update { s ->
            val list = s.npDcdusConnections.toMutableList()
            if (idx < list.size) list[idx] = list[idx].copy(breakerLabel = v)
            s.copy(npDcdusConnections = list)
        }
        recalcTotalDcdU()
    }
    fun onNpDcduConnectionPhotoChange(idx: Int, path: String) {
        _uiState.update { s ->
            val list = s.npDcdusConnections.toMutableList()
            if (idx < list.size) list[idx] = list[idx].copy(photoPath = path)
            s.copy(npDcdusConnections = list)
        }
    }
    fun addNpDcduConnection() {
        _uiState.update { s ->
            val id = "np${System.currentTimeMillis()}"
            s.copy(npDcdusConnections = s.npDcdusConnections + DcduConnection(id, "", null))
        }
        recalcTotalDcdU()
    }
    fun removeNpDcduConnection(idx: Int) {
        _uiState.update { s ->
            if (s.npDcdusConnections.size <= 1) return@update s
            val list = s.npDcdusConnections.toMutableList().apply { removeAt(idx) }
            s.copy(npDcdusConnections = list)
        }
        recalcTotalDcdU()
    }

    fun onPDcduConnectionLabelChange(idx: Int, v: String) {
        _uiState.update { s ->
            val list = s.pDcdusConnections.toMutableList()
            if (idx < list.size) list[idx] = list[idx].copy(breakerLabel = v)
            s.copy(pDcdusConnections = list)
        }
        recalcTotalDcdU()
    }
    fun onPDcduConnectionPhotoChange(idx: Int, path: String) {
        _uiState.update { s ->
            val list = s.pDcdusConnections.toMutableList()
            if (idx < list.size) list[idx] = list[idx].copy(photoPath = path)
            s.copy(pDcdusConnections = list)
        }
    }
    fun addPDcduConnection() {
        _uiState.update { s ->
            val id = "p${System.currentTimeMillis()}"
            s.copy(pDcdusConnections = s.pDcdusConnections + DcduConnection(id, "", null))
        }
        recalcTotalDcdU()
    }
    fun removePDcduConnection(idx: Int) {
        _uiState.update { s ->
            if (s.pDcdusConnections.size <= 1) return@update s
            val list = s.pDcdusConnections.toMutableList().apply { removeAt(idx) }
            s.copy(pDcdusConnections = list)
        }
        recalcTotalDcdU()
    }

    private fun recalcTotalDcdU() {
        _uiState.update { s ->
            val count = s.npDcdusConnections.count { it.breakerLabel.isNotBlank() } +
                    s.pDcdusConnections.count { it.breakerLabel.isNotBlank() }
            s.copy(totalDcdUCount = count.toString())
        }
    }

    fun onTotalDcdUCountChange(v: String) = _uiState.update { it.copy(totalDcdUCount = v) }

    // ── RRU ─────────────────────────────────────────────────────────────────
    fun onRruCountChange(v: String) = _uiState.update { it.copy(rruCount = v) }
    fun onRruPowerCableCountChange(v: String) = _uiState.update { it.copy(rruPowerCableCount = v) }
    fun onRruPowerCableMissingChange(v: String) = _uiState.update { it.copy(rruPowerCableMissing = v) }
    fun onRruPowerCableLengthPerRunChange(v: String) = _uiState.update { it.copy(rruPowerCableLengthPerRun = v) }
    fun onRruPowerCableTotalMissingChange(v: String) = _uiState.update { it.copy(rruPowerCableTotalMissing = v) }
    fun onRruEarthingCableCountChange(v: String) = _uiState.update { it.copy(rruEarthingCableCount = v) }
    fun onRruEarthingCableMissingChange(v: String) = _uiState.update { it.copy(rruEarthingCableMissing = v) }
    fun onRruEarthingCableLengthPerRunChange(v: String) = _uiState.update { it.copy(rruEarthingCableLengthPerRun = v) }

    // ── AAU ─────────────────────────────────────────────────────────────────
    fun onAauCountChange(v: String) = _uiState.update { it.copy(aauCount = v) }
    fun onAauPowerCableCountChange(v: String) = _uiState.update { it.copy(aauPowerCableCount = v) }
    fun onAauPowerCableMissingChange(v: String) = _uiState.update { it.copy(aauPowerCableMissing = v) }
    fun onAauPowerCableLengthPerRunChange(v: String) = _uiState.update { it.copy(aauPowerCableLengthPerRun = v) }
    fun onAauPowerCableTotalMissingChange(v: String) = _uiState.update { it.copy(aauPowerCableTotalMissing = v) }
    fun onAauEarthingCableCountChange(v: String) = _uiState.update { it.copy(aauEarthingCableCount = v) }
    fun onAauEarthingCableMissingChange(v: String) = _uiState.update { it.copy(aauEarthingCableMissing = v) }
    fun onAauEarthingCableLengthPerRunChange(v: String) = _uiState.update { it.copy(aauEarthingCableLengthPerRun = v) }

    // ── BTS Earthing ───────────────────────────────────────────────────────
    fun onBtsEarthingTotalChange(v: String) = _uiState.update { it.copy(btsEarthingTotal = v) }
    fun onBtsEarthingCableCountChange(v: String) = _uiState.update { it.copy(btsEarthingCableCount = v) }
    fun onBtsEarthingCableMissingChange(v: String) = _uiState.update { it.copy(btsEarthingCableMissing = v) }
    fun onBtsEarthingLengthPerRunChange(v: String) = _uiState.update { it.copy(btsEarthingLengthPerRun = v) }
    fun onBtsEarthingConnectionChange(v: String) = _uiState.update { it.copy(btsEarthingConnection = v) }
    fun onBtsEarthingTotalMissingChange(v: String) = _uiState.update { it.copy(btsEarthingTotalMissing = v) }

    // ── Section expand/collapse ─────────────────────────────────────────────
    fun toggleSection(index: Int) {
        _uiState.update { s ->
            val expanded = s.expandedSections.toMutableSet()
            if (expanded.contains(index)) expanded.remove(index) else expanded.add(index)
            s.copy(expandedSections = expanded)
        }
    }

    // ── Photo upload helpers ────────────────────────────────────────────────
    private fun buildDcdbPhotoList(s: DcdbInfoUiState, recordId: String): List<PhotoToUpload> =
        buildList {
            s.npSectionPhotoPath?.let { add(PhotoToUpload(it, "dcdb", "np_section_photo", recordId)) }
            s.npLoadPhotoPath?.let { add(PhotoToUpload(it, "dcdb", "np_load_photo", recordId)) }
            s.pSectionPhotoPath?.let { add(PhotoToUpload(it, "dcdb", "p_section_photo", recordId)) }
            s.pLoadPhotoPath?.let { add(PhotoToUpload(it, "dcdb", "p_load_photo", recordId)) }
            s.npDcdusConnections.forEachIndexed { idx, conn ->
                conn.photoPath?.let { add(PhotoToUpload(it, "dcdb", "np_dcdu_conn_${idx}_photo", recordId)) }
            }
            s.pDcdusConnections.forEachIndexed { idx, conn ->
                conn.photoPath?.let { add(PhotoToUpload(it, "dcdb", "p_dcdu_conn_${idx}_photo", recordId)) }
            }
        }

    private suspend fun collectPhotoUploads(
        photosToUpload: List<PhotoToUpload>,
        onStarted: () -> Unit,
        onPhotoStarted: (Int, String) -> Unit,
        onPhotoDone: (String) -> Unit,
        onPhotoFailed: (String) -> Unit,
        onDone: () -> Unit
    ): Pair<Map<String, String>, Set<String>> {
        if (photosToUpload.isEmpty()) return Pair(emptyMap(), emptySet())

        onStarted()
        var photoUrls = emptyMap<String, String>()
        val uploadedPaths = mutableSetOf<String>()

        auditRepository.uploadPhotos(siteId, photosToUpload).collect { progress ->
            when (progress) {
                is UploadProgress.Started -> { /* total already set in onStarted */ }
                is UploadProgress.PhotoStarted -> {
                    onPhotoStarted(progress.index, progress.fieldName)
                }
                is UploadProgress.PhotoDone -> {
                    photoUrls = photoUrls + (progress.fieldName to progress.serverUrl)
                    uploadedPaths.add(progress.localPath)
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
        return Pair(photoUrls, uploadedPaths)
    }

    // ── Submit ────────────────────────────────────────────────────────────
    fun submit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }

            val s = _uiState.value
            val engineerName = authRepository.userName.first().orEmpty().ifBlank { "Unknown" }
            val recordId = UUID.randomUUID().toString()

            val photosToUpload = buildDcdbPhotoList(s, recordId)

            var photoUrls = emptyMap<String, String>()
            var uploadedPaths = emptySet<String>()
            if (photosToUpload.isNotEmpty()) {
                val result = collectPhotoUploads(
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
                        _uiState.update { it.copy(uploadCurrent = idx + 1, uploadCurrentName = fieldName, uploadPhotoStatuses = it.uploadPhotoStatuses + (fieldName to PhotoUploadStatus.UPLOADING)) }
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
                photoUrls = result.first
                uploadedPaths = result.second

                val allFailed = _uiState.value.uploadPhotoStatuses.values.all { it == PhotoUploadStatus.FAILED }
                if (allFailed && photoUrls.isEmpty()) {
                    _uiState.update { it.copy(isSubmitting = false, isUploadingPhotos = false, submitError = "All photo uploads failed. Check your connection.") }
                    return@launch
                }
            }

            val payload = buildPayload(s, recordId, photoUrls)
            auditRepository.submitAudit(
                siteId = siteId,
                type = "dcdb",
                engineerName = engineerName,
                data = payload,
                action = "submit"
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, isUploadingPhotos = false, uploadedPhotoPaths = it.uploadedPhotoPaths + uploadedPaths) }
                    _events.emit(DcdbInfoEvent.SubmitSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSubmitting = false, isUploadingPhotos = false, submitError = e.message ?: "Submission failed") }
                }
            )
        }
    }

    // ── Save Draft ─────────────────────────────────────────────────────────
    fun saveDraft() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingDraft = true, submitError = null) }

            val s = _uiState.value
            val engineerName = authRepository.userName.first().orEmpty().ifBlank { "Unknown" }
            val recordId = UUID.randomUUID().toString()

            val photosToUpload = buildDcdbPhotoList(s, recordId)

            var photoUrls = emptyMap<String, String>()
            var uploadedPaths = emptySet<String>()
            if (photosToUpload.isNotEmpty()) {
                val result = collectPhotoUploads(
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
                        _uiState.update { it.copy(uploadCurrent = idx + 1, uploadCurrentName = fieldName, uploadPhotoStatuses = it.uploadPhotoStatuses + (fieldName to PhotoUploadStatus.UPLOADING)) }
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
                photoUrls = result.first
                uploadedPaths = result.second

                val allFailed = _uiState.value.uploadPhotoStatuses.values.all { it == PhotoUploadStatus.FAILED }
                if (allFailed && photoUrls.isEmpty()) {
                    _uiState.update { it.copy(isSavingDraft = false, isUploadingPhotos = false, submitError = "All photo uploads failed. Check your connection.") }
                    return@launch
                }
            }

            val payload = buildPayload(s, recordId, photoUrls)
            auditRepository.submitAudit(
                siteId = siteId,
                type = "dcdb",
                engineerName = engineerName,
                data = payload,
                action = "save"
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSavingDraft = false, isUploadingPhotos = false, uploadedPhotoPaths = it.uploadedPhotoPaths + uploadedPaths) }
                    _events.emit(DcdbInfoEvent.SaveDraftSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSavingDraft = false, isUploadingPhotos = false, submitError = e.message ?: "Save draft failed") }
                }
            )
        }
    }

    private fun buildPayload(s: DcdbInfoUiState, recordId: String, photoUrls: Map<String, String>): Map<String, Any> = buildMap {
        put("type", "dcdb")
        put("id", recordId)
        put("grid_distance_to_3phase", s.gridDistanceTo3Phase)

        // Non-Priority
        put("np_cable_size_dcdb", s.npCableSizeDcdb)
        put("np_breaker1_mcb", s.npBreaker1Mcb)
        put("np_dcdus", s.npDcdus.joinToString(";") { "${it.label}:${it.cableSize}mm²:${it.breakerRating}A" })
        s.npSectionPhotoPath?.let { put("np_section_photo", photoUrls["np_section_photo"] ?: it) }
        put("np_load_measurement", s.npLoadMeasurement)
        s.npLoadPhotoPath?.let { put("np_load_photo", photoUrls["np_load_photo"] ?: it) }
        put("np_load_measured_time", s.npLoadMeasuredTime)

        // Priority
        put("p_cable_size_dcdb", s.pCableSizeDcdb)
        put("p_breaker1_mcb", s.pBreaker1Mcb)
        put("p_dcdus", s.pDcdus.joinToString(";") { "${it.label}:${it.cableSize}mm²:${it.breakerRating}A" })
        s.pSectionPhotoPath?.let { put("p_section_photo", photoUrls["p_section_photo"] ?: it) }
        put("p_load_measurement", s.pLoadMeasurement)
        s.pLoadPhotoPath?.let { put("p_load_photo", photoUrls["p_load_photo"] ?: it) }
        put("p_load_measured_time", s.pLoadMeasuredTime)

        // DCDU connections — replace local photo paths with server URLs
        put("np_dcdu_connections", s.npDcdusConnections.mapIndexed { idx, conn ->
            val serverUrl = photoUrls["np_dcdu_conn_${idx}_photo"]
            "${conn.breakerLabel}|${serverUrl ?: conn.photoPath ?: ""}"
        }.joinToString(";"))
        put("p_dcdu_connections", s.pDcdusConnections.mapIndexed { idx, conn ->
            val serverUrl = photoUrls["p_dcdu_conn_${idx}_photo"]
            "${conn.breakerLabel}|${serverUrl ?: conn.photoPath ?: ""}"
        }.joinToString(";"))
        put("total_dcdu_count", s.totalDcdUCount)

        // RRU
        put("rru_count", s.rruCount)
        put("rru_power_cable_count", s.rruPowerCableCount)
        put("rru_power_cable_missing", s.rruPowerCableMissing)
        put("rru_power_cable_length_per_run", s.rruPowerCableLengthPerRun)
        put("rru_power_cable_total_missing", s.rruPowerCableTotalMissing)
        put("rru_earthing_cable_count", s.rruEarthingCableCount)
        put("rru_earthing_cable_missing", s.rruEarthingCableMissing)
        put("rru_earthing_cable_length_per_run", s.rruEarthingCableLengthPerRun)

        // AAU
        put("aau_count", s.aauCount)
        put("aau_power_cable_count", s.aauPowerCableCount)
        put("aau_power_cable_missing", s.aauPowerCableMissing)
        put("aau_power_cable_length_per_run", s.aauPowerCableLengthPerRun)
        put("aau_power_cable_total_missing", s.aauPowerCableTotalMissing)
        put("aau_earthing_cable_count", s.aauEarthingCableCount)
        put("aau_earthing_cable_missing", s.aauEarthingCableMissing)
        put("aau_earthing_cable_length_per_run", s.aauEarthingCableLengthPerRun)

        // BTS Earthing
        put("bts_earthing_total", s.btsEarthingTotal)
        put("bts_earthing_cable_count", s.btsEarthingCableCount)
        put("bts_earthing_cable_missing", s.btsEarthingCableMissing)
        put("bts_earthing_cable_length_per_run", s.btsEarthingLengthPerRun)
        put("bts_earthing_connection", s.btsEarthingConnection)
        put("bts_earthing_total_missing", s.btsEarthingTotalMissing)
    }
}
