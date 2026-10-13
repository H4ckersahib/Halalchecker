package com.halalcheck.app.domain.model

/**
 * Represents the four Sunni schools of Islamic jurisprudence (madhahib).
 * Each madhab may have different rulings on certain food ingredients.
 * Localized names are string resources (ui/components/MadhabName.kt); the Arabic name is shown as is.
 */
enum class Madhab(val displayNameAr: String) {
    HANAFI("حنفي"),
    MALIKI("مالكي"),
    SHAFI("شافعي"),
    HANBALI("حنبلي");

    companion object {
        fun fromString(value: String): Madhab {
            return when (value.uppercase().trim()) {
                "HANAFI" -> HANAFI
                "MALIKI" -> MALIKI
                "SHAFI", "SHAFII", "SHAFI'I" -> SHAFI
                "HANBALI" -> HANBALI
                else -> HANAFI // Default
            }
        }
    }
}
