package com.halalcheck.app.data.local.seed

import com.halalcheck.app.data.repository.toDomain
import com.halalcheck.app.domain.engine.HalalEngine
import com.halalcheck.app.domain.engine.IngredientIndex
import com.halalcheck.app.domain.model.DataSource
import com.halalcheck.app.domain.model.Doubt
import com.halalcheck.app.domain.model.KnownLabels
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.PackageClaim
import com.halalcheck.app.domain.model.Product
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Checks the generated assets/ingredients_db.json and runs real German labels from Open Food Facts
 * through the engine with it. Regenerate the asset with tools/build_ingredient_db.py.
 */
class IngredientAssetTest {

    companion object {
        private lateinit var asset: IngredientAsset
        private lateinit var index: IngredientIndex

        @BeforeClass
        @JvmStatic
        fun loadAsset() {
            val file = File("src/main/assets/ingredients_db.json")
            asset = file.source().buffer().use { IngredientAssetParser(Moshi.Builder().build()).parse(it) }!!
            index = IngredientIndex(asset.ingredients.map { it.toEntity().toDomain() })
        }
    }

    private val engine = HalalEngine()

    @Test
    fun `asset is well formed`() {
        assertTrue("expected a large database, got ${asset.ingredients.size}", asset.ingredients.size > 5000)
        assertEquals(asset.ingredients.size, asset.ingredients.map { it.id }.toSet().size)
        val categories = setOf("HALAL", "HARAM", "MASHBOOH", "UNKNOWN")
        asset.ingredients.forEach { entry ->
            assertTrue("${entry.id}: ${entry.defaultCategory}", entry.defaultCategory in categories)
            if (entry.defaultCategory == "MASHBOOH") assertNotNull("${entry.id} needs a doubt", Doubt.fromString(entry.doubt))
        }
    }

    @Test
    fun `parser skips an unchanged asset`() {
        val skipped = File("src/main/assets/ingredients_db.json").source().buffer().use {
            IngredientAssetParser(Moshi.Builder().build()).parse(it, skipIfVersion = asset.version)
        }
        assertNull(skipped)
    }

    @Test
    fun `key ingredients have the expected rulings`() {
        mapOf(
            "e428" to HalalStatus.MASHBOOH, // gelatine
            "pork_gelatin" to HalalStatus.HARAM,
            "soya_lecithin" to HalalStatus.HALAL,
            "e471" to HalalStatus.MASHBOOH,
            "e120" to HalalStatus.HARAM,
            "white_wine" to HalalStatus.HARAM,
            "spirit_vinegar" to HalalStatus.HALAL,
            "raisin" to HalalStatus.HALAL,
            "hazelnut" to HalalStatus.HALAL,
            "e150d" to HalalStatus.HALAL,
            "e375" to HalalStatus.HALAL, // niacin, synthetic
            "taurine" to HalalStatus.HALAL // synthetic
        ).forEach { (id, expected) ->
            assertEquals(id, expected, index.byId(id)?.defaultCategory)
        }
    }

    @Test
    fun `Nutella is halal`() {
        val result = analyze(
            "Zucker, Palmöl, HASELNÜSSE 13 %, MAGERMILCHPULVER 8,7 %, fettarmer Kakao, Emulgator Lecithine (Soja), Vanillin",
            additives = listOf("en:e322", "en:e322i")
        )
        assertEquals(result.describe(), HalalStatus.HALAL, result.halalStatus)
    }

    @Test
    fun `Haribo is doubtful because of gelatine only`() {
        val result = analyze(
            "Glukosesirup, Zucker, Gelatine, Dextrose, Fruchtsaft aus Fruchtsaftkonzentrat: Apfel, Erdbeere, Himbeere, " +
                "Orange, Zitrone, Ananas, Säuerungsmittel: Citronensäure, Frucht - und Pflanzenkonzentrate: Saflor, " +
                "Spirulina, Apfel, Holunderbeere, Orange, Schwarze Johannisbeere, Kiwi, Zitrone, Aronia, Mango, " +
                "Passionsfrucht, Traube, Aroma, Holunderbeerextrakt, Überzugsmittel: Bienenwachs weiß und gelb, Carnaubawachs"
        )
        assertEquals(result.describe(), HalalStatus.MASHBOOH, result.halalStatus)
        val doubtful = result.analyzedIngredients.filter { it.status == HalalStatus.MASHBOOH }.map { it.ingredient?.id }.toSet()
        assertEquals(result.describe(), setOf("e428", "flavouring"), doubtful)
        assertTrue(result.describe(), result.analyzedIngredients.none { it.status == HalalStatus.HARAM })
    }

