package com.halalcheck.app.domain.engine

import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.DietFlag
import com.halalcheck.app.domain.model.Doubt
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.HalalStatus.HALAL
import com.halalcheck.app.domain.model.HalalStatus.HARAM
import com.halalcheck.app.domain.model.HalalStatus.MASHBOOH
import com.halalcheck.app.domain.model.HalalStatus.UNKNOWN
import com.halalcheck.app.domain.model.Ingredient
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.PackageClaim
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
            ingredient("milk", "Milch", "Milk", HALAL, vegan = DietFlag.NO, vegetarian = DietFlag.YES),
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
                synonyms = listOf("E471"), doubt = Doubt.ANIMAL_SOURCE, eNumber = "e471",
                vegan = DietFlag.MAYBE, vegetarian = DietFlag.MAYBE
            ),
            ingredient(
                "e428", "Gelatine", "Gelatin", MASHBOOH, synonyms = listOf("Speisegelatine"), doubt = Doubt.ANIMAL_SOURCE,
                eNumber = "e428", vegan = DietFlag.NO, vegetarian = DietFlag.NO
            ),
            ingredient("e120", "Karmin", "Carmine", HARAM, synonyms = listOf("echtes Karmin", "Cochenille"), eNumber = "e120"),
            ingredient("whey", "Molke", "Whey", MASHBOOH, synonyms = listOf("Süßmolkenpulver"), doubt = Doubt.ANIMAL_SOURCE),
            ingredient("vanilla_extract", "Vanilleextrakt", "Vanilla extract", MASHBOOH, doubt = Doubt.ALCOHOL),
            ingredient("flavouring", "Aroma", "Flavouring", MASHBOOH, synonyms = listOf("Aromen"), doubt = Doubt.ANIMAL_SOURCE),
            ingredient("natural_flavouring", "natürliche Aromen", "Natural flavouring", MASHBOOH, doubt = Doubt.ANIMAL_SOURCE),
            ingredient("pork", "Schweinefleisch", "Pork", HARAM, synonyms = listOf("Schwein"), vegan = DietFlag.NO, vegetarian = DietFlag.NO),
            ingredient("soy_sauce", "Sojasoße", "Soy sauce", MASHBOOH, doubt = Doubt.ALCOHOL),
            ingredient("sausage", "Wurst", "Sausage", MASHBOOH, doubt = Doubt.OTHER, inferredFrom = "meat"),
            ingredient("filling", "Füllung", "Filling", MASHBOOH, doubt = Doubt.ANIMAL_SOURCE, source = "off_maybe"),
            ingredient("milk_chocolate", "Milchschokolade", "Milk chocolate", MASHBOOH, doubt = Doubt.OTHER, compositionResolves = true),
            ingredient("ham", "Schinken", "Ham", HARAM),
            ingredient("bacon", "Speck", "Bacon", HARAM),
            ingredient("wine", "Wein", "Wine", HARAM),
            ingredient("beer", "Bier", "Beer", HARAM),
            ingredient("rum", "Rum", "Rum", HARAM),
            ingredient("strawberry", "Erdbeere", "Strawberry", HALAL, synonyms = listOf("Erdbeeren")),
            ingredient("blueberry", "Heidelbeere", "Blueberry", HALAL),
            ingredient("raisin", "Rosine", "Raisin", HALAL, synonyms = listOf("Rosinen")),
            ingredient("yogurt", "Joghurt", "Yogurt", HALAL),
            ingredient("juice", "Saft", "Juice", MASHBOOH, doubt = Doubt.ANIMAL_SOURCE, source = "off_maybe"),
            // An English-only entry: its name_de is the English fallback
            ingredient("smoothie", "Smoothie", "Smoothie", HALAL, german = emptyList()),
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
    fun `German compounds are split with German names only`() {
        // "Erdbeer" ends in the English "beer"
        listOf("Erdbeer-Joghurt", "Erdbeerjoghurt", "Erdbeer", "Erdbeer-Smoothie").forEach { text ->
            assertEquals(text, HALAL, analyze(text).halalStatus)
        }
        assertEquals(listOf("yogurt"), analyze("Erdbeer-Joghurt").ids())
        assertFalse(analyze("Erdbeer-Joghurt").analyzedIngredients.single().inferred) // the hyphen names the head
        assertEquals(listOf("strawberry"), analyze("Erdbeer").ids()) // combining form of "Erdbeere"
    }

    @Test
    fun `a form word after a hyphen is the base product`() {
        val entry = analyze("Heidelbeer-Saft").analyzedIngredients.single()
        assertEquals("blueberry", entry.ingredient?.id)
        assertEquals(HALAL, entry.status)
        assertTrue(entry.inferred)
    }

    @Test
    fun `a haram modifier of a compound counts`() {
        assertEquals(HARAM, analyze("Rum-Rosinen").halalStatus)
        assertEquals(HARAM, analyze("Rumrosinen").halalStatus)
        assertEquals(HARAM, analyze("Erdbeer-Bier").halalStatus)
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
    fun `bare class followed by something else stays unspecified (REG-14)`() {
        val result = analyze("Farbstoff, Zucker")
        assertEquals(listOf("colour", "sugar"), result.ids())
        assertEquals(MASHBOOH, result.halalStatus)
    }

    @Test
    fun `an E-number does not hide the rest of the phrase (REG-04)`() {
        val result = analyze("Schweinefleisch mit E471")
        assertEquals(HARAM, result.halalStatus)
        assertEquals("pork", result.analyzedIngredients.single().ingredient?.id)
    }

    @Test
    fun `a parent whose doubt the composition can't answer is kept`() {
        assertEquals(MASHBOOH, analyze("Wurst (Salz, Zucker)").halalStatus)
        assertEquals(MASHBOOH, analyze("Aroma (Wasser, Zucker)").halalStatus)
    }

    @Test
    fun `a parent whose doubt is only about its contents is answered by the composition`() {
        assertEquals(HALAL, analyze("Füllung (Zucker, Salz)").halalStatus)
        assertEquals(HALAL, analyze("Milchschokolade (Zucker, Kakao, Milch)").halalStatus)
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
    fun `negative labels never count as positive (REG-01, REG-02)`() {
        val nonHalal = analyze("Zucker, Gelatine", labels = listOf("en:non-halal"))
        assertFalse(nonHalal.offHalal)
        assertEquals(MASHBOOH, nonHalal.halalStatus)
        assertEquals(MASHBOOH, analyze("Zucker, Emulgator (E471)", labels = listOf("en:non-vegan")).halalStatus)
        assertEquals(MASHBOOH, analyze("Zucker, Emulgator (E471)", labels = listOf("en:non-vegetarian")).halalStatus)
    }

    @Test
    fun `a halal label from Open Food Facts alone is only a hint (REG-03)`() {
        val result = analyze("Zucker, Gelatine", labels = listOf("en:halal"))
        assertTrue(result.offHalal)
        assertEquals(MASHBOOH, result.halalStatus)
    }

    @Test
    fun `a confirmed halal label resolves doubts but not unknown or haram ingredients`() {
        assertEquals(HALAL, analyze("Zucker, Gelatine", claims = setOf(PackageClaim.HALAL)).halalStatus)
        assertEquals(UNKNOWN, analyze("Zucker, Quinoaflocken", claims = setOf(PackageClaim.HALAL)).halalStatus)
        assertEquals(UNKNOWN, analyze("", claims = setOf(PackageClaim.HALAL)).halalStatus)

        val conflict = analyze("Zucker, Schweinefleisch", claims = setOf(PackageClaim.HALAL))
        assertEquals(HARAM, conflict.halalStatus)
        assertTrue(conflict.labelConflict)
        assertTrue(analyze("Zucker, Schweinefleisch", labels = listOf("en:halal")).labelConflict)
    }

    @Test
    fun `a vegan label the user confirmed on the package counts like one from Open Food Facts`() {
        val text = "Zucker, natürliche Aromen, Emulgator (E471)"
        val unlabeled = analyze(text)
        assertEquals(MASHBOOH, unlabeled.halalStatus)
        assertTrue(unlabeled.dietLabelWouldResolve)

        assertEquals(HALAL, analyze(text, claims = setOf(PackageClaim.VEGAN)).halalStatus)
        assertEquals(HALAL, analyze(text, claims = setOf(PackageClaim.VEGETARIAN)).halalStatus)
    }

    @Test
    fun `vegetarian is not vegan (REG-08)`() {
        val text = "Zucker, Milch, Emulgator (E471)"
        val vegetarian = analyze(text, claims = setOf(PackageClaim.VEGETARIAN))
        assertEquals(HALAL, vegetarian.halalStatus)
        assertTrue(vegetarian.veganConflicts.isEmpty())
    }

    @Test
    fun `a diet claim the ingredients contradict is not used (REG-09)`() {
        val vegan = analyze("Zucker, Milch, Emulgator (E471)", claims = setOf(PackageClaim.VEGAN))
        assertEquals(listOf("Milch"), vegan.veganConflicts)
        assertEquals(MASHBOOH, vegan.halalStatus)

        val offVegan = analyze("Zucker, Milch, Emulgator (E471)", labels = listOf("en:vegan"))
        assertEquals(MASHBOOH, offVegan.halalStatus)

        val vegetarian = analyze("Zucker, Gelatine", claims = setOf(PackageClaim.VEGETARIAN))
        assertEquals(listOf("Gelatine"), vegetarian.vegetarianConflicts)
        assertEquals(MASHBOOH, vegetarian.halalStatus)
    }

    @Test
    fun `a wrong vegan claim does not cancel an independent vegetarian label`() {
        val result = analyze(
            "Zucker, Milch, Emulgator (E471)",
            labels = listOf("en:vegetarian"),
            claims = setOf(PackageClaim.VEGAN)
        )
        assertEquals(listOf("Milch"), result.veganConflicts)
        assertEquals(HALAL, result.halalStatus)
    }

    @Test
    fun `a vegan label cannot resolve an explicitly animal source`() {
        assertFalse(analyze("Gelatine (Rind)").dietLabelWouldResolve)
        assertEquals(MASHBOOH, analyze("Gelatine (Rind)", claims = setOf(PackageClaim.VEGAN)).halalStatus)
    }

    @Test
    fun `a plant source answers animal-source doubts only`() {
        assertEquals(MASHBOOH, analyze("Sojasoße (Soja)").halalStatus)
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
    fun `haram found in the text survives when the OFF parse wins (REG-05)`() {
        val result = analyze(
            "Schweinefleisch, Quinoaflocken",
            sourceIngredients = listOf(SourceIngredient("en:sugar", "Zucker"))
        )
        assertEquals(HARAM, result.halalStatus)
        assertTrue(result.sourceConflict)
        assertTrue("pork" in result.ids())
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
    fun `a haram word in the product name is a hint, not a verdict`() {
        fun named(name: String, text: String) = engine.analyzeProduct(
            Product(barcode = "1", name = name, ingredientsText = text, source = DataSource.OPEN_FOOD_FACTS),
            index,
            Madhab.HANAFI
        )
        val noList = named("Rum Trüffel", "")
        assertEquals(UNKNOWN, noList.halalStatus)
        assertEquals(listOf("rum"), noList.nameMentions.map { it.id })
        assertEquals(listOf("rum"), named("Rum Trüffel", "Zucker, Kakao").nameMentions.map { it.id })
        assertTrue(named("Rum Trüffel", "Zucker, Rum").nameMentions.isEmpty()) // already HARAM
        assertTrue(named("Glücksschwein", "Zucker").nameMentions.isEmpty()) // marzipan, only a compound guess
    }

    @Test
    fun `text around the list on a photographed label is ignored`() {
        val result = analyze(
            "MARS GMBH, POSTFACH 1414. www.mars.de ZUCKERFREIER KAUGUMMI. ZUTATEN: Zucker, Salz. " +
                "KÜHL UND TROCKEN LAGERN. Nährwerte: Brennwert 731 kJ/175 kcal, mehrwertige Alkohole 71 g"
        )
        assertEquals(listOf("sugar", "salt"), result.ids())
    }

    @Test
    fun `html entities and quoted names are removed`() {
        assertEquals(listOf("water", "sugar", "salt"), analyze("Wasser &quot;Quelle Nord&quot;, Zucker, Salz").ids())
        assertEquals(listOf("water", "cocoa"), analyze("Mineralwasser „Quelle Nord“, Kakao&#44; Wasser").ids().distinct())
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
        claims: Set<PackageClaim> = emptySet(),
        sourceIngredients: List<SourceIngredient> = emptyList()
    ): Product = engine.analyzeProduct(
        Product(
            barcode = "1",
            name = "Test",
            ingredientsText = text,
            labelsTags = labels,
            additivesTags = additives,
            sourceIngredients = sourceIngredients,
            source = DataSource.OPEN_FOOD_FACTS,
            userClaims = claims
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
        eNumber: String? = null,
        vegan: DietFlag? = null,
        vegetarian: DietFlag? = null,
        inferredFrom: String? = null,
        source: String = "test",
        compositionResolves: Boolean = false,
        german: List<String> = listOf(nameDe) + synonyms
    ) = Ingredient(
        id = id,
        nameDe = nameDe,
        nameEn = nameEn,
        synonyms = synonyms,
        germanNames = german,
        defaultCategory = category,
        hanafiRuling = hanafi,
        doubt = doubt,
        isClass = isClass,
        eNumber = eNumber,
        vegan = vegan,
        vegetarian = vegetarian,
        inferredFrom = inferredFrom,
        source = source,
        compositionResolves = compositionResolves
    )
}
