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
    val success: Boolean?,
    val code: String?,
    val error: String?,
    val attemptsLeft: Int?,
    val retryAfterSeconds: Int?
)

data class UserDto(
    val id: String?,
    val name: String?,
    @SerializedName("full_name") val fullName: String?,
    val email: String?,
    val phone: String?,
    val role: String?
)

// ── Sites ──────────────────────────────────────────────────────────────────────

data class SiteDto(
    val id: String?,
    @SerializedName("siteId") val siteId: String,
    @SerializedName("siteName") val name: String?,
    val address: String?,
    val type: String?,
    val status: String?,
    val latitude: String?,
    val longitude: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
) {
    /** Safe parse: returns null for empty/blank strings */
    fun parseLatitude(): Double? = latitude?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
    fun parseLongitude(): Double? = longitude?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
}

data class SitesResponse(
    val success: Boolean?,
    val sites: List<SiteDto>?,
    val message: String?,
    val error: String?,
    val code: String?
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
    // Site / Tower Info
    @SerializedName("atc_id") val atcId: String?,
    @SerializedName("tower_type") val towerType: String?,
    @SerializedName("tower_height") val towerHeight: String?,
    @SerializedName("building_height") val buildingHeight: String?,
    @SerializedName("total_height") val totalHeight: String?,
    @SerializedName("site_indoor_outdoor") val siteIndoorOutdoor: String?,
    @SerializedName("no_of_tenants") val noOfTenants: String?,
    @SerializedName("other_tenants") val otherTenants: String?,
    @SerializedName("latitude") val latitude: String?,
    @SerializedName("longitude") val longitude: String?,
    @SerializedName("gps_accuracy") val gpsAccuracy: String?,
    @SerializedName("altitude") val altitude: String?,
    // Survey Details
    @SerializedName("survey_date") val surveyDate: String?,
    @SerializedName("technician_name") val technicianName: String?,
    @SerializedName("technician_contacts") val technicianContacts: String?,
    @SerializedName("contractor_name") val contractorName: String?,
    // Power Infrastructure
    @SerializedName("has_grid") val hasGrid: Boolean?,
    @SerializedName("has_dg") val hasDG: Boolean?,
    @SerializedName("has_solar") val hasSolar: Boolean?,
    @SerializedName("grid_distance_to_3phase") val gridDistanceTo3Phase: String?,
    // RRU & Cabinets
    @SerializedName("guard_at_site") val guardAtSite: Boolean?,
    @SerializedName("rru_type") val rruType: String?,
    @SerializedName("rru_count") val rruCount: String?,
    @SerializedName("cabinet_types") val cabinetTypes: String?,
    @SerializedName("cabinet_count") val cabinetCount: String?,
    @SerializedName("equipment_labelled") val equipmentLabelled: Boolean?,
    @SerializedName("cabinet_comments") val cabinetComments: String?,
    @SerializedName("cabinet_dimensions_lxwxh") val cabinetDimensionsLxWxH: String?,
    @SerializedName("active_idu_types") val activeIduTypes: String?,
    @SerializedName("non_active_idu_types") val nonActiveIduTypes: String?,
    @SerializedName("non_active_idu_count") val nonActiveIduCount: String?,
    // Slab
    @SerializedName("slab_dimensions") val slabDimensions: String?,
    // Redundant
    @SerializedName("redundant_equipment_count") val redundantEquipmentCount: String?,
    @SerializedName("redundant_item_name") val redundantItemName: String?,
    // Media
    @SerializedName("is_on_fiber") val isOnFiber: Boolean?,
    val notes: String?,
    // Metadata
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
    val tower: List<Map<String, Any>>?,
    val action: String? = null // "save" (draft) | "submit" (for review)
)

data class AuditSyncResponse(
    val success: Boolean?,
    val syncedAt: String?,
    val siteId: String?,
    val status: String?, // "draft" | "submitted"
    val message: String?
)

// ── Rejected reports ──────────────────────────────────────────────────────
data class RejectedReportsResponse(
    val rejected: RejectedRecords?
)

data class RejectedRecords(
    val ground: List<RejectedGroundRecord>?,
    val dcdb: List<RejectedDcdbRecord>?,
    val tower: List<RejectedTowerRecord>?
)

data class RejectedGroundRecord(
    val id: String,
    @SerializedName("siteId") val siteId: String,
    val status: String,
    val rejectionReason: String?,
    val updatedAt: String?
)

data class RejectedDcdbRecord(
    val id: String,
    @SerializedName("siteId") val siteId: String,
    val status: String,
    val rejectionReason: String?,
    val updatedAt: String?
)

data class RejectedTowerRecord(
    val id: String,
    @SerializedName("siteId") val siteId: String,
    val status: String,
    val rejectionReason: String?,
    val updatedAt: String?
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

// ── Photo Upload response ────────────────────────────────────────────────────────

data class PhotoUploadResponse(
    val success: Boolean?,
    val photo: PhotoInfo?
)

data class PhotoDto(
    val id: String,
    @SerializedName("siteId") val siteId: String,
    val category: String?,
    val recordId: String?,
    val fieldName: String?,
    val original: String?,
    val thumbnail: String?,
    val filename: String?,
    val size: Long?,
    @SerializedName("uploadedAt") val uploadedAt: String?
)

data class PhotosResponse(
    val photos: List<PhotoDto>?,
    val total: Int?
)

data class RecordResponseDto(
    val record: Map<String, Any>?,
    val type: String?,
    val message: String?
)

data class PhotoInfo(
    val id: String?,
    val serverUrl: String?,
    val thumbnailUrl: String?,
    val filename: String?,
    val size: Long?,
    val recordId: String? = null,
    val fieldName: String? = null
)

// ── Generic API response ────────────────────────────────────────────────────────

data class ApiResponse(
    val success: Boolean?,
    val message: String?,
    val error: String?
)
