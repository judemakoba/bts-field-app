package com.telco.btsfieldapp.ui.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.AuditRepository
import com.telco.btsfieldapp.data.repository.AuthRepository
import com.telco.btsfieldapp.data.repository.PhotoToUpload
import com.telco.btsfieldapp.data.repository.SiteRepository
import com.telco.btsfieldapp.data.repository.UploadProgress
import com.telco.btsfieldapp.domain.model.AntennaEntry
import com.telco.btsfieldapp.domain.model.RruEntry
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
import java.util.UUID
import javax.inject.Inject

data class TowerInfoUiState(
    val antennas: List<AntennaEntry> = listOf(AntennaEntry(id = newId())),
    val rrus: List<RruEntry> = listOf(RruEntry(id = newId())),
    val isSubmitting: Boolean = false,
    val isSavingDraft: Boolean = false,
    val submitError: String? = null,
    val expandedSections: Set<Int> = setOf(0, 1),
    val siteId: String = "",
    val siteName: String = "",
    val locationSummary: String = "Kampala",

    // Photo upload state
    val isUploadingPhotos: Boolean = false,
    val uploadTotal: Int = 0,
    val uploadCurrent: Int = 0,
    val uploadCurrentName: String = "",
    val uploadPhotoStatuses: Map<String, PhotoUploadStatus> = emptyMap()
)

sealed class TowerInfoEvent {
    data object SubmitSuccess : TowerInfoEvent()
    data object SaveDraftSuccess : TowerInfoEvent()
}

private fun newId() = UUID.randomUUID().toString().take(8)