    @Test
    fun `Coca Cola is doubtful only because of the natural flavourings`() {
        val result = analyze(
            "Wasser, Zucker, Kohlensäure, Farbstoff E150d, Säuerungsmittel: E338, natürliche Aromen inklusive Koffein",
            additives = listOf("en:e150d", "en:e338")
        )
        assertEquals(result.describe(), HalalStatus.MASHBOOH, result.halalStatus)
        val doubtful = result.analyzedIngredients.filter { it.status != HalalStatus.HALAL }
        assertEquals(result.describe(), 1, doubtful.size)
        assertEquals(result.describe(), "natural_flavouring", doubtful.single().ingredient?.id)
    }

    @Test
    fun `Alpro labeled vegan is halal`() {
        val result = analyze(
            "Wasser, MANDELN (2,3%), Zucker, Calcium (Tricalciumphosphat), Meersalz, Stabilisatoren (Johannisbrotkernmehl, " +
                "Gellan), Emulgator (Lecithine), natürliches Aroma, Vitamine (B2, B12, E, D2). Mandeln aus Spanien. " +
                "Kann Spuren von Schalenfrüchten (keine Erdnüsse) enthalten.",
            labels = listOf("en:vegetarian", "en:vegan", "en:no-milk"),
            additives = listOf("en:e322", "en:e418")
        )
        assertEquals(result.describe(), HalalStatus.HALAL, result.halalStatus)
    }

    @Test
    fun `Pom-Baer Ketchup is doubtful until its vegan label is confirmed`() {
        // Open Food Facts only lists "en:no-gluten"; the package says "für Veganer geeignet"
        val text = "Kartoffelpulver (30%), pflanzliche Öle (Sonnenblume, Raps in veränderlichen Gewichtsanteilen), " +
            "Maisstärke, Kartoffelstärke, Zucker, jodiertes Speisesalz (Speisesalz, Kaliumjodat), natürliches Aroma, " +
            "Hefeextrakt, Speisesalz, Emulgator (Mono - und Diglyceride von Speisefettsäuren), Zwiebelpulver, " +
            "Tomatenpulver, Gewürz, Knoblauchpulver, Säuerungsmittel (Citronensäure), Kräuter. Kann Spuren von SOJA enthalten."
        val unlabeled = analyze(text, labels = listOf("en:no-gluten"), additives = listOf("en:e330", "en:e471"))
        assertEquals(unlabeled.describe(), HalalStatus.MASHBOOH, unlabeled.halalStatus)
        val doubtful = unlabeled.analyzedIngredients.filter { it.status != HalalStatus.HALAL }.map { it.ingredient?.id }.toSet()
        assertEquals(unlabeled.describe(), setOf("e471", "natural_flavouring"), doubtful)
        assertTrue(unlabeled.dietLabelWouldResolve)

        val confirmed = engine.analyzeProduct(
            Product(
                barcode = "0", name = "Test", ingredientsText = text, source = DataSource.OPEN_FOOD_FACTS,
                userClaims = setOf(PackageClaim.VEGAN)
            ),
            index,
            Madhab.HANAFI
        )
        assertEquals(confirmed.describe(), HalalStatus.HALAL, confirmed.halalStatus)
    }

    @Test
    fun `vanilla extract without manufacturer data is doubtful, not haram, in every school (REG-06)`() {
        Madhab.entries.forEach { madhab ->
            val result = engine.analyzeProduct(
                Product(barcode = "0", name = "Test", ingredientsText = "Zucker, Bourbon-Vanilleextraktpulver", source = DataSource.OPEN_FOOD_FACTS),
                index,
                madhab
            )
            assertEquals("$madhab: ${result.describe()}", HalalStatus.MASHBOOH, result.halalStatus)
        }
    }

    @Test
    fun `negative Open Food Facts labels are not in the known label sets`() {
        listOf("en:non-vegan", "en:non-vegetarian").forEach { tag ->
            assertFalse(tag, tag in KnownLabels.VEGAN || tag in KnownLabels.VEGETARIAN || tag in KnownLabels.HALAL)
        }
    }

    @Test
    fun `dark chocolate with trace note is halal`() {
        val result = analyze("Kakaomasse, Zucker, Kakaobutter, Emulgator (Sojalecithin), Vanille. Kann Haselnüsse, Mandeln, Milch enthalten.")
        assertEquals(result.describe(), HalalStatus.HALAL, result.halalStatus)
        assertEquals(result.describe(), 5, result.analyzedIngredients.size)
    }

