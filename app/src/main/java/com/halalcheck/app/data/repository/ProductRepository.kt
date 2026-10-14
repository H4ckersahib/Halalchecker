package com.halalcheck.app.data.repository

import com.halalcheck.app.data.local.dao.ProductDao
import com.halalcheck.app.data.local.dao.ScanHistoryDao
import com.halalcheck.app.data.local.entity.ProductEntity
import com.halalcheck.app.data.local.entity.ScanHistoryEntity
import com.halalcheck.app.data.remote.OpenFoodFactsApi
import com.halalcheck.app.data.remote.OpenFoodFactsIngredient
import com.halalcheck.app.data.remote.OpenFoodFactsProduct
import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Product
import com.halalcheck.app.domain.model.ScanHistoryItem
import com.halalcheck.app.domain.model.SourceIngredient
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Product lookup:
 * 1. Local cache (Room), if fresh
 * 2. Open Food Facts API
 * 3. Stale cache when offline
 * (Future: own backend and community, see spec.md)
 */
@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val scanHistoryDao: ScanHistoryDao,
    private val openFoodFactsApi: OpenFoodFactsApi,
    moshi: Moshi
) {
    private val payloadAdapter = moshi.adapter(OpenFoodFactsProduct::class.java)

    /**
     * Look up a product by barcode. A product Open Food Facts does not know is a success with
     * [DataSource.UNKNOWN]; only network problems without a cached copy are failures.
     * [forceRefresh] skips a fresh cache, e.g. after the user corrected the data on Open Food Facts.
     */
    suspend fun lookupProduct(barcode: String, forceRefresh: Boolean = false): Result<Product> {
        val language = Locale.getDefault().language
        val cached = productDao.getProductByBarcode(barcode)
        val cachedProduct = cached?.let { toCachedProduct(it, language) }?.copy(offlineSince = cached.lastUpdated)
        if (!forceRefresh && cachedProduct != null && System.currentTimeMillis() - cached.lastUpdated < CACHE_MAX_AGE_MS) {
            return Result.success(cachedProduct.copy(offlineSince = null))
        }

        return try {
            val response = openFoodFactsApi.getProduct(barcode)
            val offProduct = response.product
            if (response.status == 1 && offProduct != null) {
                cacheProduct(barcode, offProduct, language)
                Result.success(offProduct.toDomain(barcode, language))
            } else {
                Result.success(notFound(barcode))
            }
        } catch (e: HttpException) {
            if (e.code() == HTTP_NOT_FOUND) Result.success(notFound(barcode)) else cachedOrFailure(cachedProduct, e)
        } catch (e: IOException) {
            cachedOrFailure(cachedProduct, e)
        } catch (e: JsonDataException) {
            cachedOrFailure(cachedProduct, e)
        }
    }

    /** Offline: the cached copy, marked with when it was fetched so the UI can say so. */
    private fun cachedOrFailure(cached: Product?, error: Exception): Result<Product> =
        cached?.let { Result.success(it) } ?: Result.failure(error)

    /** The cached product without network access (for the export), or null if it was never fetched. */
    suspend fun cachedProduct(barcode: String): Product? =
        productDao.getProductByBarcode(barcode)?.let { toCachedProduct(it, Locale.getDefault().language) }

    private fun toCachedProduct(entity: ProductEntity, language: String): Product? =
        entity.offPayload
            ?.let { payload -> runCatching { payloadAdapter.fromJson(payload) }.getOrNull() }
            ?.toDomain(entity.barcode, language)

    private fun notFound(barcode: String) = Product(barcode = barcode, name = "", source = DataSource.UNKNOWN)

    private suspend fun cacheProduct(barcode: String, offProduct: OpenFoodFactsProduct, language: String) {
        productDao.insertProduct(
            ProductEntity(
                barcode = barcode,
                name = offProduct.getBestName(language),
                brand = offProduct.brands,
                imageUrl = offProduct.getBestImageUrl(),
                ingredientsText = offProduct.getBestIngredientsText(language),
                source = DataSource.OPEN_FOOD_FACTS.name,
                lastUpdated = System.currentTimeMillis(),
                offPayload = payloadAdapter.toJson(offProduct)
            )
        )
    }

    /**
     * Store the analysis result of a cached product.
     */
    suspend fun saveAnalysis(product: Product) {
        productDao.updateAnalysis(product.barcode, product.halalStatus.name, product.confidence)
    }

    /**
     * Save a scan to the history. An earlier scan of the same product is replaced.
     */
    suspend fun saveScanToHistory(product: Product) {
        scanHistoryDao.insertScan(
            ScanHistoryEntity(
                barcode = product.barcode,
                productName = product.name.ifBlank { product.barcode },
                brand = product.brand,
                imageUrl = product.imageUrl,
                halalStatus = product.halalStatus.name,
                scannedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Update the history entry of a product that was opened again (not scanned), e.g. from the
     * history, so it shows the current result without moving to the top.
     */
    suspend fun updateHistoryEntry(product: Product) {
        scanHistoryDao.updateScan(
            barcode = product.barcode,
            productName = product.name.ifBlank { product.barcode },
            brand = product.brand,
            imageUrl = product.imageUrl,
            halalStatus = product.halalStatus.name
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
     * Search the history by product name, optionally only one status. Both filters apply together.
     */
    fun searchScansFlow(query: String, status: HalalStatus?): Flow<List<ScanHistoryItem>> {
        return scanHistoryDao.searchScansFlow(query.trim(), status?.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get scan count for today (local time).
     */
    suspend fun getTodayScanCount(): Int {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return scanHistoryDao.getScanCountSince(startOfDay)
    }

    /** Number of products in the history, updated live. */
    val scanCountFlow: Flow<Int> get() = scanHistoryDao.countFlow()

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

    private companion object {
        const val HTTP_NOT_FOUND = 404
        val CACHE_MAX_AGE_MS = TimeUnit.DAYS.toMillis(7)
    }
}

// Extension functions for remote/entity-to-domain conversion

fun OpenFoodFactsProduct.toDomain(barcode: String, language: String): Product {
    return Product(
        barcode = barcode,
        name = getBestName(language),
        brand = brands?.takeIf { it.isNotBlank() },
        imageUrl = getBestImageUrl(),
        ingredientsText = getBestIngredientsText(language),
        labelsTags = labelsTags.orEmpty(),
        additivesTags = additivesTags.orEmpty(),
        sourceIngredients = ingredients.orEmpty().mapNotNull { it.toDomain() },
        source = DataSource.OPEN_FOOD_FACTS
    )
}

/** Keeps nodes without text if they have a taxonomy id or children ("en:pork" must not get lost). */
private fun OpenFoodFactsIngredient.toDomain(): SourceIngredient? {
    val children = ingredients.orEmpty().mapNotNull { it.toDomain() }
    val displayText = text?.takeIf { it.isNotBlank() }
        ?: id?.substringAfter(':')?.replace('-', ' ')
        ?: if (children.isNotEmpty()) "" else return null
    return SourceIngredient(id = id, text = displayText, children = children)
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
