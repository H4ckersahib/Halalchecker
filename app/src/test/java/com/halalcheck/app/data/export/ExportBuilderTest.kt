package com.halalcheck.app.data.export

import com.halalcheck.app.domain.model.AnalyzedIngredient
import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Ingredient
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.PackageClaim
import com.halalcheck.app.domain.model.Product
import com.halalcheck.app.domain.model.ProductFeedback
import com.halalcheck.app.domain.model.Reason
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportBuilderTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `export contains analysis, feedback and versions in snake_case JSON`() {
        val gelatine = Ingredient(id = "e428", nameDe = "Gelatine", nameEn = "Gelatin", defaultCategory = HalalStatus.MASHBOOH)
        val product = Product(
            barcode = "123",
            name = "Gummibärchen",
            ingredientsText = "Zucker, Gelatine (Schwein)",
            labelsTags = listOf("en:no-gluten"),
            userClaims = setOf(PackageClaim.VEGAN),
            halalStatus = HalalStatus.HARAM,
            confidence = 0.5f,
            source = DataSource.OPEN_FOOD_FACTS,
            analyzedIngredients = listOf(
                AnalyzedIngredient("Gelatine (Schwein)", gelatine, HalalStatus.HARAM, Reason.HaramQualifier("Schwein")),
                AnalyzedIngredient("Zuckerx", null, HalalStatus.UNKNOWN)
            ),
            unknownIngredients = listOf("Zuckerx")
        )
        val export = ExportBuilder.build(
            ExportBuilder.Meta(
                appVersion = "1.0.0-alpha01", appBuild = 7, ingredientsDbVersion = 42,
                exportedAt = 0L, tester = "  ", madhab = Madhab.HANAFI
            ),
            listOf(
                ExportBuilder.Entry(
                    barcode = "123", productName = "Gummibärchen", brand = null, scannedAt = 1000L,
                    verdictShown = HalalStatus.MASHBOOH, product = product,
                    feedback = ProductFeedback("123", HalalStatus.HARAM, "Schwein steht drauf", HalalStatus.MASHBOOH, Madhab.HANAFI, 2000L)
                )
            )
        )
        val json = ExportBuilder.toJson(moshi, export)
        assertTrue(json.contains("\"export_version\": 1"))
        assertTrue(json.contains("\"ingredients_db_version\": 42"))

        val parsed = moshi.adapter(ExportFile::class.java).fromJson(json)!!
        assertNull(parsed.tester) // blank names are dropped
        val scan = parsed.scans.single()
        assertEquals("MASHBOOH", scan.verdictShown)
        assertEquals("HARAM", scan.verdict)
        assertEquals(listOf("VEGAN"), scan.userClaims)
        assertEquals(listOf("Zuckerx"), scan.unknown)
        val ingredient = scan.ingredients.first()
        assertEquals("e428", ingredient.matchedId)
        assertEquals("HaramQualifier", ingredient.reason)
        assertEquals("Schwein", ingredient.reasonDetail)
        assertEquals("HARAM", scan.feedback?.expected)
        assertEquals("1970-01-01T00:00:01Z", scan.scannedAt)
    }

    @Test
    fun `products without cached data are still listed`() {
        val export = ExportBuilder.build(
            ExportBuilder.Meta("1", 1, null, 0L, "Ali", Madhab.SHAFI),
            listOf(ExportBuilder.Entry("999", "Unbekannt", null, null, HalalStatus.UNKNOWN, product = null, feedback = null))
        )
        val scan = export.scans.single()
        assertNull(scan.verdict)
        assertTrue(scan.ingredients.isEmpty())
        assertEquals("Ali", export.tester)
    }
}
