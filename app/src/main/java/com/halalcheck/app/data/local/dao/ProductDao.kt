package com.halalcheck.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halalcheck.app.data.local.entity.ProductEntity

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("UPDATE products SET halal_status = :halalStatus, confidence = :confidence WHERE barcode = :barcode")
    suspend fun updateAnalysis(barcode: String, halalStatus: String, confidence: Float)
}
