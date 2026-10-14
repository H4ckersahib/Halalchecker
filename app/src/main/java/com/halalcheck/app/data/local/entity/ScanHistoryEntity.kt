package com.halalcheck.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the scan history. One row per product: scanning it again replaces the row.
 */
@Entity(
    tableName = "scan_history",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "barcode")
    val barcode: String,

    @ColumnInfo(name = "product_name")
    val productName: String,

    @ColumnInfo(name = "brand")
    val brand: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "halal_status")
    val halalStatus: String,

    @ColumnInfo(name = "scanned_at")
    val scannedAt: Long = System.currentTimeMillis()
)
