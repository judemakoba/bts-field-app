package com.telco.btsfieldapp.data.remote

import retrofit2.http.*

interface ApiService {

    // ── Auth ────────────────────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): LoginResponse

    // ── Sites ───────────────────────────────────────────────────────────────
    @GET("sites")
    suspend fun getSites(): SitesResponse

    // ── Site Audit (per-site audit data) ────────────────────────────────────
    @GET("audit/site/{siteId}")
    suspend fun getSiteAudit(@Path("siteId") siteId: String): SiteAuditResponse

    // ── Audit Sync (mobile offline queue → server) ───────────────────────────
    @POST("audit/sync")
    suspend fun syncAudit(@Body request: AuditSyncRequest): AuditSyncResponse

    // ── Rejected reports ───────────────────────────────────────────────────
    @GET("audit/rejected")
    suspend fun getRejectedReports(): RejectedReportsResponse

    // ── Individual record endpoints ─────────────────────────────────────────
    @GET("ground")
    suspend fun getGroundRecords(@Query("siteId") siteId: String): RecordListResponse

    @POST("ground")
    suspend fun createGroundRecord(@Body body: Map<String, Any>): RecordResponse

    @GET("dcdb")
    suspend fun getDcdbRecords(@Query("siteId") siteId: String): RecordListResponse

    @POST("dcdb")
    suspend fun createDcdbRecord(@Body body: Map<String, Any>): RecordResponse

    @GET("tower")
    suspend fun getTowerRecords(@Query("siteId") siteId: String): RecordListResponse

    @POST("tower")
    suspend fun createTowerRecord(@Body body: Map<String, Any>): RecordResponse

    // ── Equipment audit ──────────────────────────────────────────────────────
    @GET("equipment")
    suspend fun getEquipmentRecords(@Query("siteId") siteId: String): RecordListResponse

    @POST("equipment")
    suspend fun createEquipmentRecord(@Body body: Map<String, Any>): RecordResponse
}
