package com.halalcheck.app.domain.engine

import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.Doubt
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.HalalStatus.HALAL
import com.halalcheck.app.domain.model.HalalStatus.HARAM
import com.halalcheck.app.domain.model.HalalStatus.MASHBOOH
import com.halalcheck.app.domain.model.HalalStatus.UNKNOWN
import com.halalcheck.app.domain.model.Ingredient
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.Product
import com.halalcheck.app.domain.model.Reason
import com.halalcheck.app.domain.model.SourceIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HalalEngineTest {

    private val engine = HalalEngine()

    private val index = IngredientIndex(
        listOf(
            ingredient("water", "Wasser", "Water", HALAL),
            ingredient("sugar", "Zucker", "Sugar", HALAL),
            ingredient("salt", "Salz", "Salt", HALAL),
            ingredient("milk", "Milch", "Milk", HALAL),
            ingredient("cocoa", "Kakao", "Cocoa", HALAL),
            ingredient("almond", "Mandel", "Almond", HALAL, synonyms = listOf("Mandeln")),
            ingredient("berries", "Beeren", "Berries", HALAL),
            ingredient("orange_juice", "Orangensaft", "Orange juice", HALAL),
            ingredient("caffeine", "Koffein", "Caffeine", HALAL),
            ingredient("e330", "Citronensäure", "Citric acid", HALAL, synonyms = listOf("Zitronensäure"), eNumber = "e330"),
            ingredient("e150d", "Ammonsulfit-Zuckerkulör", "Sulphite ammonia caramel", HALAL, synonyms = listOf("E150d"), eNumber = "e150d"),
            ingredient("e322", "Lecithin", "Lecithin", MASHBOOH, synonyms = listOf("Lecithine"), doubt = Doubt.ANIMAL_SOURCE, eNumber = "e322"),
            ingredient(
                "soya_lecithin", "Sojalecithin", "Soy lecithin", HALAL,
                synonyms = listOf("Sojalecithine", "Emulgator Sojalecithin"), eNumber = "e322i"
            ),
            ingredient(
                "e471", "Mono- und Diglyceride von Speisefettsäuren", "Mono- and diglycerides of fatty acids", MASHBOOH,
                synonyms = listOf("E471"), doubt = Doubt.ANIMAL_SOURCE, eNumber = "e471"
            ),
            ingredient("e428", "Gelatine", "Gelatin", MASHBOOH, synonyms = listOf("Speisegelatine"), doubt = Doubt.ANIMAL_SOURCE, eNumber = "e428"),
            ingredient("e120", "Karmin", "Carmine", HARAM, synonyms = listOf("echtes Karmin", "Cochenille"), eNumber = "e120"),
            ingredient("whey", "Molke", "Whey", MASHBOOH, synonyms = listOf("Süßmolkenpulver"), doubt = Doubt.ANIMAL_SOURCE),
            ingredient("vanilla_extract", "Vanilleextrakt", "Vanilla extract", MASHBOOH, doubt = Doubt.ALCOHOL),
            ingredient("flavouring", "Aroma", "Flavouring", MASHBOOH, synonyms = listOf("Aromen"), doubt = Doubt.ANIMAL_SOURCE),
            ingredient("natural_flavouring", "natürliche Aromen", "Natural flavouring", MASHBOOH, doubt = Doubt.ANIMAL_SOURCE),
            ingredient("pork", "Schweinefleisch", "Pork", HARAM, synonyms = listOf("Schwein")),
            ingredient("ham", "Schinken", "Ham", HARAM),
            ingredient("bacon", "Speck", "Bacon", HARAM),
            ingredient("wine", "Wein", "Wine", HARAM),
            ingredient("blood", "Blut", "Blood", HARAM),
            ingredient("shrimp", "Garnelen", "Shrimp", HALAL, hanafi = MASHBOOH),
            ingredient("emulsifier", "Emulgator", "Emulsifier", MASHBOOH, synonyms = listOf("Emulgatoren"), doubt = Doubt.ANIMAL_SOURCE, isClass = true),
            ingredient("colour", "Farbstoff", "Colour", MASHBOOH, synonyms = listOf("Farbstoffe"), doubt = Doubt.ANIMAL_SOURCE, isClass = true),
            ingredient("acid", "Säuerungsmittel", "Acid", UNKNOWN, isClass = true)
        )
    )

    // ── Bugs of the old matcher ────────────────────────────────────────────────

    @Test
    fun `soy lecithin with colon class syntax is halal`() {
        val result = analyze("Zucker, Emulgator: Sojalecithine, Salz")
        assertEquals(HALAL, result.halalStatus)
        assertEquals(listOf("sugar", "soya_lecithin", "salt"), result.ids())
    }

    @Test
    fun `soy lecithin in parentheses is halal`() {
        assertEquals(HALAL, analyze("Emulgator (Sojalecithin)").halalStatus)
        assertEquals(listOf("soya_lecithin"), analyze("Lecithine (Soja)").ids())
    }

    @Test
    fun `lecithin with a plant qualifier is halal`() {
        val entry = analyze("Lecithine (Sonnenblume)").analyzedIngredients.single()
        assertEquals("e322", entry.ingredient?.id)
        assertEquals(HALAL, entry.status)
        assertTrue(entry.reason is Reason.HalalSourceQualifier)
    }

    @Test
    fun `class name followed by an inline E-number or name is dropped`() {
        assertEquals(listOf("e471"), analyze("Emulgator (E471)").ids())
        assertEquals(listOf("e150d"), analyze("Farbstoff E 150d").ids())
        assertEquals(listOf("soya_lecithin"), analyze("Emulgator Lecithine (Soja)").ids())
    }

    @Test
    fun `gelatine qualifiers`() {
        assertEquals(HARAM, analyze("Gelatine (Schwein)").halalStatus)
        assertEquals(HARAM, analyze("Gelatine (Rind, Schwein)").halalStatus)
        val beef = analyze("Gelatine (beef)").analyzedIngredients.single()
        assertEquals(MASHBOOH, beef.status)
        assertTrue(beef.reason is Reason.AnimalQualifier)
    }

    @Test
    fun `compounds that merely start with a haram word are not haram`() {
        val result = analyze("Weinbeeren, Blutorangensaft")
        assertEquals(HALAL, result.halalStatus)
        assertTrue(result.analyzedIngredients.all { it.inferred })
    }

    @Test
    fun `compound with a haram head and another animal is doubtful, not haram`() {
        assertEquals(MASHBOOH, analyze("Putenschinken").halalStatus)
        assertEquals(MASHBOOH, analyze("Rinderspeck").halalStatus)
        assertEquals(HARAM, analyze("Kochschinken").halalStatus)
        assertEquals(HARAM, analyze("Schweinegelatine").halalStatus)
    }

    @Test
    fun `class with its content in parentheses yields the content`() {
        assertEquals(HARAM, analyze("Farbstoff (Karmin)").halalStatus)
        assertEquals(listOf("e330"), analyze("Säuerungsmittel (Citronensäure)").ids())
        assertEquals(listOf("e330"), analyze("Säuerungsmittel: Citronensäure").ids())
    }

    @Test
    fun `class with several additives does not count as unspecified`() {
        val result = analyze("Emulgatoren (Sojalecithine, E471)")
        assertEquals(listOf("soya_lecithin", "e471"), result.ids())
        assertEquals(MASHBOOH, result.halalStatus)
    }

    @Test
    fun `bare class followed by its additive is a typo for a colon`() {
        assertEquals(listOf("water", "e150d", "sugar"), analyze("Wasser, Farbstoff, Zuckerkulör (E150d), Zucker").ids())
    }

    @Test
    fun `class without content is unspecified`() {
        val entry = analyze("Zucker, Emulgator").analyzedIngredients.last()
        assertEquals(MASHBOOH, entry.status)
        assertTrue(entry.reason is Reason.NotSpecified)
    }

    @Test
    fun `percentages, origin and trace sentences are ignored`() {
        val result = analyze("Wasser, MANDELN (2,3%), Zucker. Mandeln aus Spanien. Kann Spuren von Schalenfrüchten enthalten.")
        assertEquals(listOf("water", "almond", "sugar"), result.ids())
        assertEquals(HALAL, result.halalStatus)
    }

    @Test
    fun `every ingredient found in a phrase counts`() {
        val result = analyze("natürliche Aromen inkl. Koffein")
        assertEquals(MASHBOOH, result.halalStatus)
        assertTrue("natural_flavouring" in result.ids())
    }

    // ── Labels ─────────────────────────────────────────────────────────────────

    @Test
    fun `vegan label resolves animal-source doubts only`() {
        val e471 = analyze("Zucker, Emulgator (E471)", labels = listOf("en:vegan"))
        assertEquals(HALAL, e471.halalStatus)
        assertEquals(Reason.VeganLabel, e471.analyzedIngredients.last().reason)

        assertEquals(MASHBOOH, analyze("Zucker, Vanilleextrakt", labels = listOf("en:vegan")).halalStatus)
    }

    @Test
    fun `halal label resolves doubts but never haram`() {
        assertEquals(HALAL, analyze("Zucker, Gelatine", labels = listOf("en:halal")).halalStatus)

        val conflict = analyze("Zucker, Schweinefleisch", labels = listOf("en:halal"))
        assertEquals(HARAM, conflict.halalStatus)
        assertTrue(conflict.labelConflict)
    }

    @Test
    fun `a vegan label the user confirmed on the package counts like one from Open Food Facts`() {
        val text = "Zucker, natürliche Aromen, Emulgator (E471)"
        val unlabeled = analyze(text)
        assertEquals(MASHBOOH, unlabeled.halalStatus)
        assertTrue(unlabeled.veganLabelWouldResolve)

        assertEquals(HALAL, analyze(text, userConfirmedVegan = true).halalStatus)
    }

    @Test
    fun `a vegan label cannot resolve an explicitly animal source`() {
        assertFalse(analyze("Gelatine (Rind)").veganLabelWouldResolve)
        assertEquals(MASHBOOH, analyze("Gelatine (Rind)", userConfirmedVegan = true).halalStatus)
    }

    // ── Annotations ────────────────────────────────────────────────────────────

    @Test
    fun `allergen note in parentheses is not listed as an extra ingredient`() {
        val result = analyze("Zucker, Süßmolkenpulver (aus Milch)")
        assertEquals(listOf("sugar", "whey"), result.ids())
        // "aus Milch" names the allergen, not the rennet the whey's doubt is about
        assertEquals(MASHBOOH, result.halalStatus)
    }

    // ── Aggregation and madhab ─────────────────────────────────────────────────

    @Test
    fun `halal plus unrecognized is not fully checked`() {
        val result = analyze("Zucker, Quinoaflocken")
        assertEquals(UNKNOWN, result.halalStatus)
        assertEquals(listOf("Quinoaflocken"), result.unknownIngredients)
        assertEquals(0.5f, result.confidence)
    }

    @Test
    fun `madhab rulings differ`() {
        assertEquals(MASHBOOH, analyze("Garnelen", madhab = Madhab.HANAFI).halalStatus)
        assertEquals(HALAL, analyze("Garnelen", madhab = Madhab.SHAFI).halalStatus)
    }

    // ── Open Food Facts structured data ────────────────────────────────────────

    @Test
    fun `uses the OFF ingredient ids when they recognize more`() {
        val product = Product(
            barcode = "1",
            name = "Test",
            ingredientsText = "sucre, lécithine de tournesol",
            sourceIngredients = listOf(
                SourceIngredient("en:sugar", "sucre"),
                SourceIngredient("en:soya-lecithin", "lécithine de soja")
            ),
            source = DataSource.OPEN_FOOD_FACTS
        )
        val result = engine.analyzeProduct(product, index, Madhab.HANAFI)
        assertEquals(listOf("sugar", "soya_lecithin"), result.ids())
        assertEquals(HALAL, result.halalStatus)
    }

    @Test
    fun `OFF additives are added only when not already covered`() {
        val covered = analyze("Zucker, Sojalecithin", additives = listOf("en:e322", "en:e322i"))
        assertEquals(listOf("sugar", "soya_lecithin"), covered.ids())

        val hidden = analyze("Zucker", additives = listOf("en:e120"))
        assertEquals(HARAM, hidden.halalStatus)
        assertTrue(hidden.analyzedIngredients.last().reason is Reason.OffAdditive)
    }

    @Test
    fun `nothing to analyze is unknown`() {
        assertEquals(UNKNOWN, analyze("").halalStatus)
        assertFalse(analyze("").labelConflict)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun analyze(
        text: String,
        labels: List<String> = emptyList(),
        additives: List<String> = emptyList(),
        madhab: Madhab = Madhab.HANAFI,
        userConfirmedVegan: Boolean = false
    ): Product = engine.analyzeProduct(
        Product(
            barcode = "1",
            name = "Test",
            ingredientsText = text,
            labelsTags = labels,
            additivesTags = additives,
            source = DataSource.OPEN_FOOD_FACTS,
            userConfirmedVegan = userConfirmedVegan
        ),
        index,
        madhab
    )

    private fun Product.ids() = analyzedIngredients.map { it.ingredient?.id }

    private fun ingredient(
        id: String,
        nameDe: String,
        nameEn: String,
        category: HalalStatus,
        synonyms: List<String> = emptyList(),
        doubt: Doubt? = null,
        hanafi: HalalStatus? = null,
        isClass: Boolean = false,
        eNumber: String? = null
    ) = Ingredient(
        id = id,
        nameDe = nameDe,
        nameEn = nameEn,
        synonyms = synonyms,
        defaultCategory = category,
        hanafiRuling = hanafi,
        doubt = doubt,
        isClass = isClass,
        eNumber = eNumber,
        source = "test"
    )
}
