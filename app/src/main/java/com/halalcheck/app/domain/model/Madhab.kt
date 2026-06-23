package com.halalcheck.app.domain.model

/**
 * Represents the four Sunni schools of Islamic jurisprudence (madhahib).
 * Each madhab may have different rulings on certain food ingredients.
 */
enum class Madhab(
    val displayNameDe: String,
    val displayNameEn: String,
    val displayNameAr: String,
    val displayNameTr: String
) {
    HANAFI(
        displayNameDe = "Hanafitisch",
        displayNameEn = "Hanafi",
        displayNameAr = "حنفي",
        displayNameTr = "Hanefi"
    ),
    MALIKI(
        displayNameDe = "Malikitisch",
        displayNameEn = "Maliki",
        displayNameAr = "مالكي",
        displayNameTr = "Maliki"
    ),
    SHAFI(
        displayNameDe = "Schafiitisch",
        displayNameEn = "Shafi'i",
        displayNameAr = "شافعي",
        displayNameTr = "Şafii"
    ),
    HANBALI(
        displayNameDe = "Hanbalitisch",
        displayNameEn = "Hanbali",
        displayNameAr = "حنبلي",
        displayNameTr = "Hanbeli"
    );

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
