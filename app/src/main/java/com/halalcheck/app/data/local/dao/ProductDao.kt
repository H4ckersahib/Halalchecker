package com.halalcheck.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halalcheck.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products ORDER BY last_updated DESC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'")
    suspend fun searchProducts(query: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE barcode = :barcode")
    suspend fun deleteProduct(barcode: String)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}
