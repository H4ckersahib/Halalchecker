package com.halalcheck.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached products that have been looked up.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    @ColumnInfo(name = "barcode")
    val barcode: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "brand")
    val brand: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "ingredients_text")
    val ingredientsText: String = "",

    @ColumnInfo(name = "halal_status")
    val halalStatus: String = "UNKNOWN",

    @ColumnInfo(name = "confidence")
    val confidence: Float = 0f,

    @ColumnInfo(name = "source")
    val source: String = "UNKNOWN",

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis(),

    /** Moshi JSON of the fetched OpenFoodFactsProduct, so labels and structured ingredients survive caching. */
    @ColumnInfo(name = "off_payload")
    val offPayload: String? = null
)
