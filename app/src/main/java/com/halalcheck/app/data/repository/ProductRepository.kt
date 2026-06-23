package com.halalcheck.app.data.repository

import com.halalcheck.app.data.local.dao.ProductDao
import com.halalcheck.app.data.local.dao.ScanHistoryDao
import com.halalcheck.app.data.local.entity.ProductEntity
import com.halalcheck.app.data.local.entity.ScanHistoryEntity
import com.halalcheck.app.data.remote.OpenFoodFactsApi
import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Product
import com.halalcheck.app.domain.model.ScanHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementing the three-tier product lookup:
 * 1. Local cache (Room)
 * 2. Open Food Facts API
 * 3. (Future: Community / Supabase backend)
 */
@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val scanHistoryDao: ScanHistoryDao,
    private val openFoodFactsApi: OpenFoodFactsApi
) {
    /**
     * Look up a product by barcode using the three-tier strategy.
     */
    suspend fun lookupProduct(barcode: String): Result<Product> {
        // Tier 1: Check local cache
        val cached = productDao.getProductByBarcode(barcode)
        if (cached != null) {
            return Result.success(cached.toDomain())
        }

        // Tier 2: Query Open Food Facts API
        return try {
            val response = openFoodFactsApi.getProduct(barcode)
            if (response.status == 1 && response.product != null) {
                val offProduct = response.product
                val product = Product(
                    barcode = barcode,
                    name = offProduct.productName ?: "Unbekanntes Produkt",
                    brand = offProduct.brands,
                    imageUrl = offProduct.getBestImageUrl(),
                    ingredientsText = offProduct.getBestIngredientsText(),
                    labels = offProduct.labels,
                    source = DataSource.OPEN_FOOD_FACTS
                )

                // Cache the product locally
                cacheProduct(product)

                Result.success(product)
            } else {
                // Product not found anywhere
                Result.success(
                    Product(
                        barcode = barcode,
                        name = "Produkt nicht gefunden",
                        source = DataSource.UNKNOWN
                    )
                )
            }
        } catch (e: Exception) {
            // Network error - return a basic product with the barcode
            Result.failure(e)
        }
    }

    /**
     * Cache a product in the local database.
     */
    suspend fun cacheProduct(product: Product) {
        productDao.insertProduct(
            ProductEntity(
                barcode = product.barcode,
                name = product.name,
                brand = product.brand,
                imageUrl = product.imageUrl,
                ingredientsText = product.ingredientsText,
                halalStatus = product.halalStatus.name,
                confidence = product.confidence,
                source = product.source.name,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    /**
     * Save a scan to the history.
     */
    suspend fun saveScanToHistory(product: Product) {
        scanHistoryDao.insertScan(
            ScanHistoryEntity(
                barcode = product.barcode,
                productName = product.name,
                brand = product.brand,
                imageUrl = product.imageUrl,
                halalStatus = product.halalStatus.name,
                scannedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Get recent scan history as Flow.
     */
    fun getRecentScansFlow(limit: Int = 10): Flow<List<ScanHistoryItem>> {
        return scanHistoryDao.getRecentScansFlow(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get all scan history as Flow.
     */
    fun getAllScansFlow(): Flow<List<ScanHistoryItem>> {
        return scanHistoryDao.getAllScansFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get scans filtered by status.
     */
    fun getScansByStatusFlow(status: HalalStatus): Flow<List<ScanHistoryItem>> {
        return scanHistoryDao.getScansByStatusFlow(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Search scan history.
     */
    fun searchScansFlow(query: String): Flow<List<ScanHistoryItem>> {
        return scanHistoryDao.searchScansFlow(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get scan count for today.
     */
    suspend fun getTodayScanCount(): Int {
        val startOfDay = getStartOfDayMillis()
        return scanHistoryDao.getScanCountSince(startOfDay)
    }

    /**
     * Get total scan count.
     */
    suspend fun getTotalScanCount(): Int {
        return scanHistoryDao.getScanCount()
    }

    /**
     * Delete a scan from history.
     */
    suspend fun deleteScan(id: Long) {
        scanHistoryDao.deleteScan(id)
    }

    /**
     * Clear all scan history.
     */
    suspend fun clearHistory() {
        scanHistoryDao.deleteAllScans()
    }

    private fun getStartOfDayMillis(): Long {
        val now = System.currentTimeMillis()
        return now - (now % (24 * 60 * 60 * 1000))
    }
}

// Extension functions for entity-to-domain conversion

fun ProductEntity.toDomain(): Product {
    return Product(
        barcode = barcode,
        name = name,
        brand = brand,
        imageUrl = imageUrl,
        ingredientsText = ingredientsText,
        halalStatus = HalalStatus.fromString(halalStatus),
        confidence = confidence,
        source = try { DataSource.valueOf(source) } catch (e: Exception) { DataSource.UNKNOWN }
    )
}

/**
 * Extension function to convert ScanHistoryEntity to domain model.
 */
fun ScanHistoryEntity.toDomain(): ScanHistoryItem {
    return ScanHistoryItem(
        id = id,
        barcode = barcode,
        productName = productName,
        brand = brand,
        imageUrl = imageUrl,
        halalStatus = HalalStatus.fromString(halalStatus),
        scannedAt = scannedAt
    )
}
