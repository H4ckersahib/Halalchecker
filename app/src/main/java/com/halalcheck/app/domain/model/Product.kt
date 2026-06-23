package com.halalcheck.app.domain.model

/**
 * Domain model for a scanned product.
 */
data class Product(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val ingredientsText: String = "",
    val labels: String? = null,
    val halalStatus: HalalStatus = HalalStatus.UNKNOWN,
    val confidence: Float = 0f,
    val source: DataSource = DataSource.UNKNOWN,
    val analyzedIngredients: List<AnalyzedIngredient> = emptyList(),
    val unknownIngredients: List<String> = emptyList()
) {
    /**
     * Check if the product has vegan or vegetarian labels.
     * This can be used to resolve MASHBOOH emulsifiers/additives as HALAL.
     */
    fun isLabeledVeganOrVegetarian(): Boolean {
        if (labels.isNullOrBlank()) return false
        val labelsLower = labels.lowercase()
        return labelsLower.contains("vegan") ||
               labelsLower.contains("vegetari") ||
               labelsLower.contains("pflanzlich") ||
               labelsLower.contains("plant-based") ||
               labelsLower.contains("végétalien") ||
               labelsLower.contains("végétarien")
    }
}

/**
 * Represents an ingredient that has been analyzed by the HalalEngine.
 */
data class AnalyzedIngredient(
    val name: String,
    val ingredient: Ingredient?,
    val status: HalalStatus,
    val reason: String? = null
)

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
