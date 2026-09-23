package com.telco.btsfieldapp.data.remote

import com.google.gson.annotations.SerializedName

// --- Auth ---

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
    val id: Long?,
    val name: String?,
    @SerializedName("full_name") val fullName: String?,
    val email: String?,
    val role: String?
)

// --- Sites ---

data class SiteDto(
    val id: Long,
    val name: String,
    @SerializedName("bts_id") val btsId: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val type: String?,
    val status: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class SitesResponse(
    val success: Boolean?,
    val data: List<SiteDto>?,
    val message: String?
)

data class SiteDetailResponse(
    val success: Boolean?,
    val data: SiteDto?,
    val message: String?
)

// --- Audits ---

data class AuditDto(
    val id: Long?,
    @SerializedName("site_id") val siteId: Long?,
    val type: String?,
    @SerializedName("engineer_name") val engineerName: String?,
    val status: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    val data: Map<String, Any>?
)

data class AuditsResponse(
    val success: Boolean?,
    val data: List<AuditDto>?,
    val message: String?
)

data class AuditDetailResponse(
    val success: Boolean?,
    val data: AuditDto?,
    val message: String?
)

data class CreateAuditRequest(
    @SerializedName("site_id") val siteId: Long,
    val type: String,
    @SerializedName("engineer_name") val engineerName: String,
    val data: Map<String, Any>
)

data class ApiResponse(
    val success: Boolean?,
    val message: String?,
    val data: Any?
)
