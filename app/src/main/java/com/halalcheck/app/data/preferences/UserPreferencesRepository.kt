package com.halalcheck.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.PackageClaim
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Owns the app's Preferences DataStore. The UI language is stored by AppCompat
 * (AppCompatDelegate.setApplicationLocales), not here.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val madhab: Flow<Madhab> = context.dataStore.data.map { prefs ->
        prefs[KEY_MADHAB]?.let { Madhab.fromString(it) } ?: Madhab.HANAFI
    }

    /** null = follow the system setting. */
    val darkMode: Flow<Boolean?> = context.dataStore.data.map { it[KEY_DARK_MODE] }

    suspend fun setMadhab(madhab: Madhab) {
        context.dataStore.edit { it[KEY_MADHAB] = madhab.name }
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit {
            if (enabled == null) it.remove(KEY_DARK_MODE) else it[KEY_DARK_MODE] = enabled
        }
    }

    /** What the user confirmed is printed on this product's package (labels Open Food Facts lacks). */
    suspend fun packageClaims(barcode: String): Set<PackageClaim> {
        val prefs = context.dataStore.data.first()
        return PackageClaim.entries.filterTo(mutableSetOf()) { barcode in prefs[claimKey(it)].orEmpty() }
    }

    suspend fun setPackageClaim(barcode: String, claim: PackageClaim, confirmed: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[claimKey(claim)].orEmpty()
            prefs[claimKey(claim)] = if (confirmed) current + barcode else current - barcode
        }
    }

    val packageClaimCount: Flow<Int> = context.dataStore.data.map { prefs ->
        PackageClaim.entries.sumOf { prefs[claimKey(it)].orEmpty().size }
    }

    suspend fun clearPackageClaims() {
        context.dataStore.edit { prefs -> PackageClaim.entries.forEach { prefs.remove(claimKey(it)) } }
    }

    private fun claimKey(claim: PackageClaim) = when (claim) {
        PackageClaim.VEGAN -> KEY_VEGAN_CONFIRMED
        PackageClaim.VEGETARIAN -> KEY_VEGETARIAN_CONFIRMED
        PackageClaim.HALAL -> KEY_HALAL_CONFIRMED
    }

    suspend fun ingredientsDbVersion(): Int? = context.dataStore.data.first()[KEY_INGREDIENTS_DB_VERSION]

    suspend fun setIngredientsDbVersion(version: Int) {
        context.dataStore.edit { it[KEY_INGREDIENTS_DB_VERSION] = version }
    }

    private companion object {
        val KEY_MADHAB = stringPreferencesKey("madhab")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_INGREDIENTS_DB_VERSION = intPreferencesKey("ingredients_db_version")
        val KEY_VEGAN_CONFIRMED = stringSetPreferencesKey("vegan_confirmed_barcodes")
        val KEY_VEGETARIAN_CONFIRMED = stringSetPreferencesKey("vegetarian_confirmed_barcodes")
        val KEY_HALAL_CONFIRMED = stringSetPreferencesKey("halal_confirmed_barcodes")
    }
}
