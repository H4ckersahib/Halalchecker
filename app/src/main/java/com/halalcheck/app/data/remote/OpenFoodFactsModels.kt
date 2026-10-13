package com.halalcheck.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenFoodFactsResponse(
    @Json(name = "status")
    val status: Int = 0,

    @Json(name = "status_verbose")
    val statusVerbose: String? = null,

    @Json(name = "product")
    val product: OpenFoodFactsProduct? = null
)

/** Also stored as JSON in ProductEntity.offPayload. */
@JsonClass(generateAdapter = true)
data class OpenFoodFactsProduct(
    @Json(name = "product_name")
    val productName: String? = null,

    @Json(name = "product_name_de")
    val productNameDe: String? = null,

    @Json(name = "product_name_en")
    val productNameEn: String? = null,

    @Json(name = "brands")
    val brands: String? = null,

    @Json(name = "ingredients_text")
    val ingredientsText: String? = null,

    @Json(name = "ingredients_text_de")
    val ingredientsTextDe: String? = null,

    @Json(name = "ingredients_text_en")
    val ingredientsTextEn: String? = null,

    @Json(name = "image_url")
    val imageUrl: String? = null,

    @Json(name = "image_front_url")
    val imageFrontUrl: String? = null,

    @Json(name = "labels_tags")
    val labelsTags: List<String>? = null,

    @Json(name = "additives_tags")
    val additivesTags: List<String>? = null,

    @Json(name = "ingredients")
    val ingredients: List<OpenFoodFactsIngredient>? = null
) {
    /**
     * Best non-blank ingredients text: the app language first, then German, then the product's main
     * language, then English. OFF often sends "" for languages it has no text for.
     */
    fun getBestIngredientsText(language: String): String {
        val byLanguage = mapOf("de" to ingredientsTextDe, "en" to ingredientsTextEn)
        return listOf(byLanguage[language], ingredientsTextDe, ingredientsText, ingredientsTextEn)
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
    }

    fun getBestName(language: String): String {
        val byLanguage = mapOf("de" to productNameDe, "en" to productNameEn)
        return listOf(byLanguage[language], productName, productNameDe, productNameEn)
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
    }

    /**
     * Get the best available image URL.
     */
    fun getBestImageUrl(): String? {
        return imageFrontUrl?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }
    }
}

/** One node of Open Food Facts' own ingredient parse. */
@JsonClass(generateAdapter = true)
data class OpenFoodFactsIngredient(
    @Json(name = "id")
    val id: String? = null,

    @Json(name = "text")
    val text: String? = null,

    @Json(name = "ingredients")
    val ingredients: List<OpenFoodFactsIngredient>? = null
)
