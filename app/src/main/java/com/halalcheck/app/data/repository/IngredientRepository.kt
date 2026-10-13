package com.halalcheck.app.data.repository

import com.halalcheck.app.data.local.dao.IngredientDao
import com.halalcheck.app.data.local.entity.IngredientEntity
import com.halalcheck.app.data.local.seed.IngredientAssetEntry
import com.halalcheck.app.data.local.seed.IngredientSeeder
import com.halalcheck.app.domain.engine.IngredientIndex
import com.halalcheck.app.domain.model.Doubt
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Ingredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientRepository @Inject constructor(
    private val ingredientDao: IngredientDao,
    private val seeder: IngredientSeeder
) {
    private val indexMutex = Mutex()

    @Volatile
    private var index: IngredientIndex? = null

    /**
     * The lookup index over all ingredients. Built once per process, after seeding.
     */
    suspend fun getIndex(): IngredientIndex {
        index?.let { return it }
        return indexMutex.withLock {
            index ?: run {
                seeder.ensureSeeded()
                val ingredients = ingredientDao.getAllIngredients()
                withContext(Dispatchers.Default) { IngredientIndex(ingredients.map { it.toDomain() }) }
            }.also { index = it }
        }
    }

    /**
     * Get the total number of ingredients in the database.
     */
    suspend fun getIngredientCount(): Int {
        seeder.ensureSeeded()
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
        nameFr = nameFr,
        synonyms = synonyms?.split(IngredientAssetEntry.SYNONYM_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList(),
        defaultCategory = HalalStatus.fromString(defaultCategory),
        hanafiRuling = hanafiRuling?.let { HalalStatus.fromString(it) },
        malikiRuling = malikiRuling?.let { HalalStatus.fromString(it) },
        shafiRuling = shafiRuling?.let { HalalStatus.fromString(it) },
        hanbaliRuling = hanbaliRuling?.let { HalalStatus.fromString(it) },
        reason = reason,
        reasonDe = reasonDe,
        source = source,
        alternatives = alternatives?.split(",")?.map { it.trim() } ?: emptyList(),
        doubt = Doubt.fromString(doubt),
        inferredFrom = inferredFrom,
        isClass = isClass,
        eNumber = eNumber
    )
}