    @Test
    fun `strawberry and raspberry compounds are not beer`() {
        // Tester report: strawberry milk was HARAM because "Erdbeer" ended in the English "beer"
        listOf(
            "Vollmilch, Zucker, Erdbeer-Fruchtzubereitung (Erdbeeren, Zucker, Pektin), Rote-Bete-Saftkonzentrat",
            "Joghurt, Himbeer-Joghurt, Erdbeer-Bananen-Smoothie, Heidelbeer-Saft, Erdbeerpüree"
        ).forEach { text ->
            val result = analyze(text)
            assertEquals(result.describe(), HalalStatus.HALAL, result.halalStatus)
        }
        // Without its composition the fruit preparation is doubtful (gelatine), never haram
        val unlisted = analyze("Milch, Zucker, Erdbeer-Fruchtzubereitung")
        assertEquals(unlisted.describe(), HalalStatus.MASHBOOH, unlisted.halalStatus)
    }

    @Test
    fun `fruit flesh and sugar alcohols are halal`() {
        val result = analyze("Zitronensaft mit Fruchtfleisch, Kokosfleisch, Kokosraspeln, mehrwertige Alkohole")
        assertEquals(result.describe(), HalalStatus.HALAL, result.halalStatus)
        assertEquals(result.describe(), 4, result.analyzedIngredients.size)
    }

    @Test
    fun `alcohol in a German compound is still found`() {
        listOf("Rum-Rosinen", "Rumrosinen", "Himbeerlikör", "Weißwein").forEach { text ->
            assertEquals(text, HalalStatus.HARAM, analyze(text).halalStatus)
        }
    }

    @Test
    fun `ethanol of unknown origin is doubtful, also with a vegan label`() {
        // askimam: forbidden from grapes or dates, permissible from other sources if not intoxicating
        val bread = "Weizenmehl, Wasser, Hefe, Speisesalz, Rapsöl, Ethanol"
        assertEquals(HalalStatus.MASHBOOH, analyze(bread).halalStatus)
        assertEquals(HalalStatus.MASHBOOH, analyze(bread, labels = listOf("en:vegan")).halalStatus)
    }

    @Test
    fun `text around a photographed ingredient list is ignored`() {
        // From a tester's export: OFF's text of an elmex chewing gum, OCR'd from the whole package
        val result = analyze(
            "DOSE AUS 30% RECYCELTEM PLASTIK DE MARS GMBH, POSTFACH 1414, D-82004 UNTERHACHING. www.mar.de 089-130 130 60 " +
                "ZUCKERFREIER KAUGUMMI MIT SÜSSUNGSMITTELN UND ERDBEERGESCHMACK. ZUTATEN: SÜSSUNGSMITTEL (SORBIT, XYLIT), " +
                "VERDICKUNGSMITTEL (GUMMI ARABICUM), EMULGATOR (SOJALECITHIN). KANN BEI ÜBERMÄSSIGEM VERZEHR ABFÜHREND WIRKEN. " +
                "ENT ALT EINE PHENYLALANINQUELLE. ABWECHSLUNGSREICHE AUSGEW ERNÄHRUNG UND GESUNDE LEBENSWEISE SIND WICHTIG. " +
                "KUHL UND TROCK BEWAHREN. 50 STOCK 40592 12 AUSGE MEN DECKEL, FOLIE UND ETIKETT MAHR INFORMATION ENE FETT " +
                "100 g 731 kJ/175 kcal DAVON ZUCKER 0g - MEHRWERTIGE ALKOHOLE 71 g"
        )
        assertEquals(result.describe(), HalalStatus.HALAL, result.halalStatus)
        assertEquals(result.describe(), listOf("e420", "e967", "e414", "soya_lecithin"), result.analyzedIngredients.map { it.ingredient?.id })
    }

    @Test
    fun `quoted spring names, html entities and certification notes are not ingredients`() {
        val water = analyze("Natürliches Mineralwasser &quot;Fonti di Crodo-Lisiel&quot;, Zucker")
        assertEquals(water.describe(), HalalStatus.HALAL, water.halalStatus)
        assertEquals(water.describe(), 2, water.analyzedIngredients.size)

        val tea = analyze("Wasser, Zucker, Zitronensaft. **Rainforest Alliance-zertifiziert. www.ra.org")
        assertEquals(tea.describe(), HalalStatus.HALAL, tea.halalStatus)
        assertEquals(tea.describe(), 3, tea.analyzedIngredients.size)
    }

