package com.telco.btsfieldapp.data.local.dao

import androidx.room.*
import com.telco.btsfieldapp.data.local.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getSiteById(id: Long): SiteEntity?

    @Query("SELECT * FROM sites WHERE name LIKE '%' || :query || '%' OR btsId LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    fun searchSites(query: String): Flow<List<SiteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity)

    @Query("DELETE FROM sites")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sites")
    suspend fun count(): Int
}
