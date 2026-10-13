package com.halalcheck.app.data.local.seed

import android.content.Context
import com.halalcheck.app.data.local.dao.IngredientDao
import com.halalcheck.app.data.preferences.UserPreferencesRepository
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Replaces the ingredients table whenever the bundled asset's version differs from the seeded one,
 * so updates to ingredients_db.json reach existing installs. Callers await it before reading.
 */
@Singleton
class IngredientSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ingredientDao: IngredientDao,
    private val preferences: UserPreferencesRepository,
    moshi: Moshi
) {
    private val parser = IngredientAssetParser(moshi)
    private val mutex = Mutex()

    @Volatile
    private var seeded = false

    suspend fun ensureSeeded() {
        if (seeded) return
        mutex.withLock {
            if (seeded) return
            withContext(Dispatchers.IO) {
                val seededVersion = preferences.ingredientsDbVersion()
                    ?.takeIf { ingredientDao.getIngredientCount() > 0 }
                val asset = context.assets.open(ASSET_NAME).source().buffer().use {
                    parser.parse(it, skipIfVersion = seededVersion)
                }
                if (asset != null) {
                    ingredientDao.replaceAll(asset.ingredients.map { it.toEntity() })
                    preferences.setIngredientsDbVersion(asset.version)
                }
            }
            seeded = true
        }
    }

    companion object {
        const val ASSET_NAME = "ingredients_db.json"
    }
}
