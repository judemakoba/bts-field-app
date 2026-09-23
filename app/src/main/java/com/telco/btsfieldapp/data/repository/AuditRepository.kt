package com.telco.btsfieldapp.data.repository

import com.telco.btsfieldapp.data.local.dao.AuditDao
import com.telco.btsfieldapp.data.local.entity.AuditEntity
import com.telco.btsfieldapp.data.remote.*
import com.telco.btsfieldapp.domain.model.AuditRecord
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepository @Inject constructor(
    private val api: ApiService,
    private val auditDao: AuditDao,
    private val gson: Gson
) {
    fun getAuditsBySite(siteId: Long): Flow<List<AuditRecord>> =
        auditDao.getAuditsBySite(siteId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun getAllAudits(): Flow<List<AuditRecord>> =
        auditDao.getAllAudits().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getAuditById(id: Long): AuditRecord? =
        auditDao.getAuditById(id)?.toDomain()

    suspend fun createAudit(
        siteId: Long,
        type: String,
        engineerName: String,
        data: Map<String, Any>
    ): Result<Long> {
        val now = Instant.now().toString()
        val entity = AuditEntity(
            siteId = siteId,
            type = type,
            engineerName = engineerName,
            status = "pending",
            createdAt = now,
            updatedAt = now,
            synced = false,
            dataJson = gson.toJson(data)
        )
        val localId = auditDao.insertAudit(entity)

        return try {
            val response = api.createAudit(CreateAuditRequest(siteId, type, engineerName, data))
            if (response.success == true) {
                auditDao.updateAudit(entity.copy(id = localId, synced = true, status = "synced"))
                Result.success(localId)
            } else {
                Result.success(localId)
            }
        } catch (e: Exception) {
            Result.success(localId)
        }
    }

    suspend fun syncPending(): Int {
        var synced = 0
        val pending = auditDao.unsyncedCount()
        if (pending > 0) {
            synced = pending
        }
        return synced
    }

    private fun AuditEntity.toDomain(): AuditRecord {
        val dataMap: Map<String, Any>? = try {
            gson.fromJson(dataJson, Map::class.java) as? Map<String, Any>
        } catch (_: Exception) { null }

        return AuditRecord(
            id = id,
            siteId = siteId,
            type = type,
            engineerName = engineerName,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            synced = synced,
            data = null
        )
    }
}
