package com.telco.btsfieldapp.domain.model

/**
 * Tower Equipment Scope audit record.
 * Submitted via POST /api/audit/sync with type="tower_info".
 */
data class TowerInfoRecord(
    // Metadata
    val siteId: String = "",
    val createdAt: String = "",
    val status: String = "pending",

    // ── Antenna Sub-section ──────────────────────────────────────
    val antennas: List<AntennaEntry> = listOf(AntennaEntry(id = "ant_1")),

    // ── RRU Sub-section ──────────────────────────────────────────
    val rrus: List<RruEntry> = listOf(RruEntry(id = "rru_1"))
)

data class AntennaEntry(
    val id: String = "",
    // Equipment
    val equipmentType: String = "", // RF Antenna | MW Antenna | MW ODU | RRU | AAU
    val manufacturer: String = "",
    val modelNumber: String = "",
    val tenantOwner: String = "Airtel", // readonly, fixed to Airtel
    val sector: String = "", // Sec A | Sec B | Sec C | Sec D | N/A
    val azimuth: String = "", // double
    val heightToCentre: String = "", // integer mm
    // Dimensions
    val lengthDia: String = "", // mm
    val width: String = "", // mm
    val height: String = "", // mm
    // Status
    val activeInactive: String = "", // Active | Inactive
    val equipmentLabelling: String = "", // Done | Not Done
    // Photos
    val modelPlatePhoto: String? = null,
    val portsPhoto: String? = null,
    val dimensionsPhoto1: String? = null,
    val dimensionsPhoto2: String? = null,
    val dimensionsPhoto3: String? = null,
    val azimuthPhoto: String? = null,
    val heightPhoto: String? = null
)

data class RruEntry(
    val id: String = "",
    // Equipment
    val equipmentType: String = "", // RF Antenna | MW Antenna | MW ODU | RRU | AAU
    val manufacturer: String = "",
    val modelNumber: String = "",
    val tenantOwner: String = "Airtel", // readonly, fixed to Airtel
    val sector: String = "", // Sec A | Sec B | Sec C | Sec D | N/A
    // Dimensions
    val lengthDia: String = "", // mm
    val width: String = "", // mm
    val height: String = "", // mm
    // Status
    val activeInactive: String = "", // Active | Inactive
    val equipmentLabelling: String = "", // Done | Not Done
    // Photos
    val modelPlatePhoto: String? = null,
    val dimensionsPhoto1: String? = null,
    val dimensionsPhoto2: String? = null,
    val dimensionsPhoto3: String? = null
)
