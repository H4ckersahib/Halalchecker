package com.halalcheck.app.data.repository

import com.halalcheck.app.data.local.dao.IngredientDao
import com.halalcheck.app.data.local.entity.IngredientEntity
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Ingredient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientRepository @Inject constructor(
    private val ingredientDao: IngredientDao
) {
    /**
     * Get all ingredients from the local database.
     */
    suspend fun getAllIngredients(): List<Ingredient> {
        return ingredientDao.getAllIngredients().map { it.toDomain() }
    }

    /**
     * Observe all ingredients as a Flow.
     */
    fun getAllIngredientsFlow(): Flow<List<Ingredient>> {
        return ingredientDao.getAllIngredientsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Search ingredients by name or ID.
     */
    suspend fun searchIngredients(query: String): List<Ingredient> {
        return ingredientDao.searchIngredients(query).map { it.toDomain() }
    }

    /**
     * Get the total number of ingredients in the database.
     */
    suspend fun getIngredientCount(): Int {
        return ingredientDao.getIngredientCount()
    }
}

/**
 * Extension function to convert IngredientEntity to domain model.
 */
fun IngredientEntity.toDomain(): Ingredient {
    return Ingredient(
        id = id,
        nameDe = nameDe,
        nameEn = nameEn,
        nameTr = nameTr,
        nameAr = nameAr,
        defaultCategory = HalalStatus.fromString(defaultCategory),
        hanafiRuling = hanafiRuling?.let { HalalStatus.fromString(it) },
        malikiRuling = malikiRuling?.let { HalalStatus.fromString(it) },
        shafiRuling = shafiRuling?.let { HalalStatus.fromString(it) },
        hanbaliRuling = hanbaliRuling?.let { HalalStatus.fromString(it) },
        reason = reason,
        reasonDe = reasonDe,
        source = source,
        alternatives = alternatives?.split(",")?.map { it.trim() } ?: emptyList()
    )
}
