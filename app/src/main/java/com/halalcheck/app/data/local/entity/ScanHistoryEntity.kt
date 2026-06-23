package com.halalcheck.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the scan history.
 */
@Entity(tableName = "scan_history")
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
