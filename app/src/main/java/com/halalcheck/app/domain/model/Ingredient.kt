package com.halalcheck.app.domain.model

/**
 * Domain model for an ingredient with madhab-specific rulings.
 */
data class Ingredient(
    val id: String,
    val nameDe: String,
    val nameEn: String,
    val nameTr: String? = null,
    val nameAr: String? = null,
    val defaultCategory: HalalStatus,
    val hanafiRuling: HalalStatus? = null,
    val malikiRuling: HalalStatus? = null,
    val shafiRuling: HalalStatus? = null,
    val hanbaliRuling: HalalStatus? = null,
    val reason: String? = null,
    val reasonDe: String? = null,
    val source: String? = null,
    val alternatives: List<String> = emptyList()
) {
    /**
     * Get the ruling for a specific madhab.
     * Falls back to the default category if no madhab-specific ruling exists.
     */
    fun getRulingForMadhab(madhab: Madhab): HalalStatus {
        return when (madhab) {
            Madhab.HANAFI -> hanafiRuling ?: defaultCategory
            Madhab.MALIKI -> malikiRuling ?: defaultCategory
            Madhab.SHAFI -> shafiRuling ?: defaultCategory
            Madhab.HANBALI -> hanbaliRuling ?: defaultCategory
        }
    }

    /**
     * Returns the localized name for the given language code.
     */
    fun getLocalizedName(languageCode: String): String {
        return when (languageCode) {
            "de" -> nameDe
            "en" -> nameEn
            "tr" -> nameTr ?: nameEn
            "ar" -> nameAr ?: nameEn
            else -> nameEn
        }
    }
}
