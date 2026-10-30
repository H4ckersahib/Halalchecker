package com.halalcheck.app.data.export

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The test data export (JSON). Bump [ExportFile.exportVersion] when fields change in a way
 * tools/analyze_feedback.py has to know about.
 */
@JsonClass(generateAdapter = true)
data class ExportFile(
    @Json(name = "export_version") val exportVersion: Int = EXPORT_VERSION,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "app_build") val appBuild: Int,
    @Json(name = "ingredients_db_version") val ingredientsDbVersion: Int?,
    @Json(name = "exported_at") val exportedAt: String,
    val tester: String?,
    /** The madhab the "verdict" fields were computed with (the tester's default). */
    val madhab: String,
    val scans: List<ExportScan>
) {
    companion object {
        /** 2: ingredients_language, off_language, ignored_text. 3: ingredients_from_photo. */
        const val EXPORT_VERSION = 3
    }
}

@JsonClass(generateAdapter = true)
data class ExportScan(
    val barcode: String,
    @Json(name = "product_name") val productName: String?,
    val brand: String?,
    @Json(name = "scanned_at") val scannedAt: String?,
    /** What the history showed (the last result the tester saw). */
    @Json(name = "verdict_shown") val verdictShown: String?,
    /** The result of this app version at export time; null if the product data is not cached. */
    val verdict: String?,
    @Json(name = "ingredients_recognized") val ingredientsRecognized: Float?,
    @Json(name = "ingredients_text") val ingredientsText: String?,
    /** Language of ingredients_text; off_language is the one Open Food Facts parsed its ingredients in. */
    @Json(name = "ingredients_language") val ingredientsLanguage: String?,
    @Json(name = "off_language") val offLanguage: String?,
    /** ingredients_text is the user's photo of the list, not Open Food Facts' data. */
    @Json(name = "ingredients_from_photo") val ingredientsFromPhoto: Boolean,
    @Json(name = "off_labels") val offLabels: List<String>,
    @Json(name = "off_additives") val offAdditives: List<String>,
    @Json(name = "user_claims") val userClaims: List<String>,
    @Json(name = "vegan_conflicts") val veganConflicts: List<String>,
    @Json(name = "vegetarian_conflicts") val vegetarianConflicts: List<String>,
    @Json(name = "source_conflict") val sourceConflict: Boolean,
    val ingredients: List<ExportIngredient>,
    val unknown: List<String>,
    /** Unrecognized label text that was not counted as an ingredient. */
    @Json(name = "ignored_text") val ignoredText: List<String>,
    val feedback: ExportFeedback?
)

@JsonClass(generateAdapter = true)
data class ExportIngredient(
    val text: String,
    @Json(name = "matched_id") val matchedId: String?,
    @Json(name = "matched_name") val matchedName: String?,
    val status: String,
    /** Why it got this status, e.g. "Database", "VeganLabel", "HaramQualifier". */
    val reason: String?,
    /** The qualifier word for qualifier reasons, e.g. "Rind". */
    @Json(name = "reason_detail") val reasonDetail: String?,
    /** Matched only via a compound word ("Putenschinken" -> "Schinken"). */
    val inferred: Boolean
)

@JsonClass(generateAdapter = true)
data class ExportFeedback(
    /** HALAL, HARAM, MASHBOOH, or null for "don't know". */
    val expected: String?,
    val comment: String,
    @Json(name = "verdict_shown") val verdictShown: String,
    val madhab: String,
    @Json(name = "created_at") val createdAt: String
)
