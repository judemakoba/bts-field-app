package com.telco.btsfieldapp.domain.model

data class AuditRecord(
    val id: Long,
    val siteId: Long,
    val type: String,
    val engineerName: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val synced: Boolean = true
)

data class GroundRecord(
    val id: String,
    val siteId: String,
    val type: String = "ground",
    val fenceCondition: String = "",
    val gateLock: String = "",
    val groundResistance: String = "",
    val drainageCondition: String = "",
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class DcdbRecord(
    val id: String,
    val siteId: String,
    val type: String = "dcdb",
    val dcdbType: String = "",
    val dcdbCapacity: String = "",
    val cablesCondition: String = "",
    val surgeProtection: String = "",
    val cableEntrySealed: String = "",
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class TowerRecord(
    val id: String,
    val siteId: String,
    val type: String = "tower",
    val towerType: String = "",
    val towerHeight: String = "",
    val structuralIntegrity: String = "",
    val rustCorrosion: String = "",
    val boltCondition: String = "",
    val lightningRod: String = "",
    val climbSafety: String = "",
    val antennaMounting: String = "",
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class EquipmentRecord(
    val id: String,
    val siteId: String,
    val type: String = "equipment",
    val cabinetCondition: String = "",
    val equipmentModel: String = "",
    val powerSupplyStatus: String = "",
    val cableManagement: String = "",
    val ledIndicators: String = "",
    val temperature: String = "",
    val uptime: String = "",
    val signalStrength: String = "",
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
