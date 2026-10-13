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
    @Json(name = "e_number") val eNumber: String? = null
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
        eNumber = eNumber
    )

    companion object {
        const val SYNONYM_SEPARATOR = "|"
    }
}

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
