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
    val ingredients: List<OpenFoodFactsIngredient>? = null,

    /** The product's main language; [ingredientsText] and OFF's parse [ingredients] are in it. */
    @Json(name = "lang")
    val lang: String? = null
) {
    /**
     * Best non-blank ingredients text: the app language first, then German, then English, then the
     * product's main language (Norwegian, French ... for imported goods). OFF often sends "" for
     * languages it has no text for.
     */
    fun getBestIngredientsText(language: String): String = bestIngredientsText(language)?.second.orEmpty()

    /** Language of [getBestIngredientsText]; null if unknown (no main language in old cached data). */
    fun getBestIngredientsLanguage(language: String): String? = bestIngredientsText(language)?.first

    private fun bestIngredientsText(language: String): Pair<String?, String>? {
        val byLanguage = mapOf("de" to ingredientsTextDe, "en" to ingredientsTextEn)
        return listOf(language to byLanguage[language], "de" to ingredientsTextDe, "en" to ingredientsTextEn, lang to ingredientsText)
            .firstOrNull { !it.second.isNullOrBlank() }
            ?.let { (textLanguage, text) -> textLanguage to text.orEmpty() }
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
