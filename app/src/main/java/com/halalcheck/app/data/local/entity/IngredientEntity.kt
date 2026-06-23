package com.halalcheck.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for ingredients with madhab-specific rulings.
 * Pre-populated from assets/ingredients_db.json on first app launch.
 */
@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name_de")
    val nameDe: String,

    @ColumnInfo(name = "name_en")
    val nameEn: String,

    @ColumnInfo(name = "name_tr")
    val nameTr: String? = null,

    @ColumnInfo(name = "name_ar")
    val nameAr: String? = null,

    @ColumnInfo(name = "default_category")
    val defaultCategory: String, // HALAL, HARAM, MASHBOOH, UNKNOWN

    @ColumnInfo(name = "hanafi_ruling")
    val hanafiRuling: String? = null,

    @ColumnInfo(name = "maliki_ruling")
    val malikiRuling: String? = null,

    @ColumnInfo(name = "shafi_ruling")
    val shafiRuling: String? = null,

    @ColumnInfo(name = "hanbali_ruling")
    val hanbaliRuling: String? = null,

    @ColumnInfo(name = "reason")
    val reason: String? = null,

    @ColumnInfo(name = "reason_de")
    val reasonDe: String? = null,

    @ColumnInfo(name = "source")
    val source: String? = null,

    @ColumnInfo(name = "alternatives")
    val alternatives: String? = null // JSON array stored as string
)
