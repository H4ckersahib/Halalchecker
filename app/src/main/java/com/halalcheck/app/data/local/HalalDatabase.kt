package com.halalcheck.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.halalcheck.app.data.local.dao.IngredientDao
import com.halalcheck.app.data.local.dao.ProductDao
import com.halalcheck.app.data.local.dao.ScanHistoryDao
import com.halalcheck.app.data.local.entity.IngredientEntity
import com.halalcheck.app.data.local.entity.ProductEntity
import com.halalcheck.app.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

@Database(
    entities = [
        IngredientEntity::class,
        ProductEntity::class,
        ScanHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class HalalDatabase : RoomDatabase() {

    abstract fun ingredientDao(): IngredientDao
    abstract fun productDao(): ProductDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        const val DATABASE_NAME = "halalcheck_db"

        /**
         * Create the Room database callback that pre-populates the ingredients
         * from the assets/ingredients_db.json file on first creation.
         */
        fun createCallback(context: Context): Callback {
            return object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        prepopulateIngredients(context, db)
                    }
                }


            }
        }

        /**
         * Read ingredients from the JSON asset and insert them into the database.
         */
        private fun prepopulateIngredients(context: Context, db: SupportSQLiteDatabase) {
            try {
                val jsonString = context.assets
                    .open("ingredients_db.json")
                    .bufferedReader()
                    .use { it.readText() }

                val jsonArray = JSONArray(jsonString)

                db.beginTransaction()
                try {
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        db.execSQL(
                            """INSERT OR REPLACE INTO ingredients 
                            (id, name_de, name_en, name_tr, name_ar, default_category, 
                             hanafi_ruling, maliki_ruling, shafi_ruling, hanbali_ruling,
                             reason, reason_de, source, alternatives) 
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                            arrayOf(
                                obj.getString("id"),
                                obj.getString("name_de"),
                                obj.getString("name_en"),
                                obj.optString("name_tr", null),
                                obj.optString("name_ar", null),
                                obj.getString("default_category"),
                                obj.optString("hanafi_ruling", null),
                                obj.optString("maliki_ruling", null),
                                obj.optString("shafi_ruling", null),
                                obj.optString("hanbali_ruling", null),
                                obj.optString("reason", null),
                                obj.optString("reason_de", null),
                                obj.optString("source", null),
                                obj.optString("alternatives", null)
                            )
                        )
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