@HiltViewModel
class TowerInfoViewModel @Inject constructor(
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository,
    private val siteRepository: SiteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val siteId: String = savedStateHandle.get<String>("siteId") ?: ""

    private val _uiState = MutableStateFlow(TowerInfoUiState(siteId = siteId))
    val uiState: StateFlow<TowerInfoUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TowerInfoEvent>()
    val events: SharedFlow<TowerInfoEvent> = _events.asSharedFlow()

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

    // ── Antenna helpers ───────────────────────────────────────────────────
    private fun findAntennaIndex(id: String) =
        _uiState.value.antennas.indexOfFirst { it.id == id }

    fun updateAntenna(id: String, update: AntennaEntry.() -> AntennaEntry) {
        _uiState.update { s ->
            s.copy(antennas = s.antennas.map { if (it.id == id) it.update() else it })
        }
    }

    fun addAntenna() {
        _uiState.update { s ->
            s.copy(antennas = s.antennas + AntennaEntry(id = newId()))
        }
    }

    fun removeAntenna(id: String) {
        _uiState.update { s ->
            if (s.antennas.size <= 1) s
            else s.copy(antennas = s.antennas.filter { it.id != id })
        }
    }

    fun onAntennaEquipmentType(id: String, v: String) = updateAntenna(id) { copy(equipmentType = v) }
    fun onAntennaManufacturer(id: String, v: String) = updateAntenna(id) { copy(manufacturer = v) }
    fun onAntennaModel(id: String, v: String) = updateAntenna(id) { copy(modelNumber = v) }
    fun onAntennaSector(id: String, v: String) = updateAntenna(id) { copy(sector = v) }
    fun onAntennaAzimuth(id: String, v: String) = updateAntenna(id) { copy(azimuth = v) }
    fun onAntennaHeight(id: String, v: String) = updateAntenna(id) { copy(heightToCentre = v) }
    fun onAntennaLength(id: String, v: String) = updateAntenna(id) { copy(lengthDia = v) }
    fun onAntennaWidth(id: String, v: String) = updateAntenna(id) { copy(width = v) }
    fun onAntennaHeightDim(id: String, v: String) = updateAntenna(id) { copy(height = v) }
    fun onAntennaActive(id: String, v: String) = updateAntenna(id) { copy(activeInactive = v) }
    fun onAntennaLabelling(id: String, v: String) = updateAntenna(id) { copy(equipmentLabelling = v) }

    fun onAntennaPhoto(id: String, photoType: String, path: String) {
        updateAntenna(id) {
            when (photoType) {
                "model_plate" -> copy(modelPlatePhoto = path)
                "ports" -> copy(portsPhoto = path)
                "dim1" -> copy(dimensionsPhoto1 = path)
                "dim2" -> copy(dimensionsPhoto2 = path)
                "dim3" -> copy(dimensionsPhoto3 = path)
                "azimuth" -> copy(azimuthPhoto = path)
                "height" -> copy(heightPhoto = path)
                else -> this
            }
        }
    }

    // ── RRU helpers ────────────────────────────────────────────────────────
    private fun findRruIndex(id: String) =
        _uiState.value.rrus.indexOfFirst { it.id == id }

    fun updateRru(id: String, update: RruEntry.() -> RruEntry) {
        _uiState.update { s ->
            s.copy(rrus = s.rrus.map { if (it.id == id) it.update() else it })
        }
    }

    fun addRru() {
        _uiState.update { s ->
            s.copy(rrus = s.rrus + RruEntry(id = newId()))
        }
    }

    fun removeRru(id: String) {
        _uiState.update { s ->
            if (s.rrus.size <= 1) s
            else s.copy(rrus = s.rrus.filter { it.id != id })
        }
    }

    fun onRruEquipmentType(id: String, v: String) = updateRru(id) { copy(equipmentType = v) }
    fun onRruManufacturer(id: String, v: String) = updateRru(id) { copy(manufacturer = v) }
    fun onRruModel(id: String, v: String) = updateRru(id) { copy(modelNumber = v) }
    fun onRruSector(id: String, v: String) = updateRru(id) { copy(sector = v) }
    fun onRruLength(id: String, v: String) = updateRru(id) { copy(lengthDia = v) }
    fun onRruWidth(id: String, v: String) = updateRru(id) { copy(width = v) }
    fun onRruHeight(id: String, v: String) = updateRru(id) { copy(height = v) }
    fun onRruActive(id: String, v: String) = updateRru(id) { copy(activeInactive = v) }
    fun onRruLabelling(id: String, v: String) = updateRru(id) { copy(equipmentLabelling = v) }

    fun onRruPhoto(id: String, photoType: String, path: String) {
        updateRru(id) {
            when (photoType) {
                "model_plate" -> copy(modelPlatePhoto = path)
                "dim1" -> copy(dimensionsPhoto1 = path)
                "dim2" -> copy(dimensionsPhoto2 = path)
                "dim3" -> copy(dimensionsPhoto3 = path)
                else -> this
            }
        }
    }

    // ── Section expand/collapse ─────────────────────────────────────────────
    fun toggleSection(index: Int) {
        _uiState.update { s ->
            val expanded = s.expandedSections.toMutableSet()
            if (expanded.contains(index)) expanded.remove(index) else expanded.add(index)
            s.copy(expandedSections = expanded)
        }
    }

    // ── Photo upload helpers ────────────────────────────────────────────────
    private fun buildTowerPhotoList(s: TowerInfoUiState, recordId: String): List<PhotoToUpload> =
        buildList {
            s.antennas.forEach { a ->
            a.modelPlatePhoto?.let  { add(PhotoToUpload(it, "tower", "ant_${a.id}_model_plate", recordId)) }
            a.portsPhoto?.let        { add(PhotoToUpload(it, "tower", "ant_${a.id}_ports", recordId)) }
            a.dimensionsPhoto1?.let  { add(PhotoToUpload(it, "tower", "ant_${a.id}_dim1", recordId)) }
            a.dimensionsPhoto2?.let  { add(PhotoToUpload(it, "tower", "ant_${a.id}_dim2", recordId)) }
            a.dimensionsPhoto3?.let  { add(PhotoToUpload(it, "tower", "ant_${a.id}_dim3", recordId)) }
            a.azimuthPhoto?.let      { add(PhotoToUpload(it, "tower", "ant_${a.id}_azimuth", recordId)) }
            a.heightPhoto?.let       { add(PhotoToUpload(it, "tower", "ant_${a.id}_height", recordId)) }
        }
            s.rrus.forEach { r ->
            r.modelPlatePhoto?.let  { add(PhotoToUpload(it, "tower", "rru_${r.id}_model_plate", recordId)) }
            r.dimensionsPhoto1?.let { add(PhotoToUpload(it, "tower", "rru_${r.id}_dim1", recordId)) }
            r.dimensionsPhoto2?.let { add(PhotoToUpload(it, "tower", "rru_${r.id}_dim2", recordId)) }
            r.dimensionsPhoto3?.let { add(PhotoToUpload(it, "tower", "rru_${r.id}_dim3", recordId)) }
        }
        }

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
                is UploadProgress.Started -> { /* total already set in onStarted */ }
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

    // ── Submit ────────────────────────────────────────────────────────────
    fun submit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }

            val s = _uiState.value
            val engineerName = authRepository.userName.first().orEmpty().ifBlank { "Unknown" }
            val recordId = UUID.randomUUID().toString()

            val photosToUpload = buildTowerPhotoList(s, recordId)

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

                val allFailed = _uiState.value.uploadPhotoStatuses.values.all { it == PhotoUploadStatus.FAILED }
                if (allFailed && photoUrls.isEmpty()) {
                    _uiState.update { it.copy(isSubmitting = false, isUploadingPhotos = false, submitError = "All photo uploads failed. Check your connection.") }
                    return@launch
                }
            }

            val payload = buildPayload(s, recordId, photoUrls)
            auditRepository.submitAudit(
                siteId = siteId,
                type = "tower",
                engineerName = engineerName,
                data = payload,
                action = "submit"
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, isUploadingPhotos = false) }
                    _events.emit(TowerInfoEvent.SubmitSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSubmitting = false, isUploadingPhotos = false, submitError = e.message ?: "Submission failed") }
                }
            )
        }
    }

    // ── Save Draft ────────────────────────────────────────────────────────
    fun saveDraft() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingDraft = true, submitError = null) }

            val s = _uiState.value
            val engineerName = authRepository.userName.first().orEmpty().ifBlank { "Unknown" }
            val recordId = UUID.randomUUID().toString()

            val photosToUpload = buildTowerPhotoList(s, recordId)

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

                val allFailed = _uiState.value.uploadPhotoStatuses.values.all { it == PhotoUploadStatus.FAILED }
                if (allFailed && photoUrls.isEmpty()) {
                    _uiState.update { it.copy(isSavingDraft = false, isUploadingPhotos = false, submitError = "All photo uploads failed. Check your connection.") }
                    return@launch
                }
            }

            val payload = buildPayload(s, recordId, photoUrls)
            auditRepository.submitAudit(
                siteId = siteId,
                type = "tower",
                engineerName = engineerName,
                data = payload,
                action = "save"
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSavingDraft = false, isUploadingPhotos = false) }
                    _events.emit(TowerInfoEvent.SaveDraftSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSavingDraft = false, isUploadingPhotos = false, submitError = e.message ?: "Save draft failed") }
                }
            )
        }
    }

    private fun buildPayload(s: TowerInfoUiState, recordId: String, photoUrls: Map<String, String>): Map<String, Any> {
        val antennasJson = s.antennas.map { a ->
            buildMap<String, Any> {
                put("id", a.id)
                put("equipment_type", a.equipmentType)
                put("manufacturer", a.manufacturer)
                put("model_number", a.modelNumber)
                put("tenant_owner", a.tenantOwner)
                put("sector", a.sector)
                put("azimuth", a.azimuth)
                put("height_to_centre", a.heightToCentre)
                put("length_dia_mm", a.lengthDia)
                put("width_mm", a.width)
                put("height_mm", a.height)
                put("active_inactive", a.activeInactive)
                put("equipment_labelling", a.equipmentLabelling)
                a.modelPlatePhoto?.let { put("photo_model_plate", photoUrls["ant_${a.id}_model_plate"] ?: it) }
                a.portsPhoto?.let { put("photo_ports", photoUrls["ant_${a.id}_ports"] ?: it) }
                a.dimensionsPhoto1?.let { put("photo_dim1", photoUrls["ant_${a.id}_dim1"] ?: it) }
                a.dimensionsPhoto2?.let { put("photo_dim2", photoUrls["ant_${a.id}_dim2"] ?: it) }
                a.dimensionsPhoto3?.let { put("photo_dim3", photoUrls["ant_${a.id}_dim3"] ?: it) }
                a.azimuthPhoto?.let { put("photo_azimuth", photoUrls["ant_${a.id}_azimuth"] ?: it) }
                a.heightPhoto?.let { put("photo_height", photoUrls["ant_${a.id}_height"] ?: it) }
            }
        }

        val rrusJson = s.rrus.map { r ->
            buildMap<String, Any> {
                put("id", r.id)
                put("equipment_type", r.equipmentType)
                put("manufacturer", r.manufacturer)
                put("model_number", r.modelNumber)
                put("tenant_owner", r.tenantOwner)
                put("sector", r.sector)
                put("length_dia_mm", r.lengthDia)
                put("width_mm", r.width)
                put("height_mm", r.height)
                put("active_inactive", r.activeInactive)
                put("equipment_labelling", r.equipmentLabelling)
                r.modelPlatePhoto?.let { put("photo_model_plate", photoUrls["rru_${r.id}_model_plate"] ?: it) }
                r.dimensionsPhoto1?.let { put("photo_dim1", photoUrls["rru_${r.id}_dim1"] ?: it) }
                r.dimensionsPhoto2?.let { put("photo_dim2", photoUrls["rru_${r.id}_dim2"] ?: it) }
                r.dimensionsPhoto3?.let { put("photo_dim3", photoUrls["rru_${r.id}_dim3"] ?: it) }
            }
        }

        return buildMap<String, Any> {
            put("type", "tower")
            put("id", recordId)
            put("antennas", antennasJson)
            put("rrus", rrusJson)
        }
    }
}
