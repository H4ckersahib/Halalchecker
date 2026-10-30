package com.halalcheck.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halalcheck.app.data.local.entity.PhotoIngredientsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoIngredientsDao {

    @Query("SELECT * FROM photo_ingredients WHERE barcode = :barcode")
    suspend fun get(barcode: String): PhotoIngredientsEntity?

    @Query("SELECT * FROM photo_ingredients WHERE barcode = :barcode")
    fun observe(barcode: String): Flow<PhotoIngredientsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: PhotoIngredientsEntity)

    @Query("DELETE FROM photo_ingredients WHERE barcode = :barcode")
    suspend fun delete(barcode: String)
}
