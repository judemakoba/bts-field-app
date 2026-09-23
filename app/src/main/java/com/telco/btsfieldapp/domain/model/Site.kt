package com.telco.btsfieldapp.domain.model

data class Site(
    val siteId: String,
    val name: String,
    val address: String,
    val type: String,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: String,
    val updatedAt: String
)
