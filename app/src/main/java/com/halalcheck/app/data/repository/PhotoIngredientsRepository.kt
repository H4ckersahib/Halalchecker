package com.halalcheck.app.data.repository

import com.halalcheck.app.data.local.dao.PhotoIngredientsDao
import com.halalcheck.app.data.local.entity.PhotoIngredientsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** An ingredient list the user photographed for a product, see [com.halalcheck.app.domain.model.withPhotoIngredients]. */
data class PhotoIngredients(val text: String, val savedAt: Long)

/** Ingredient lists from the user's photos; they stay on the device. */
@Singleton
class PhotoIngredientsRepository @Inject constructor(
    private val dao: PhotoIngredientsDao
) {
    suspend fun get(barcode: String): PhotoIngredients? = dao.get(barcode)?.toDomain()

    /** Emits the current text and every change (saved or deleted). */
    fun observe(barcode: String): Flow<PhotoIngredients?> = dao.observe(barcode).map { it?.toDomain() }.distinctUntilChanged()

    suspend fun save(barcode: String, text: String) {
        dao.save(PhotoIngredientsEntity(barcode = barcode, text = text.trim()))
    }

    suspend fun delete(barcode: String) = dao.delete(barcode)
}

private fun PhotoIngredientsEntity.toDomain() = PhotoIngredients(text = text, savedAt = createdAt)
