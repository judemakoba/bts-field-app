package com.telco.btsfieldapp.data.repository

import com.telco.btsfieldapp.data.local.dao.AuditDao
import com.telco.btsfieldapp.data.local.dao.PendingSyncDao
import com.telco.btsfieldapp.data.local.entity.AuditEntity
import com.telco.btsfieldapp.data.local.entity.PendingSyncEntity
import com.telco.btsfieldapp.data.remote.ApiService
import com.telco.btsfieldapp.data.remote.AuditSyncRequest
import com.telco.btsfieldapp.domain.model.AuditRecord
import com.telco.btsfieldapp.domain.model.DcdbRecord
import com.telco.btsfieldapp.domain.model.GroundRecord
import com.telco.btsfieldapp.domain.model.TowerRecord
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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
     */
    suspend fun submitAudit(
        siteId: String,
        type: String,
        engineerName: String,
        data: Map<String, Any>
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
                ground = if (type == "ground" || type == "ground_equipment") listOf(data) else null,
                dcdb = if (type == "dcdb") listOf(data) else null,
                tower = if (type == "tower") listOf(data) else null
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

    fun getAllAudits(): Flow<List<AuditRecord>> =
        auditDao.getAllAudits().map { entities -> entities.map { it.toDomain() } }
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
    fenceCondition = fenceCondition ?: "",
    gateLock = gateLock ?: "",
    groundResistance = groundResistance ?: "",
    drainageCondition = drainageCondition ?: "",
    notes = notes ?: "",
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
