package com.halalcheck.app.domain.model

/**
 * Domain model for a scanned product.
 */
data class Product(
    val barcode: String,
    /** Blank if unknown; the UI shows a localized fallback. */
    val name: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val ingredientsText: String = "",
    /** Open Food Facts label tags, e.g. "en:vegan", "en:halal". */
    val labelsTags: List<String> = emptyList(),
    /** Open Food Facts additive tags, e.g. "en:e471". */
    val additivesTags: List<String> = emptyList(),
    /** Ingredients as parsed by Open Food Facts, with taxonomy ids. */
    val sourceIngredients: List<SourceIngredient> = emptyList(),
    val halalStatus: HalalStatus = HalalStatus.UNKNOWN,
    val confidence: Float = 0f,
    val source: DataSource = DataSource.UNKNOWN,
    val analyzedIngredients: List<AnalyzedIngredient> = emptyList(),
    val unknownIngredients: List<String> = emptyList(),
    /** A halal label is present although a HARAM ingredient was found. */
    val labelConflict: Boolean = false
) {
    val hasVeganOrVegetarianLabel: Boolean
        get() = labelsTags.any { it.contains("vegan") || it.contains("vegetarian") }

    val hasHalalLabel: Boolean
        get() = labelsTags.any { it.contains("halal") }
}

/** An ingredient node from Open Food Facts' own parse ("id" is a taxonomy id like "en:soya-lecithin"). */
data class SourceIngredient(
    val id: String?,
    val text: String,
    val children: List<SourceIngredient> = emptyList()
)

/**
 * Represents an ingredient that has been analyzed by the HalalEngine.
 */
data class AnalyzedIngredient(
    val name: String,
    val ingredient: Ingredient?,
    val status: HalalStatus,
    val reason: Reason? = null,
    /** Matched only via the last part of a compound word, e.g. "Putenschinken" -> "Schinken". */
    val inferred: Boolean = false
)

/** Why an ingredient got its status. The UI turns this into localized text. */
sealed interface Reason {
    /** The ingredient's own database reason (or its source, if it has no reason text). */
    data class Database(val ingredient: Ingredient) : Reason

    /** Matched via the head of a compound word. */
    data class Inferred(val ingredient: Ingredient) : Reason

    /** A functional class like "Emulgator" without the concrete additive. */
    data class NotSpecified(val ingredient: Ingredient) : Reason

    /** Not in the ingredient list text, but listed by Open Food Facts as an additive. */
    data class OffAdditive(val ingredient: Ingredient) : Reason

    data class HaramQualifier(val qualifier: String) : Reason
    data class AnimalQualifier(val qualifier: String) : Reason
    /** A plant, synthetic or otherwise unproblematic source (Soja, pflanzlich, Fisch). */
    data class HalalSourceQualifier(val qualifier: String) : Reason
    data object VeganLabel : Reason
    data object HalalLabel : Reason
}

/**
 * The source of the product data.
 */
enum class DataSource {
    LOCAL,
    OPEN_FOOD_FACTS,
    COMMUNITY,
    UNKNOWN
}

/**
 * Represents a scan history entry.
 */
data class ScanHistoryItem(
    val id: Long = 0,
    val barcode: String,
    val productName: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val halalStatus: HalalStatus,
    val scannedAt: Long = System.currentTimeMillis()
)
