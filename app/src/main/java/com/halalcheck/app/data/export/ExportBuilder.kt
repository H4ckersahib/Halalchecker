package com.halalcheck.app.data.export

import com.halalcheck.app.domain.model.AnalyzedIngredient
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.Product
import com.halalcheck.app.domain.model.ProductFeedback
import com.halalcheck.app.domain.model.Reason
import com.squareup.moshi.Moshi
import java.time.Instant

/** Turns history, analyses and feedback into the export JSON. No Android dependencies. */
object ExportBuilder {

    class Meta(
        val appVersion: String,
        val appBuild: Int,
        val ingredientsDbVersion: Int?,
        val exportedAt: Long,
        val tester: String?,
        val madhab: Madhab
    )

    class Entry(
        val barcode: String,
        val productName: String?,
        val brand: String?,
        val scannedAt: Long?,
        val verdictShown: HalalStatus?,
        /** Analyzed with the current rules; null if the product data is not cached. */
        val product: Product?,
        val feedback: ProductFeedback?
    )

    fun build(meta: Meta, entries: List<Entry>): ExportFile = ExportFile(
        appVersion = meta.appVersion,
        appBuild = meta.appBuild,
        ingredientsDbVersion = meta.ingredientsDbVersion,
        exportedAt = iso(meta.exportedAt),
        tester = meta.tester?.trim()?.ifBlank { null },
        madhab = meta.madhab.name,
        scans = entries.map(::scan)
    )

    fun toJson(moshi: Moshi, file: ExportFile): String =
        moshi.adapter(ExportFile::class.java).indent("  ").toJson(file)

    private fun scan(entry: Entry): ExportScan {
        val product = entry.product
        return ExportScan(
            barcode = entry.barcode,
            productName = entry.productName ?: product?.name,
            brand = entry.brand ?: product?.brand,
            scannedAt = entry.scannedAt?.let(::iso),
            verdictShown = entry.verdictShown?.name,
            verdict = product?.halalStatus?.name,
            ingredientsRecognized = product?.confidence,
            ingredientsText = product?.ingredientsText,
            offLabels = product?.labelsTags.orEmpty(),
            offAdditives = product?.additivesTags.orEmpty(),
            userClaims = product?.userClaims.orEmpty().map { it.name }.sorted(),
            veganConflicts = product?.veganConflicts.orEmpty(),
            vegetarianConflicts = product?.vegetarianConflicts.orEmpty(),
            sourceConflict = product?.sourceConflict == true,
            ingredients = product?.analyzedIngredients.orEmpty().map(::ingredient),
            unknown = product?.unknownIngredients.orEmpty(),
            feedback = entry.feedback?.let {
                ExportFeedback(
                    expected = it.expectedStatus?.name,
                    comment = it.comment,
                    verdictShown = it.shownStatus.name,
                    madhab = it.madhab.name,
                    createdAt = iso(it.createdAt)
                )
            }
        )
    }

    private fun ingredient(a: AnalyzedIngredient) = ExportIngredient(
        text = a.name,
        matchedId = a.ingredient?.id,
        matchedName = a.ingredient?.nameDe,
        status = a.status.name,
        reason = a.reason?.let(::reasonName),
        reasonDetail = when (val reason = a.reason) {
            is Reason.HaramQualifier -> reason.qualifier
            is Reason.AnimalQualifier -> reason.qualifier
            is Reason.HalalSourceQualifier -> reason.qualifier
            else -> null
        },
        inferred = a.inferred
    )

    /** Written out by hand: R8 renames the classes in release builds ("m" instead of "Database"). */
    private fun reasonName(reason: Reason): String = when (reason) {
        is Reason.Database -> "Database"
        is Reason.Inferred -> "Inferred"
        is Reason.NotSpecified -> "NotSpecified"
        is Reason.OffAdditive -> "OffAdditive"
        is Reason.HaramQualifier -> "HaramQualifier"
        is Reason.AnimalQualifier -> "AnimalQualifier"
        is Reason.HalalSourceQualifier -> "HalalSourceQualifier"
        Reason.VeganLabel -> "VeganLabel"
        Reason.HalalLabel -> "HalalLabel"
    }

    private fun iso(millis: Long): String = Instant.ofEpochMilli(millis).toString()
}
