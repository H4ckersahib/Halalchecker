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

@JsonClass(generateAdapter = true)
data class OpenFoodFactsProduct(
    @Json(name = "product_name")
    val productName: String? = null,

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

    @Json(name = "categories")
    val categories: String? = null,

    @Json(name = "labels")
    val labels: String? = null
) {
    /**
     * Get the best available ingredients text.
     * Prefers German, then generic, then English.
     */
    fun getBestIngredientsText(): String {
        return ingredientsTextDe
            ?: ingredientsText
            ?: ingredientsTextEn
            ?: ""
    }

    /**
     * Get the best available image URL.
     */
    fun getBestImageUrl(): String? {
        return imageFrontUrl ?: imageUrl
    }
}
