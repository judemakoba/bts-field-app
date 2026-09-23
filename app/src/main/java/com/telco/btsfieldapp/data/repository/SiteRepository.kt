package com.telco.btsfieldapp.data.repository

import com.telco.btsfieldapp.data.local.dao.SiteDao
import com.telco.btsfieldapp.data.local.entity.SiteEntity
import com.telco.btsfieldapp.data.remote.ApiService
import com.telco.btsfieldapp.data.remote.SiteDto
import com.telco.btsfieldapp.domain.model.Site
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteRepository @Inject constructor(
    private val api: ApiService,
    private val siteDao: SiteDao
) {
    fun getAllSites(): Flow<List<Site>> = siteDao.getAllSites().map { entities ->
        entities.map { it.toDomain() }
    }

    fun searchSites(query: String): Flow<List<Site>> = siteDao.searchSites(query).map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getSiteById(id: Long): Site? = siteDao.getSiteById(id)?.toDomain()

    suspend fun refreshSites(): Result<List<Site>> {
        return try {
            val response = api.getSites()
            if (response.success == true || response.data != null) {
                val sites = response.data ?: emptyList()
                siteDao.insertSites(sites.map { it.toEntity() })
                Result.success(sites.map { it.toDomain() })
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch sites"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SiteDto.toEntity() = SiteEntity(
        id = id,
        name = name,
        btsId = btsId,
        address = address,
        latitude = latitude,
        longitude = longitude,
        type = type ?: "",
        status = status ?: "",
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: ""
    )

    private fun SiteDto.toDomain() = Site(
        id = id,
        name = name,
        btsId = btsId,
        address = address,
        latitude = latitude,
        longitude = longitude,
        type = type ?: "",
        status = status ?: "",
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: ""
    )

    private fun SiteEntity.toDomain() = Site(
        id = id,
        name = name,
        btsId = btsId,
        address = address,
        latitude = latitude,
        longitude = longitude,
        type = type,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
