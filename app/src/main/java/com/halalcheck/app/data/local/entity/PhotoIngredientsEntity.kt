package com.halalcheck.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An ingredient list the user photographed (text recognition on the device, then corrected by the
 * user). One per product; it replaces the Open Food Facts ingredients for that barcode.
 */
@Entity(tableName = "photo_ingredients")
data class PhotoIngredientsEntity(
    @PrimaryKey
    @ColumnInfo(name = "barcode")
    val barcode: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
