package com.telco.btsfieldapp.domain.model

data class AuditRecord(
    val id: Long,
    val siteId: Long,
    val type: String,
    val engineerName: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val synced: Boolean = true,
    val data: AuditData? = null
)

data class AuditData(
    val ground: GroundAudit? = null,
    val dcdb: DcdbAudit? = null,
    val tower: TowerAudit? = null,
    val equipment: EquipmentAudit? = null
)

data class GroundAudit(
    val fenceCondition: String = "",
    val gateLock: String = "",
    val groundResistance: String = "",
    val drainageCondition: String = "",
    val notes: String = "",
    val photos: List<String> = emptyList()
)

data class DcdbAudit(
    val dcdbType: String = "",
    val dcdbCapacity: String = "",
    val cablesCondition: String = "",
    val surgeProtection: String = "",
    val cableEntrySealed: String = "",
    val notes: String = "",
    val photos: List<String> = emptyList()
)

data class TowerAudit(
    val towerType: String = "",
    val towerHeight: String = "",
    val structuralIntegrity: String = "",
    val rustCorrosion: String = "",
    val boltCondition: String = "",
    val lightningRod: String = "",
    val climbSafety: String = "",
    val antennaMounting: String = "",
    val notes: String = "",
    val photos: List<String> = emptyList()
)

data class EquipmentAudit(
    val cabinetCondition: String = "",
    val equipmentModel: String = "",
    val powerSupplyStatus: String = "",
    val cableManagement: String = "",
    val ledIndicators: String = "",
    val temperature: String = "",
    val uptime: String = "",
    val signalStrength: String = "",
    val notes: String = "",
    val photos: List<String> = emptyList()
)
