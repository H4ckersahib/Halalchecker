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
    val nameFr: String? = null,
    val synonyms: List<String> = emptyList(),
    val defaultCategory: HalalStatus,
    val hanafiRuling: HalalStatus? = null,
    val malikiRuling: HalalStatus? = null,
    val shafiRuling: HalalStatus? = null,
    val hanbaliRuling: HalalStatus? = null,
    val reason: String? = null,
    val reasonDe: String? = null,
    val source: String? = null,
    val alternatives: List<String> = emptyList(),
    /** Why a MASHBOOH ingredient is doubtful; decides whether a vegan label can resolve it. */
    val doubt: Doubt? = null,
    /** Id of the ancestor whose ruling this ingredient inherited, e.g. "alcohol" for "white_wine". */
    val inferredFrom: String? = null,
    /** A functional class such as "Emulgator" rather than a concrete ingredient. */
    val isClass: Boolean = false,
    /** Own or nearest ancestor E-number id, e.g. "e322i" for soy lecithin. */
    val eNumber: String? = null
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

    fun allNames(): List<String> = listOfNotNull(nameDe, nameEn, nameTr, nameAr, nameFr) + synonyms
}

enum class Doubt {
    /** Could be animal or plant derived (E471, gelatine, aroma). Resolved by a vegan/vegetarian label. */
    ANIMAL_SOURCE,

    /** May contain alcohol (vanilla extract, soy sauce). Not resolved by a vegan label. */
    ALCOHOL,

    /** Slaughter method, madhab differences, mixed products. */
    OTHER;

    companion object {
        fun fromString(value: String?): Doubt? = when (value?.lowercase()) {
            "animal_source" -> ANIMAL_SOURCE
            "alcohol" -> ALCOHOL
            "other" -> OTHER
            else -> null
        }
    }
}
