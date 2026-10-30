package com.halalcheck.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextTest {

    @Test
    fun `lines of a block become one line and broken words are joined`() {
        val text = OcrText.join(
            listOf(
                listOf("Zutaten: Wasser, Zucker, Süßungsmittel (Natri-", "umcyclamat), Emulgator (Mono-", "und Diglyceride", "von Speisefettsäuren)")
            )
        )
        assertEquals(
            "Zutaten: Wasser, Zucker, Süßungsmittel (Natriumcyclamat), Emulgator (Mono- und Diglyceride von Speisefettsäuren)",
            text
        )
    }

    @Test
    fun `blocks become sentences and empty lines are skipped`() {
        val text = OcrText.join(listOf(listOf("Erdbeer Joghurt", ""), listOf("Zutaten: Joghurt, Erdbeeren."), listOf(" ")))
        assertEquals("Erdbeer Joghurt. Zutaten: Joghurt, Erdbeeren", text)
    }

    @Test
    fun `a hyphen before a capitalized word stays`() {
        assertEquals("Glukose-Fruktose-Sirup", OcrText.join(listOf(listOf("Glukose-", "Fruktose-Sirup"))).replace(" ", ""))
        assertEquals("Vollmilch- Schokolade", OcrText.join(listOf(listOf("Vollmilch-", "Schokolade"))))
    }

    @Test
    fun `the photographed list goes through the normal parser`() {
        val text = OcrText.join(
            listOf(
                listOf("Fruchtgummi"),
                listOf("Zutaten: Glukosesirup, Zucker, Gelatine, Säuerungs-", "mittel: Citronensäure"),
                listOf("Nährwerte je 100 g", "Brennwert 1435 kJ / 343 kcal")
            )
        )
        val parser = IngredientTextParser(isClassName = { it.equals("Säuerungsmittel", ignoreCase = true) }, isKnown = { true })
        assertEquals(listOf("Glukosesirup", "Zucker", "Gelatine", "Citronensäure"), parser.parse(text).map { it.name })
    }
}
