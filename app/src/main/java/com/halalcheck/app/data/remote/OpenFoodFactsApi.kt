package com.halalcheck.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Open Food Facts API v2.
 * https://world.openfoodfacts.org/api/v2/
 */
interface OpenFoodFactsApi {

    @GET("api/v2/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = PRODUCT_FIELDS
    ): OpenFoodFactsResponse

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
        const val PRODUCT_FIELDS = "product_name,brands,ingredients_text,ingredients_text_de,ingredients_text_en,image_url,image_front_url,categories,labels"
    }
}
