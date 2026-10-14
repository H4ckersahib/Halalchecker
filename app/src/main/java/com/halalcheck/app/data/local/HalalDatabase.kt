package com.halalcheck.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.halalcheck.app.data.local.dao.IngredientDao
import com.halalcheck.app.data.local.dao.ProductDao
import com.halalcheck.app.data.local.dao.ScanHistoryDao
import com.halalcheck.app.data.local.entity.IngredientEntity
import com.halalcheck.app.data.local.entity.ProductEntity
import com.halalcheck.app.data.local.entity.ScanHistoryEntity

/**
 * The ingredients table is filled by IngredientSeeder, not by a creation callback.
 */
@Database(
    entities = [
        IngredientEntity::class,
        ProductEntity::class,
        ScanHistoryEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class HalalDatabase : RoomDatabase() {

    abstract fun ingredientDao(): IngredientDao
    abstract fun productDao(): ProductDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        const val DATABASE_NAME = "halalcheck_db"

        /** v2: richer ingredient data and the cached OFF payload. Keeps the scan history. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ingredients ADD COLUMN name_fr TEXT")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN synonyms TEXT")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN doubt TEXT")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN inferred_from TEXT")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN is_class INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN e_number TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN off_payload TEXT")
            }
        }

        /** v3: one history row per barcode. Keeps the latest scan of each product. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM scan_history WHERE id NOT IN (SELECT MAX(id) FROM scan_history GROUP BY barcode)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_scan_history_barcode ON scan_history (barcode)")
            }
        }
    }
}
