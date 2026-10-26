package com.halalcheck.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Upgrades a v1 database (the first release) to the current version with all migrations. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), HalalDatabase::class.java)

    @Test
    fun upgradeFromV1KeepsDataAndDeduplicatesHistory() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL("INSERT INTO scan_history VALUES (1, '111', 'Nutella', 'Ferrero', NULL, 'MASHBOOH', 1000)")
            execSQL("INSERT INTO scan_history VALUES (2, '222', 'Goldbären', 'Haribo', NULL, 'MASHBOOH', 1500)")
            execSQL("INSERT INTO scan_history VALUES (3, '111', 'Nutella', 'Ferrero', NULL, 'HALAL', 2000)")
            execSQL("INSERT INTO products VALUES ('111', 'Nutella', 'Ferrero', NULL, 'Zucker', 'HALAL', 1.0, 'OPEN_FOOD_FACTS', 1000)")
            execSQL(
                "INSERT INTO ingredients (id, name_de, name_en, default_category) VALUES ('sugar', 'Zucker', 'Sugar', 'HALAL')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 6, true, *HalalDatabase.ALL_MIGRATIONS)

        db.query("SELECT id, halal_status FROM scan_history WHERE barcode = '111'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(3L, cursor.getLong(0)) // the latest scan survives
            assertEquals("HALAL", cursor.getString(1))
        }
        db.query("SELECT COUNT(*) FROM scan_history").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }
        db.query("SELECT name, off_payload FROM products WHERE barcode = '111'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Nutella", cursor.getString(0))
            assertEquals(true, cursor.isNull(1))
        }
        db.query("SELECT is_class, composition_resolves, names_de FROM ingredients WHERE id = 'sugar'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(true, cursor.isNull(2))
        }
        db.query("SELECT COUNT(*) FROM feedback").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
