package com.halalcheck.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.halalcheck.app.data.local.dao.FeedbackDao
import com.halalcheck.app.data.local.dao.IngredientDao
import com.halalcheck.app.data.local.dao.ProductDao
import com.halalcheck.app.data.local.dao.ScanHistoryDao
import com.halalcheck.app.data.local.entity.FeedbackEntity
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
        ScanHistoryEntity::class,
        FeedbackEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class HalalDatabase : RoomDatabase() {

    abstract fun ingredientDao(): IngredientDao
    abstract fun productDao(): ProductDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun feedbackDao(): FeedbackDao

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

        /** v4: diet flags, ruling sources and the composition marker for ingredients (reseeded anyway). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ingredients ADD COLUMN vegan TEXT")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN vegetarian TEXT")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN ruling_references TEXT")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN composition_resolves INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v5: testers' feedback on wrong results. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS feedback (barcode TEXT NOT NULL, expected_status TEXT, " +
                        "comment TEXT NOT NULL, shown_status TEXT NOT NULL, madhab TEXT NOT NULL, " +
                        "created_at INTEGER NOT NULL, PRIMARY KEY(barcode))"
                )
            }
        }

        /** v6: German names on their own, for splitting German compounds (reseeded anyway). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ingredients ADD COLUMN names_de TEXT")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
    }
}
