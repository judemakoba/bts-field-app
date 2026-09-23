package com.telco.btsfieldapp.domain.model

data class Site(
    val id: Long,
    val name: String,
    val btsId: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val type: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