    @Test
    fun `OCR errors from the second tester export are recognized`() {
        mapOf(
            // Krombacher Spezi: words broken at the line end
            "Süßungsmittel (Natri - umcyclamat, Acesulfam-K, Sucralose und Natriumsaccharin), " +
                "Säureregulator Tri - natríumcitrat" to null,
            // Active O2: lost umlaut, footnote
            "Natürliches Mineralwasser, Apfelsaft* (42%), Orangensaft (5%), Glukose-Fruktose-Sirup, Zitronensaft&quot; (2,2%), " +
                "Zucker, Sternfruchtsaft' (0,5%), Kohlensäure, Maracujasaft* (0,3%), Antioxidationsmittel Ascorbinsaure, " +
                "Provitamin A. *aus Fruchtsaftkonzentrat" to HalalStatus.HALAL,
            // Effect: upper case OCR without umlauts
            "WASSER, ZUCKER, SAUERUNGSMITTEL: CITRONENSAURE, PANTOTHENSAURE, NATURLICHES AROMA" to null,
            // Monster: spelling variant
            "Konservierungsstoffe (Sorbinsäure, Benzosäure)" to HalalStatus.HALAL,
            // Accent in a borrowed name, Latin name in parentheses
            "Jalapeños, Wasser, 100% Chiasamen (Salvia hispanica)" to HalalStatus.HALAL
        ).forEach { (text, expected) ->
            val result = analyze(text)
            assertTrue(result.describe(), result.unknownIngredients.isEmpty())
            if (expected != null) assertEquals(result.describe(), expected, result.halalStatus)
        }
    }

    @Test
    fun `notes after the list of a photographed label are dropped`() {
        // Tester export: Rainforest Alliance footnote, web address, storage notes; OFF filed the German text as French
        val result = analyze(
            "Weizenmehl. pflanzliche Fette (palm), Wasser Zucker. pflanzliche Öle (Raps. Kokos. Sonnenblume). Hefe, " +
                "1.7% fettarmes Kakaopulver Dextrose, Sojamehl, Speisesal Backtriebmittel: Diphosphate. Natriumcarbonate: " +
                "Magermilchpulver. Emulgatoren: Mono - und Diglyceride von Speisefettsäuren** Natriumstearoyl - 2-lactylat: " +
                "natürliches Aroma. * Rainforest Alliance-zertifziert. Weitere Informationen unter ra.org. &quot;auf " +
                "pflanzlicher Basis. Kann Spuren von Ei und Schalenfrüchten enthalten. Aufbewahrung: Vor Wärme schützen " +
                "und trocken lagern. Nach dem Öffnen innerhalb von 1 Tag verzehren. Ungeöffnet zu"
        )
        val names = result.analyzedIngredients.map { it.name.lowercase() }
        listOf("rainforest", "ra.org", "pflanzlicher basis", "ungeöffnet").forEach { junk ->
            assertTrue(result.describe(), names.none { junk in it })
        }
        assertTrue(result.describe(), result.unknownIngredients.isEmpty())
    }

    @Test
    fun `Yfood with its German list and vegan label is halal`() {
        val result = analyze(
            "Wasser, Sojaprotein, pflanzliche Öle (Raps, Sonnenblume), lösliche Maisfaser, Maltodextrin, glutenfreie " +
                "Haferfaser, Tapiokastärke, Reispulver (Reismehl, Reisstärke, Reissirup), fett - armer Kakao 1,9%, Vitamine " +
                "(A, C, K, B1, B3, B5, B6, Folsäure, B12), Mineralstoffe (Chlorid, Calcium, Magnesium, Zink, Mangan, Selen, " +
                "Chrom, Molybdän, Jod), Emulgator: Lecithine (Soja); Maisstärke, Süßungsmittel: Sucralose; Stabilisatoren: " +
                "Johannisbrotkernmehl, Gellan; Salz, natürliche Aromen.",
            labels = listOf("en:vegan", "en:vegetarian")
        )
        assertEquals(result.describe(), HalalStatus.HALAL, result.halalStatus)
    }

    private fun analyze(text: String, labels: List<String> = emptyList(), additives: List<String> = emptyList()) =
        engine.analyzeProduct(
            Product(
                barcode = "0",
                name = "Test",
                ingredientsText = text,
                labelsTags = labels,
                additivesTags = additives,
                source = DataSource.OPEN_FOOD_FACTS
            ),
            index,
            Madhab.HANAFI
        )

    private fun Product.describe() = analyzedIngredients.joinToString("\n", prefix = "\n") {
        "${it.status} ${it.name} -> ${it.ingredient?.id} (${it.reason?.javaClass?.simpleName})"
    }
}
