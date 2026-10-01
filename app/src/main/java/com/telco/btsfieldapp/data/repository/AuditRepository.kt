package com.telco.btsfieldapp.data.repository

import com.telco.btsfieldapp.data.local.dao.AuditDao
import com.telco.btsfieldapp.data.local.dao.PendingSyncDao
import com.telco.btsfieldapp.data.local.entity.AuditEntity
import com.telco.btsfieldapp.data.local.entity.PendingSyncEntity
import com.telco.btsfieldapp.data.remote.ApiService
import com.telco.btsfieldapp.data.remote.AuditSyncRequest
import com.telco.btsfieldapp.data.remote.PhotoDto
import com.telco.btsfieldapp.data.remote.RecordResponseDto
import com.telco.btsfieldapp.data.remote.RejectedReportsResponse
import com.telco.btsfieldapp.domain.model.AuditRecord
import com.telco.btsfieldapp.domain.model.DcdbRecord
import com.telco.btsfieldapp.domain.model.GroundRecord
import com.telco.btsfieldapp.domain.model.TowerRecord
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// ── Photo upload types ─────────────────────────────────────────────────────────

/** Describes one photo to be uploaded before saving a draft. */
data class PhotoToUpload(
    val localPath: String,
    val auditType: String,  // "ground" | "dcdb" | "tower"
    val fieldName: String,  // e.g. "site_photo", "np_section_photo", "tower_ant_abc123_model_plate"
    val recordId: String   // UUID of the record this photo belongs to
)

/** Progress events emitted during a batch photo upload. */
sealed class UploadProgress {
    data class Started(val total: Int) : UploadProgress()
    data class PhotoStarted(val index: Int, val fieldName: String) : UploadProgress()
    data class PhotoDone(val index: Int, val fieldName: String, val serverUrl: String, val localPath: String) : UploadProgress()
    data class PhotoFailed(val index: Int, val fieldName: String, val error: String) : UploadProgress()
    data class Done(val photoUrls: Map<String, String>) : UploadProgress()  // fieldName → serverUrl
}

