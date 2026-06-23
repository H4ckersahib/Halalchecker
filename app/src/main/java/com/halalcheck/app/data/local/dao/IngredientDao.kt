package com.halalcheck.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halalcheck.app.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients")
    suspend fun getAllIngredients(): List<IngredientEntity>

    @Query("SELECT * FROM ingredients")
    fun getAllIngredientsFlow(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getIngredientById(id: String): IngredientEntity?

    @Query("""
        SELECT * FROM ingredients 
        WHERE name_de LIKE '%' || :query || '%' 
        OR name_en LIKE '%' || :query || '%' 
        OR name_tr LIKE '%' || :query || '%'
        OR name_ar LIKE '%' || :query || '%'
        OR id LIKE '%' || :query || '%'
    """)
    suspend fun searchIngredients(query: String): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE default_category = :category")
    suspend fun getIngredientsByCategory(category: String): List<IngredientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: IngredientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun getIngredientCount(): Int

    @Query("DELETE FROM ingredients")
    suspend fun deleteAll()
}
