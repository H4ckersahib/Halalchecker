package com.halalcheck.app.domain.model

/**
 * Represents the halal status of a product or ingredient.
 */
enum class HalalStatus {
    /** Clearly permissible - no problematic ingredients */
    HALAL,

    /** Clearly prohibited - contains haram ingredients */
    HARAM,

    /** Doubtful - contains ingredients whose status depends on source/processing */
    MASHBOOH,

    /** Unknown - ingredient not found in the database */
    UNKNOWN;

    companion object {
        fun fromString(value: String): HalalStatus {
            return when (value.uppercase().trim()) {
                "HALAL" -> HALAL
                "HARAM" -> HARAM
                "MASHBOOH", "MUSHBOOH", "DOUBTFUL" -> MASHBOOH
                else -> UNKNOWN
            }
        }
    }
}
