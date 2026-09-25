package com.telco.btsfieldapp.domain.model

/**
 * DCDB Information audit record.
 * Submitted via POST /api/audit/sync with type="dcdb_info".
 */
data class DcdbInfoRecord(
    // Header / metadata
    val siteId: String = "",
    val createdAt: String = "",
    val status: String = "pending",

    // ── DCDB Sub-section ────────────────────────────────────────────
    val gridDistanceTo3Phase: String = "", // metres, pulled from Ground Equipment Power section

    // Non-Priority cable
    val npCableSizeDcdb: String = "",       // mm², integer
    val npBreaker1Mcb: String = "",         // A, integer
    val npDcdus: List<DcduSlot> = emptyList(), // DCDU1, DCDU2, DCDU3...
    val npSectionPhotoPath: String? = null,
    val npLoadMeasurement: String = "",     // A, integer
    val npLoadPhotoPath: String? = null,
    val npLoadMeasuredTime: String = "",    // auto-captured HH:mm:ss AM/PM

    // Priority cable
    val pCableSizeDcdb: String = "",
    val pBreaker1Mcb: String = "",
    val pDcdus: List<DcduSlot> = emptyList(),
    val pSectionPhotoPath: String? = null,
    val pLoadMeasurement: String = "",
    val pLoadPhotoPath: String? = null,
    val pLoadMeasuredTime: String = "",

    // ── DCDU Sub-section ───────────────────────────────────────────
    val npDcdusConnections: List<DcduConnection> = emptyList(), // breaker ID strings + photos
    val pDcdusConnections: List<DcduConnection> = emptyList(),
    val totalDcdUCount: String = "", // integer, auto-tallied but editable

    // ── RRU Sub-section ────────────────────────────────────────────
    val rruCount: String = "",
    val rruPowerCableCount: String = "",
    val rruPowerCableMissing: String = "",
    val rruPowerCableLengthPerRun: String = "",  // m
    val rruPowerCableTotalMissing: String = "",   // m
    val rruEarthingCableCount: String = "",
    val rruEarthingCableMissing: String = "",
    val rruEarthingCableLengthPerRun: String = "", // m

    // ── AAU Sub-section ────────────────────────────────────────────
    val aauCount: String = "",
    val aauPowerCableCount: String = "",
    val aauPowerCableMissing: String = "",
    val aauPowerCableLengthPerRun: String = "",
    val aauPowerCableTotalMissing: String = "",
    val aauEarthingCableCount: String = "",
    val aauEarthingCableMissing: String = "",
    val aauEarthingCableLengthPerRun: String = "",

    // ── BTS Earthing Sub-section ──────────────────────────────────
    val btsEarthingCableCount: String = "",
    val btsEarthingCableMissing: String = "",
    val btsEarthingLengthPerRun: String = "",   // m
    val btsEarthingTotalMissing: String = ""    // m
)

/** Single DCDU slot in the DCDB cable section (upstream of DCDB) */
data class DcduSlot(
    val label: String,           // "DCDU1", "DCDU2", "DCDU3"...
    val cableSize: String = "",  // mm²
    val breakerRating: String = "" // A
)

/** A DCDU breaker connection entry (downstream from DCDB) */
data class DcduConnection(
    val id: String,              // auto-generated incremental ID
    val breakerLabel: String = "", // e.g. "DCDB12A"
    val photoPath: String? = null
)
