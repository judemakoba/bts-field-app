package com.telco.btsfieldapp.data.remote

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String?,
    val user: UserDto?,
    val message: String?,
    val success: Boolean?
)

data class UserDto(
    val id: String?,
    val name: String?,
    @SerializedName("full_name") val fullName: String?,
    val email: String?,
    val role: String?
)

// ── Sites ──────────────────────────────────────────────────────────────────────

data class SiteDto(
    val id: String?,
    @SerializedName("siteId") val siteId: String,
    val name: String,
    val address: String?,
    val type: String?,
    val status: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("assignedUsers") val assignedUsers: List<String>?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class SitesResponse(
    val success: Boolean?,
    val data: List<SiteDto>?,
    val message: String?
)

// ── Audit / Site Audit ─────────────────────────────────────────────────────────

/** GET /api/audit/site/:siteId */
data class SiteAuditResponse(
    val siteId: String,
    val site: SiteDto?,
    @SerializedName("siteInfo") val siteInfo: Any?,
    @SerializedName("groundEquipment") val groundEquipment: List<GroundRecordDto>?,
    @SerializedName("dcdbRecords") val dcdbRecords: List<DcdbRecordDto>?,
    @SerializedName("towerEquipment") val towerEquipment: List<TowerRecordDto>?
)

data class GroundRecordDto(
    val id: String,
    @SerializedName("siteId") val siteId: String,
    val type: String?,
    @SerializedName("fence_condition") val fenceCondition: String?,
    @SerializedName("gate_lock") val gateLock: String?,
    @SerializedName("ground_resistance") val groundResistance: String?,
    @SerializedName("drainage_condition") val drainageCondition: String?,
    val notes: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class DcdbRecordDto(
    val id: String,
    @SerializedName("siteId") val siteId: String,
    val type: String?,
    @SerializedName("dcdb_type") val dcdbType: String?,
    @SerializedName("dcdb_capacity") val dcdbCapacity: String?,
    @SerializedName("cables_condition") val cablesCondition: String?,
    @SerializedName("surge_protection") val surgeProtection: String?,
    @SerializedName("cable_entry_sealed") val cableEntrySealed: String?,
    val notes: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class TowerRecordDto(
    val id: String,
    @SerializedName("siteId") val siteId: String,
    val type: String?,
    @SerializedName("tower_type") val towerType: String?,
    @SerializedName("tower_height") val towerHeight: String?,
    @SerializedName("structural_integrity") val structuralIntegrity: String?,
    @SerializedName("rust_corrosion") val rustCorrosion: String?,
    @SerializedName("bolt_condition") val boltCondition: String?,
    @SerializedName("lightning_rod") val lightningRod: String?,
    @SerializedName("climb_safety") val climbSafety: String?,
    @SerializedName("antenna_mounting") val antennaMounting: String?,
    val notes: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

// ── Audit Sync (mobile → server) ────────────────────────────────────────────────

data class AuditSyncRequest(
    @SerializedName("siteId") val siteId: String,
    val site: Map<String, Any>?,
    val ground: List<Map<String, Any>>?,
    val dcdb: List<Map<String, Any>>?,
    val tower: List<Map<String, Any>>?
)

data class AuditSyncResponse(
    val success: Boolean?,
    val syncedAt: String?,
    val siteId: String?,
    val message: String?
)

// ── Ground / DCDB / Tower individual endpoints ─────────────────────────────────

data class RecordListResponse(
    val records: List<Map<String, Any>>?,
    val total: Int?,
    val message: String?
)

data class RecordResponse(
    val success: Boolean?,
    val record: Map<String, Any>?,
    val message: String?
)

// ── Generic API response ────────────────────────────────────────────────────────

data class ApiResponse(
    val success: Boolean?,
    val message: String?,
    val error: String?
)
