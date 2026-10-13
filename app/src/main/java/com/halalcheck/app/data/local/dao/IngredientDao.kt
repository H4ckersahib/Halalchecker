package com.halalcheck.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.halalcheck.app.data.local.entity.IngredientEntity

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients")
    suspend fun getAllIngredients(): List<IngredientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun getIngredientCount(): Int

    @Query("DELETE FROM ingredients")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(ingredients: List<IngredientEntity>) {
        deleteAll()
        insertIngredients(ingredients)
    }
}
