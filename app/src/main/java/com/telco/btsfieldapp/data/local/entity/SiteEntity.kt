package com.telco.btsfieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val siteId: String,
    val name: String,
    val address: String,
    val type: String,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: String,
    val updatedAt: String
)
