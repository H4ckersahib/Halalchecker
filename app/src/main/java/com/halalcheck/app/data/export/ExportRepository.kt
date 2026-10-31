package com.halalcheck.app.data.export

import android.content.Context
import com.halalcheck.app.BuildConfig
import com.halalcheck.app.data.local.dao.ScanHistoryDao
import com.halalcheck.app.data.preferences.UserPreferencesRepository
import com.halalcheck.app.data.repository.FeedbackRepository
import com.halalcheck.app.data.repository.IngredientRepository
import com.halalcheck.app.data.repository.PhotoIngredientsRepository
import com.halalcheck.app.data.repository.ProductRepository
import com.halalcheck.app.domain.engine.HalalEngine
import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Product
import com.halalcheck.app.domain.model.withPhotoIngredients
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes the test data export: every product in the history plus every feedback, re-analyzed with
 * the current rules from the cached product data. No network access. The file stays in the app's
 * cache until the user shares it.
 */
@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanHistoryDao: ScanHistoryDao,
    private val productRepository: ProductRepository,
    private val feedbackRepository: FeedbackRepository,
    private val ingredientRepository: IngredientRepository,
    private val photoRepository: PhotoIngredientsRepository,
    private val preferences: UserPreferencesRepository,
    private val halalEngine: HalalEngine,
    private val moshi: Moshi
) {
    suspend fun export(): File {
        val madhab = preferences.madhab.first()
        val index = ingredientRepository.getIndex()
        return withContext(Dispatchers.IO) {
            val history = scanHistoryDao.getAllScans().associateBy { it.barcode }
            val feedback = feedbackRepository.getAllFeedback().associateBy { it.barcode }
            val entries = (history.keys + feedback.keys).map { barcode ->
                val scan = history[barcode]
                // Same data as the result screen: the user's photo replaces the Open Food Facts ingredients
                val photo = photoRepository.get(barcode)
                val cached = productRepository.cachedProduct(barcode)
                    ?: photo?.let { Product(barcode = barcode, name = scan?.productName.orEmpty(), source = DataSource.UNKNOWN) }
                val product = cached?.let { base ->
                    val withPhoto = photo?.let {
                        base.withPhotoIngredients(it.text, it.savedAt).copy(photoConfirmedComplete = preferences.isPhotoComplete(barcode))
                    } ?: base
                    halalEngine.analyzeProduct(withPhoto.copy(userClaims = preferences.packageClaims(barcode)), index, madhab)
                }
                ExportBuilder.Entry(
                    barcode = barcode,
                    productName = scan?.productName,
                    brand = scan?.brand,
                    scannedAt = scan?.scannedAt,
                    verdictShown = scan?.halalStatus?.let { HalalStatus.fromString(it) },
                    product = product,
                    feedback = feedback[barcode]
                )
            }
            val now = System.currentTimeMillis()
            val export = ExportBuilder.build(
                ExportBuilder.Meta(
                    appVersion = BuildConfig.VERSION_NAME,
                    appBuild = BuildConfig.VERSION_CODE,
                    ingredientsDbVersion = preferences.ingredientsDbVersion(),
                    exportedAt = now,
                    tester = preferences.testerName.first(),
                    madhab = madhab
                ),
                entries
            )
            val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // only the latest export is kept
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(Date(now))
            File(dir, "halalcheck-export-$stamp.json").apply { writeText(ExportBuilder.toJson(moshi, export)) }
        }
    }

    companion object {
        /** Shared through the FileProvider (res/xml/file_paths.xml). */
        const val EXPORT_DIR = "exports"
    }
}
