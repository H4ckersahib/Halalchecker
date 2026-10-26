package com.halalcheck.app.data.local.seed

import com.halalcheck.app.data.local.entity.IngredientEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.BufferedSource

/** One entry of assets/ingredients_db.json (see tools/build_ingredient_db.py). */
@JsonClass(generateAdapter = true)
data class IngredientAssetEntry(
    val id: String,
    @Json(name = "name_de") val nameDe: String,
    @Json(name = "name_en") val nameEn: String,
    @Json(name = "name_tr") val nameTr: String? = null,
    @Json(name = "name_ar") val nameAr: String? = null,
    @Json(name = "name_fr") val nameFr: String? = null,
    val synonyms: List<String>? = null,
    @Json(name = "names_de") val namesDe: List<String>? = null,
    @Json(name = "default_category") val defaultCategory: String,
    @Json(name = "hanafi_ruling") val hanafiRuling: String? = null,
    @Json(name = "maliki_ruling") val malikiRuling: String? = null,
    @Json(name = "shafi_ruling") val shafiRuling: String? = null,
    @Json(name = "hanbali_ruling") val hanbaliRuling: String? = null,
    val reason: String? = null,
    @Json(name = "reason_de") val reasonDe: String? = null,
    val source: String? = null,
    val alternatives: String? = null,
    val doubt: String? = null,
    @Json(name = "inferred_from") val inferredFrom: String? = null,
    @Json(name = "is_class") val isClass: Boolean? = null,
    @Json(name = "e_number") val eNumber: String? = null,
    val vegan: String? = null,
    val vegetarian: String? = null,
    val references: List<AssetReference>? = null,
    @Json(name = "composition_resolves") val compositionResolves: Boolean? = null
) {
    fun toEntity() = IngredientEntity(
        id = id,
        nameDe = nameDe,
        nameEn = nameEn,
        nameTr = nameTr,
        nameAr = nameAr,
        defaultCategory = defaultCategory,
        hanafiRuling = hanafiRuling,
        malikiRuling = malikiRuling,
        shafiRuling = shafiRuling,
        hanbaliRuling = hanbaliRuling,
        reason = reason,
        reasonDe = reasonDe,
        source = source,
        alternatives = alternatives,
        nameFr = nameFr,
        synonyms = synonyms?.joinToString(SYNONYM_SEPARATOR),
        doubt = doubt,
        inferredFrom = inferredFrom,
        isClass = isClass == true,
        eNumber = eNumber,
        vegan = vegan,
        vegetarian = vegetarian,
        rulingReferences = references?.takeIf { it.isNotEmpty() }?.let(::encodeReferences),
        compositionResolves = compositionResolves == true,
        namesDe = namesDe?.joinToString(SYNONYM_SEPARATOR)
    )

    companion object {
        const val SYNONYM_SEPARATOR = "|"
        private const val FIELD_SEPARATOR = '\u001F'
        private const val RECORD_SEPARATOR = '\u001E'

        /** Stored in one Room column: url, title, title_de per record. */
        fun encodeReferences(references: List<AssetReference>): String =
            references.joinToString(RECORD_SEPARATOR.toString()) {
                listOf(it.url, it.title, it.titleDe.orEmpty()).joinToString(FIELD_SEPARATOR.toString())
            }

        fun decodeReferences(encoded: String?): List<AssetReference> =
            encoded.orEmpty().split(RECORD_SEPARATOR).mapNotNull { record ->
                val fields = record.split(FIELD_SEPARATOR)
                if (fields.size < 2 || fields[0].isBlank()) null
                else AssetReference(url = fields[0], title = fields[1], titleDe = fields.getOrNull(2)?.ifBlank { null })
            }
    }
}

/** A published source for a ruling, e.g. a fatwa page. */
@JsonClass(generateAdapter = true)
data class AssetReference(
    val url: String,
    val title: String,
    @Json(name = "title_de") val titleDe: String? = null
)

class IngredientAsset(val version: Int, val ingredients: List<IngredientAssetEntry>)

/**
 * Streams the asset. The generator writes "version" first, so an unchanged asset is detected
 * without parsing the ingredient list.
 */
class IngredientAssetParser(moshi: Moshi) {
    private val listAdapter = moshi.adapter<List<IngredientAssetEntry>>(
        Types.newParameterizedType(List::class.java, IngredientAssetEntry::class.java)
    )

    /** Returns null if the asset's version equals [skipIfVersion]. */
    fun parse(source: BufferedSource, skipIfVersion: Int? = null): IngredientAsset? {
        JsonReader.of(source).use { reader ->
            var version: Int? = null
            var ingredients: List<IngredientAssetEntry>? = null
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "version" -> {
                        version = reader.nextInt()
                        if (version == skipIfVersion) return null
                    }
                    "ingredients" -> ingredients = listAdapter.fromJson(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return IngredientAsset(
                version = checkNotNull(version) { "ingredients_db.json has no version" },
                ingredients = ingredients.orEmpty()
            )
        }
    }
}
