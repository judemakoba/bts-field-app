package com.telco.btsfieldapp.data.local.dao

import androidx.room.*
import com.telco.btsfieldapp.data.local.entity.AuditEntity
import com.telco.btsfieldapp.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Query("SELECT * FROM audits WHERE siteId = :siteId ORDER BY createdAt DESC")
    fun getAuditsBySite(siteId: Long): Flow<List<AuditEntity>>

    @Query("SELECT * FROM audits ORDER BY createdAt DESC")
    fun getAllAudits(): Flow<List<AuditEntity>>

    @Query("SELECT * FROM audits WHERE id = :id")
    suspend fun getAuditById(id: Long): AuditEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: AuditEntity): Long

    @Update
    suspend fun updateAudit(audit: AuditEntity)

    @Query("DELETE FROM audits WHERE id = :id")
    suspend fun deleteAudit(id: Long)

    @Query("SELECT COUNT(*) FROM audits WHERE synced = 0")
    suspend fun unsyncedCount(): Int
}

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingSyncEntity>

    @Insert
    suspend fun insert(entity: PendingSyncEntity)

    @Delete
    suspend fun delete(entity: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync")
    suspend fun deleteAll()
}
