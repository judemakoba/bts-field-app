package com.telco.btsfieldapp.domain.model

/**
 * Ground Equipment Scope audit record.
 * Submitted via POST /api/audit/sync with type="ground_equipment".
 */
data class GroundEquipmentRecord(
    // Section 1: Site Identification
    val atcId: String = "",
    val siteNamePlatePhotoPath: String? = null,

    // Section 2: Survey Details
    val surveyDate: String = "",         // auto-filled: ISO-8601
    val technicianName: String = "",      // auto-filled from account
    val technicianContacts: String = "", // auto-filled from account
    val contractorName: String = "Innovis",

    // Section 3: Site / Tower Info
    val latitude: String = "",
    val longitude: String = "",
    val gpsAccuracy: String = "",
    val altitude: String = "",
    val gpsScreenshotPath: String? = null,
    val towerType: String = "",          // GBT | RTT | RTP
    val towerHeight: String = "",        // integer in metres
    val buildingHeight: String = "",      // integer in metres, default 0
    val totalHeight: String = "",         // auto-calculated
    val siteIndoorOutdoor: String = "",   // Indoor | Outdoor
    val noOfTenants: String = "",        // integer
    val otherTenants: List<String> = emptyList(), // Lyca | MTN | UTL | Savanna | Other
    val sitePhotoPath: String? = null,

    // Section 4: Power Infrastructure
    val hasGrid: Boolean = false,
    val hasDG: Boolean = false,
    val hasSolar: Boolean = false,
    val gridDistanceTo3Phase: String = "", // metres, integer

    // Section 5: RRU & Cabinets
    val guardAtSite: Boolean = false,
    val rruType: String = "",
    val rruPhotosPaths: List<String> = emptyList(), // up to 4
    val rruCount: Int = 0,
    val cabinetTypes: String = "",
    val cabinetPhotosPaths: List<String> = emptyList(), // up to 20
    val cabinetCount: Int = 0,
    val equipmentLabelled: Boolean = false,
    val cabinetComments: String = "",
    val cabinetDimensionsLxW: String = "", // e.g. "0.6x0.4"
    val cabinetDimensionPhotos: List<String> = emptyList(), // L, W, H — 3
    val activeIduTypes: String = "",
    val nonActiveIduTypes: String = "",
    val nonActiveIduCount: Int = 0,
    val nonActiveIduPhotos: List<String> = emptyList(), // up to 5
    val slabDimensions: String = "",      // e.g. "1.2x0.8"
    val slabPhotos: List<String> = emptyList(), // overview + 2 dims — 3

    // Section 6: Redundant Equipment
    val redundantEquipmentCount: Int = 0,
    val redundantItemName: String = "",
    val redundantPhotos: List<String> = emptyList(), // up to 3

    // Section 7: Media & Remarks
    val isOnFiber: Boolean? = null,       // true/false/null (null = not answered)
    val overallRemarks: String = "",

    // Metadata
    val siteId: String = "",
    val createdAt: String = "",
    val status: String = "pending"
)
