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
    // Legacy ground inspection
    val fenceCondition: String = "",
    val gateLock: String = "",
    val groundResistance: String = "",
    val drainageCondition: String = "",
    // Site / Tower Info
    val atcId: String = "",
    val towerType: String = "",
    val towerHeight: String = "",
    val buildingHeight: String = "0",
    val totalHeight: String = "0",
    val siteIndoorOutdoor: String = "",
    val noOfTenants: String = "",
    val otherTenants: List<String> = emptyList(),
    val latitude: String = "",
    val longitude: String = "",
    val gpsAccuracy: String = "",
    val altitude: String = "",
    val gpsScreenshot: String? = null,
    // Survey Details
    val surveyDate: String = "",
    val technicianName: String = "",
    val technicianContacts: String = "",
    val contractorName: String = "Innovis",
    // Power Infrastructure
    val hasGrid: Boolean = false,
    val hasDG: Boolean = false,
    val hasSolar: Boolean = false,
    val gridDistanceTo3Phase: String = "",
    // RRU & Cabinets
    val guardAtSite: Boolean = false,
    val rruType: String = "",
    val rruCount: String = "",
    val cabinetTypes: String = "",
    val cabinetCount: String = "",
    val equipmentLabelled: Boolean = false,
    val cabinetComments: String = "",
    val cabinetDimensionsLxW: String = "",
    val activeIduTypes: String = "",
    val nonActiveIduTypes: String = "",
    val nonActiveIduCount: String = "",
    // Slab
    val slabDimensions: String = "",
    // Redundant
    val redundantEquipmentCount: String = "",
    val redundantItemName: String = "",
    // Media & Remarks
    val isOnFiber: Boolean? = null,
    val notes: String = "",
    // Metadata
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
