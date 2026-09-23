package com.telco.btsfieldapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audits")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val siteId: Long,
    val type: String,
    val engineerName: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val synced: Boolean = true,
    val dataJson: String = "{}"
)
