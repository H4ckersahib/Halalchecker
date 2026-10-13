package com.halalcheck.app.domain.engine

import java.util.Locale

enum class QualifierKind { HARAM, ANIMAL, HALAL_SOURCE }

/**
 * Source words that appear in parentheses ("Gelatine (Rind)") or as the first part of a German
 * compound ("Rinderspeck"). Matched on word tokens so that "bee" does not match "beef".
 */
object Qualifiers {

    private val haram = setOf(
        "schwein", "schweine", "pork", "porc", "porcine", "pig", "swine", "suino", "domuz"
    )

    private val animal = setOf(
        "rind", "rinder", "beef", "bovine", "cattle",
        "kalb", "kalbs", "veal", "calf",
        "huhn", "hühner", "hähnchen", "chicken", "poultry", "geflügel",
        "pute", "puten", "truthahn", "turkey", "ente", "duck", "gans", "goose",
        "lamm", "lamb", "schaf", "sheep", "ziege", "goat", "hirsch", "wild",
        "tier", "tierisch", "animal"
    )

    private val halalSource = setOf(
        // Soja / Soy
        "soja", "soy", "soya", "sojabohne",
        // Sonnenblume / Sunflower
        "sonnenblume", "sunflower",
        // Pflanzlich / Vegetable
        "pflanzlich", "pflanzen", "vegetable", "plant", "vegan", "végétal", "végétale",
        // Raps / Rapeseed
        "raps", "rapeseed", "canola",
        // Mais / Corn
        "mais", "corn",
        // Kokos / Coconut
        "kokos", "coconut",
        // Andere pflanzliche Quellen
        "palm", "olive", "weizen", "wheat", "reis", "rice", "kartoffel", "potato",
        "erbse", "erbsen", "pea", "lein", "flax", "hanf", "hemp", "mandel", "almond",
        // Synthetisch / Mikrobiell
        "synthetisch", "synthetic", "mikrobiell", "microbial", "bakteriell", "bacterial",
        // Früchte
        "citrus", "zitrus", "apfel", "apple", "trauben", "grape",
        // Tierisch, aber halal
        "biene", "bee", "fisch", "fish",
        // Weitere
        "tapioka", "tapioca", "maniok", "cassava", "shea", "karite",
        "alge", "algen", "algae", "seaweed", "seetang",
        // Nüsse
        "erdnuss", "peanut", "haselnuss", "hazelnut", "walnuss", "walnut",
        "cashew", "pistazie", "macadamia"
    )

    private val tokenSplit = Regex("[^\\p{L}]+")

    /** Kind of the strongest source word in [text]: HARAM beats ANIMAL beats HALAL_SOURCE. */
    fun kindOf(text: String): QualifierKind? {
        val tokens = text.lowercase(Locale.ROOT).split(tokenSplit).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        return when {
            tokens.any { token -> haram.any { token == it || (it.length >= 6 && token.contains(it)) || matches(token, it) } } ->
                QualifierKind.HARAM
            tokens.any { token -> animal.any { matches(token, it) } } -> QualifierKind.ANIMAL
            tokens.any { token -> halalSource.any { matches(token, it) } } -> QualifierKind.HALAL_SOURCE
            else -> null
        }
    }

    /** Exact token, or a German stem ("rinder" for "rind", "sonnenblumen" for "sonnenblume"). */
    private fun matches(token: String, word: String): Boolean =
        token == word || (word.length >= 4 && token.startsWith(word))
}
