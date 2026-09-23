package com.telco.btsfieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id: Long,
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
