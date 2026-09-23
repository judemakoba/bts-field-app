package com.telco.btsfieldapp.data.remote

import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): LoginResponse

    // Sites
    @GET("sites")
    suspend fun getSites(): SitesResponse

    @GET("sites/{id}")
    suspend fun getSiteDetail(@Path("id") id: Long): SiteDetailResponse

    // Audits
    @GET("audits")
    suspend fun getAudits(@Query("site_id") siteId: Long? = null): AuditsResponse

    @GET("audits/{id}")
    suspend fun getAuditDetail(@Path("id") id: Long): AuditDetailResponse

    @POST("audits")
    suspend fun createAudit(@Body request: CreateAuditRequest): ApiResponse

    @PUT("audits/{id}")
    suspend fun updateAudit(@Path("id") id: Long, @Body request: CreateAuditRequest): ApiResponse

    @DELETE("audits/{id}")
    suspend fun deleteAudit(@Path("id") id: Long): ApiResponse
}
