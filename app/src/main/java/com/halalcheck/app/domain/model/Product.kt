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
    /** Language of [ingredientsText] ("de", "nb" ...); null if unknown. */
    val ingredientsLanguage: String? = null,
    /** Open Food Facts label tags, e.g. "en:vegan", "en:halal". */
    val labelsTags: List<String> = emptyList(),
    /** Open Food Facts additive tags, e.g. "en:e471". */
    val additivesTags: List<String> = emptyList(),
    /** Ingredients as parsed by Open Food Facts, with taxonomy ids. */
    val sourceIngredients: List<SourceIngredient> = emptyList(),
    /** OFF parses only the product's main language, which may differ from [ingredientsLanguage]. */
    val sourceIngredientsLanguage: String? = null,
    val halalStatus: HalalStatus = HalalStatus.UNKNOWN,
    val confidence: Float = 0f,
    val source: DataSource = DataSource.UNKNOWN,
    val analyzedIngredients: List<AnalyzedIngredient> = emptyList(),
    val unknownIngredients: List<String> = emptyList(),
    /** A halal label is present although a HARAM ingredient was found. */
    val labelConflict: Boolean = false,
    /** What the user confirmed is printed on the package (Open Food Facts often lacks it). */
    val userClaims: Set<PackageClaim> = emptySet(),
    /** Some doubtful ingredients would be resolved by a valid vegan/vegetarian label. */
    val dietLabelWouldResolve: Boolean = false,
    /** Ingredients that contradict a vegan claim (milk, honey, gelatine ...); the claim is then ignored. */
    val veganConflicts: List<String> = emptyList(),
    /** Ingredients that contradict a vegetarian claim (meat, gelatine ...); the claim is then ignored. */
    val vegetarianConflicts: List<String> = emptyList(),
    /** The ingredient text and Open Food Facts' own parse disagree about a doubtful or haram ingredient. */
    val sourceConflict: Boolean = false,
    /** HARAM ingredients the product name mentions ("Vodka") that are not among the analyzed ingredients. */
    val nameMentions: List<Ingredient> = emptyList(),
    /** Unrecognized label text that is no ingredient ("Weitere Informationen unter ra.org"); not counted. */
    val ignoredText: List<String> = emptyList(),
    /** When the user saved a photo of the ingredient list that replaces Open Food Facts' data; null if none. */
    val ingredientsFromPhoto: Long? = null,
    /** With a photo: the Open Food Facts data it replaced, checked for what the photo lacks. */
    val offData: Product? = null,
    /** The user confirmed the photo shows the whole, current list; Open Food Facts is not compared then. */
    val photoConfirmedComplete: Boolean = false,
    /** Doubtful or haram ingredients Open Food Facts lists that the photo lacks (cut off, misread or outdated). */
    val photoMissing: List<AnalyzedIngredient> = emptyList(),
    /** Set when Open Food Facts could not be reached and a cached copy from this time is shown. */
    val offlineSince: Long? = null
) {
    val offVegan: Boolean get() = labelsTags.any { it in KnownLabels.VEGAN }
    val offVegetarian: Boolean get() = labelsTags.any { it in KnownLabels.VEGETARIAN }
    /** A halal label on Open Food Facts: community data, shown as a hint only. */
    val offHalal: Boolean get() = labelsTags.any { it in KnownLabels.HALAL }

    val claimsVegan: Boolean get() = offVegan || PackageClaim.VEGAN in userClaims
    /** A vegetarian claim of its own, not one implied by a vegan claim (which may be wrong). */
    val claimsVegetarian: Boolean get() = (offVegetarian && !offVegan) || PackageClaim.VEGETARIAN in userClaims
    /** Only the user's confirmation counts for the verdict, never the Open Food Facts tag alone. */
    val halalConfirmed: Boolean get() = PackageClaim.HALAL in userClaims
}

/**
 * The user's photo of the ingredient list replaces what Open Food Facts has (or lacks): its text,
 * its own parse and its additive list. Labels stay, they are claims on the package. The replaced data
 * stays in [Product.offData], so a cut-off photo can't silently drop a haram ingredient.
 */
fun Product.withPhotoIngredients(text: String, savedAt: Long): Product = copy(
    offData = takeIf { ingredientsText.isNotBlank() || sourceIngredients.isNotEmpty() || additivesTags.isNotEmpty() },
    ingredientsText = text,
    ingredientsLanguage = null,
    sourceIngredients = emptyList(),
    sourceIngredientsLanguage = null,
    additivesTags = emptyList(),
    ingredientsFromPhoto = savedAt,
    source = if (source == DataSource.UNKNOWN) DataSource.LOCAL else source
)

enum class PackageClaim { VEGAN, VEGETARIAN, HALAL }

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