@Singleton
class AuditRepository @Inject constructor(
    private val api: ApiService,
    private val auditDao: AuditDao,
    private val pendingSyncDao: PendingSyncDao,
    private val gson: Gson
) {
    /**
     * Fetch audit data for a site from the server.
     * Admin gets all users' data; engineer gets own data only.
     */
    suspend fun fetchSiteAudit(siteId: String): Result<SiteAuditData> {
        return try {
            val response = api.getSiteAudit(siteId)
            Result.success(
                SiteAuditData(
                    groundRecords = response.groundEquipment?.map { it.toDomain() } ?: emptyList(),
                    dcdbRecords = response.dcdbRecords?.map { it.toDomain() } ?: emptyList(),
                    towerRecords = response.towerEquipment?.map { it.toDomain() } ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submit audit records to the server via the sync endpoint.
     * Stores in local DB first, then syncs. If offline, queues for later sync.
     * action: null (legacy), "save" (draft), "submit" (for review)
     */
    suspend fun submitAudit(
        siteId: String,
        type: String,
        engineerName: String,
        data: Map<String, Any>,
        action: String? = null
    ): Result<Unit> {
        val pendingEntry = PendingSyncEntity(
            entityType = type,
            entityId = System.currentTimeMillis(),
            action = "create",
            payload = gson.toJson(data)
        )

        return try {
            val request = AuditSyncRequest(
                siteId = siteId,
                site = null,
                ground = if (type == "ground") listOf(data) else null,
                dcdb = if (type == "dcdb" || type == "dcdb_info") listOf(data) else null,
                tower = if (type == "tower" || type == "tower_info") listOf(data) else null,
                action = action
            )
            val response = api.syncAudit(request)
            if (response.success == true) {
                pendingSyncDao.delete(pendingEntry)
                Result.success(Unit)
            } else {
                pendingSyncDao.insert(pendingEntry)
                Result.failure(Exception(response.message ?: "Sync failed"))
            }
        } catch (e: Exception) {
            pendingSyncDao.insert(pendingEntry)
            Result.failure(e)
        }
    }

    /**
     * Get rejected reports for the current engineer.
     */
    suspend fun getRejectedReports(): Result<RejectedReportsResponse> {
        return try {
            val response = api.getRejectedReports()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync all pending local records to the server.
     */
    suspend fun syncPending(): Int {
        val pending = pendingSyncDao.getAllPending()
        var synced = 0
        for (item in pending) {
            try {
                val data = gson.fromJson(item.payload, Map::class.java) as Map<String, Any>
                val request = AuditSyncRequest(
                    siteId = item.entityId.toString(),
                    site = null,
                    ground = if (item.entityType == "ground") listOf(data) else null,
                    dcdb = if (item.entityType == "dcdb") listOf(data) else null,
                    tower = if (item.entityType == "tower") listOf(data) else null
                )
                val response = api.syncAudit(request)
                if (response.success == true) {
                    pendingSyncDao.deleteById(item.id)
                    synced++
                }
            } catch (_: Exception) {
                // leave in queue for next sync
            }
        }
        return synced
    }

    /**
     * Upload a batch of photos to the server with progress tracking.
     * Returns a Flow<UploadProgress> that emits progress for each photo.
     *
     * @param siteId  The site identifier
     * @param photos  List of photos to upload — each describes its local path,
     *                audit type (ground/dcdb/tower), field name, and record ID
     * @return Flow<UploadProgress> — emits per-photo and overall progress
     */
    fun uploadPhotos(siteId: String, photos: List<PhotoToUpload>): Flow<UploadProgress> = flow {
        if (photos.isEmpty()) {
            emit(UploadProgress.Done(emptyMap()))
            return@flow
        }

        val results = mutableMapOf<String, String>() // fieldName → serverUrl
        emit(UploadProgress.Started(photos.size))

        photos.forEachIndexed { idx, photo ->
            emit(UploadProgress.PhotoStarted(idx, photo.fieldName))
            try {
                val result = uploadSinglePhoto(siteId, photo)
                results[photo.fieldName] = result
                emit(UploadProgress.PhotoDone(idx, photo.fieldName, result, photo.localPath))
            } catch (e: Exception) {
                emit(UploadProgress.PhotoFailed(idx, photo.fieldName, e.message ?: "Upload failed"))
            }
        }

        emit(UploadProgress.Done(results))
    }.flowOn(Dispatchers.IO)

    private suspend fun uploadSinglePhoto(siteId: String, photo: PhotoToUpload): String {
        return withContext(Dispatchers.IO) {
            val file = File(photo.localPath)
            if (!file.exists()) throw IllegalStateException("Photo file not found: ${photo.localPath}")

            val siteIdBody = siteId.toRequestBody("text/plain".toMediaTypeOrNull())
            val auditTypeBody = photo.auditType.toRequestBody("text/plain".toMediaTypeOrNull())
            val recordIdBody = photo.recordId.toRequestBody("text/plain".toMediaTypeOrNull())
            val fieldNameBody = photo.fieldName.toRequestBody("text/plain".toMediaTypeOrNull())

            val mediaType = "image/jpeg".toMediaTypeOrNull() ?: throw IllegalStateException("Unsupported media type")
            val requestFile = file.asRequestBody(mediaType)
            val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)

            val response = api.uploadPhoto(siteIdBody, auditTypeBody, recordIdBody, fieldNameBody, photoPart)
            response.photo?.serverUrl ?: throw IllegalStateException("Server returned no URL")
        }
    }

    fun getAllAudits(): Flow<List<AuditRecord>> =
        auditDao.getAllAudits().map { entities -> entities.map { it.toDomain() } }

    /**
     * Fetch photos for a specific audit record.
     * Returns photos with recordId and fieldName metadata.
     */
    suspend fun fetchPhotosForRecord(siteId: String, recordId: String): Result<List<PhotoDto>> {
        return try {
            val response = api.getAuditPhotos(siteId, recordId)
            Result.success(response.photos ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch a single audit record by type and ID.
     * Returns the record data as a Map for flexible field access.
     */
    suspend fun fetchAuditRecord(type: String, id: String): Result<Map<String, Any>?> {
        return try {
            val response = api.getAuditRecord(type, id)
            Result.success(response.record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── API response → domain mappers ────────────────────────────────────────────────

data class SiteAuditData(
    val groundRecords: List<GroundRecord>,
    val dcdbRecords: List<DcdbRecord>,
    val towerRecords: List<TowerRecord>
)

private fun com.telco.btsfieldapp.data.remote.GroundRecordDto.toDomain() = GroundRecord(
    id = id,
    siteId = siteId,
    type = type ?: "ground",
    // Legacy ground inspection (not in DTO, always empty)
    fenceCondition = "",
    gateLock = "",
    groundResistance = "",
    drainageCondition = "",
    // Site / Tower Info
    atcId = atcId ?: "",
    towerType = towerType ?: "",
    towerHeight = towerHeight ?: "",
    buildingHeight = buildingHeight ?: "0",
    totalHeight = totalHeight ?: "0",
    siteIndoorOutdoor = siteIndoorOutdoor ?: "",
    noOfTenants = noOfTenants ?: "",
    otherTenants = otherTenants?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
    latitude = latitude ?: "",
    longitude = longitude ?: "",
    gpsAccuracy = gpsAccuracy ?: "",
    altitude = altitude ?: "",
    // Survey Details
    surveyDate = surveyDate ?: "",
    technicianName = technicianName ?: "",
    technicianContacts = technicianContacts ?: "",
    contractorName = contractorName ?: "Innovis",
    // Power Infrastructure
    hasGrid = hasGrid ?: false,
    hasDG = hasDG ?: false,
    hasSolar = hasSolar ?: false,
    gridDistanceTo3Phase = gridDistanceTo3Phase ?: "",
    // RRU & Cabinets
    guardAtSite = guardAtSite ?: false,
    rruType = rruType ?: "",
    rruCount = rruCount ?: "",
    cabinetTypes = cabinetTypes ?: "",
    cabinetCount = cabinetCount ?: "",
    equipmentLabelled = equipmentLabelled ?: false,
    cabinetComments = cabinetComments ?: "",
    cabinetDimensionsLxW = cabinetDimensionsLxWxH ?: "",
    activeIduTypes = activeIduTypes ?: "",
    nonActiveIduTypes = nonActiveIduTypes ?: "",
    nonActiveIduCount = nonActiveIduCount ?: "",
    // Slab
    slabDimensions = slabDimensions ?: "",
    // Redundant
    redundantEquipmentCount = redundantEquipmentCount ?: "",
    redundantItemName = redundantItemName ?: "",
    // Media & Remarks
    isOnFiber = isOnFiber,
    notes = notes ?: "",
    // Metadata
    createdAt = createdAt ?: "",
    updatedAt = updatedAt ?: ""
)

private fun com.telco.btsfieldapp.data.remote.DcdbRecordDto.toDomain() = DcdbRecord(
    id = id,
    siteId = siteId,
    type = type ?: "dcdb",
    dcdbType = dcdbType ?: "",
    dcdbCapacity = dcdbCapacity ?: "",
    cablesCondition = cablesCondition ?: "",
    surgeProtection = surgeProtection ?: "",
    cableEntrySealed = cableEntrySealed ?: "",
    notes = notes ?: "",
    createdAt = createdAt ?: "",
    updatedAt = updatedAt ?: ""
)

private fun com.telco.btsfieldapp.data.remote.TowerRecordDto.toDomain() = TowerRecord(
    id = id,
    siteId = siteId,
    type = type ?: "tower",
    towerType = towerType ?: "",
    towerHeight = towerHeight ?: "",
    structuralIntegrity = structuralIntegrity ?: "",
    rustCorrosion = rustCorrosion ?: "",
    boltCondition = boltCondition ?: "",
    lightningRod = lightningRod ?: "",
    climbSafety = climbSafety ?: "",
    antennaMounting = antennaMounting ?: "",
    notes = notes ?: "",
    createdAt = createdAt ?: "",
    updatedAt = updatedAt ?: ""
)

private fun AuditEntity.toDomain() = AuditRecord(
    id = id,
    siteId = siteId,
    type = type,
    engineerName = engineerName,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    synced = synced
)
