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

    suspend fun getSiteById(siteId: String): Site? = siteDao.getSiteById(siteId)?.toDomain()

    suspend fun refreshSites(): Result<List<Site>> {
        return try {
            val response = api.getSites()
            val sites = response.sites ?: emptyList()
            siteDao.insertSites(sites.map { it.toEntity() })
            Result.success(sites.map { it.toDomain() })
        } catch (e: Exception) {
            val msg = e.message ?: ""
            val errMsg = when {
                msg.contains("SSL", ignoreCase = true) ->
                    "Network error — check your internet connection."
                msg.contains("connect", ignoreCase = true) ||
                msg.contains("timeout", ignoreCase = true) ->
                    "Could not reach the server — please try again."
                else -> "Failed to load sites: ${e::class.simpleName} — $msg"
            }
            Result.failure(Exception(errMsg))
        }
    }

    private fun SiteDto.toEntity() = SiteEntity(
        siteId = siteId,
        name = name ?: siteId,
        address = address ?: "",
        type = type ?: "",
        status = status ?: "",
        latitude = parseLatitude(),
        longitude = parseLongitude(),
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: ""
    )

    private fun SiteDto.toDomain() = Site(
        siteId = siteId,
        name = name ?: siteId,
        address = address ?: "",
        type = type ?: "",
        status = status ?: "",
        latitude = parseLatitude(),
        longitude = parseLongitude(),
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: ""
    )

    private fun SiteEntity.toDomain() = Site(
        siteId = siteId,
        name = name,
        address = address,
        type = type,
        status = status,
        latitude = latitude,
        longitude = longitude,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
