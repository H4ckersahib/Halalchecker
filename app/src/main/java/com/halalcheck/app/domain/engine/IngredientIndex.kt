package com.halalcheck.app.domain.engine

import com.halalcheck.app.domain.model.Ingredient
import java.util.Locale

/** [modifier] is the first part of a compound that was matched by its last part ("Puten" in "Putenschinken"). */
data class IngredientMatch(
    val ingredient: Ingredient,
    val inferred: Boolean = false,
    val modifier: String? = null
)

/**
 * In-memory lookup over all ingredient ids, names and synonyms (all languages).
 * Build it once per ingredient list; lookups are hash based.
 */
class IngredientIndex(ingredients: List<Ingredient>) {

    val size: Int = ingredients.size

    private val byId = HashMap<String, Ingredient>(ingredients.size * 2)
    private val primaryNames = HashMap<String, Ingredient>()
    private val synonyms = HashMap<String, Ingredient>()
    private val compactNames = HashMap<String, Ingredient>()
    private val classNames = HashSet<String>()

    init {
        // Hand-curated entries win name collisions over entries derived from the OFF taxonomy.
        val ordered = ingredients.sortedWith(
            compareBy<Ingredient>({ it.source.orEmpty().startsWith("off_") }, { it.inferredFrom != null }, { it.id })
        )
        for (ingredient in ordered) {
            byId[ingredient.id] = ingredient
            val primary = listOfNotNull(
                ingredient.nameDe, ingredient.nameEn, ingredient.nameTr, ingredient.nameAr, ingredient.nameFr
            )
            for (name in primary) primaryNames.putIfAbsent(normalize(name), ingredient)
            for (name in ingredient.synonyms) synonyms.putIfAbsent(normalize(name), ingredient)
            for (name in ingredient.allNames()) {
                compactNames.putIfAbsent(normalize(name).replace(" ", ""), ingredient)
                if (ingredient.isClass) classNames.add(normalize(name))
            }
        }
    }

    fun byId(id: String): Ingredient? = byId[id]

    /** Open Food Facts taxonomy id ("en:soya-lecithin") to our id ("soya_lecithin"). */
    fun byOffId(offId: String): Ingredient? {
        val (lang, rest) = offId.split(":", limit = 2).takeIf { it.size == 2 } ?: return null
        val id = rest.replace('-', '_')
        return byId[if (lang == "en") id else "${lang}_$id"]
    }

    fun isClassName(text: String): Boolean = normalize(text) in classNames

    /** Whole-text match on a name or synonym, tolerating spacing and simple plural endings. */
    fun exact(text: String): Ingredient? = lookupNormalized(normalize(text))

    /** "E 322 i" / "e322i" / "INS 322" -> e322i, falling back to e322. */
    fun eNumber(digits: String, suffix: String?): Ingredient? {
        val base = "e$digits"
        val withSuffix = suffix?.let { "$base$it" }
        return withSuffix?.let { byId[it] } ?: byId[base]
    }

    /**
     * All ingredients found in [phrase]:
     * 1. the whole phrase,
     * 2. else every E-number in it plus what the words around them contain
     *    ("Schweinefleisch mit E471" -> both),
     * 3. the longest known word spans ("natürliche Aromen inkl Koffein" -> both),
     * 4. plus compound heads for words no span covered ("Putenschinken" -> "Schinken", inferred),
     *    or the base product of a form word ("Kartoffelpulver" -> "Kartoffel", inferred).
     * A functional class ("Farbstoff") only counts when nothing more specific was found.
     */
    fun findAll(phrase: String): List<IngredientMatch> {
        val normalized = normalize(phrase)
        if (normalized.isEmpty()) return emptyList()
        lookupNormalized(normalized)?.let { return listOf(IngredientMatch(it)) }

        val matches = mutableListOf<IngredientMatch>()
        E_NUMBER.findAll(normalized).forEach { m ->
            eNumber(m.groupValues[1], m.groupValues[2].ifEmpty { null })?.let { matches += IngredientMatch(it) }
        }
        // Split at the E-numbers so that no word span bridges one
        E_NUMBER.split(normalized).map { it.trim() }.filter { it.isNotEmpty() }.forEach { segment ->
            matches += wordMatches(segment.split(' '))
        }
        val distinct = matches.distinctBy { it.ingredient.id }
        return distinct.filterNot { it.ingredient.isClass }.ifEmpty { distinct }
    }

    private fun wordMatches(words: List<String>): List<IngredientMatch> {
        val covered = BooleanArray(words.size)
        val matches = mutableListOf<IngredientMatch>()
        for (length in words.size downTo 1) {
            for (start in 0..words.size - length) {
                if ((start until start + length).any { covered[it] }) continue
                val span = words.subList(start, start + length)
                if (span.all { it in STOPWORDS || it.length < 3 }) continue
                val ingredient = lookupNormalized(span.joinToString(" ")) ?: continue
                matches += IngredientMatch(ingredient)
                for (i in start until start + length) covered[i] = true
            }
        }
        words.forEachIndexed { i, word ->
            if (!covered[i]) (headMatch(word) ?: formMatch(word))?.let { matches += it }
        }
        return matches
    }

    private fun headMatch(word: String): IngredientMatch? {
        if (word.length < MIN_HEAD + MIN_MODIFIER) return null
        for (split in MIN_MODIFIER..word.length - MIN_HEAD) {
            val ingredient = lookupNormalized(word.substring(split)) ?: continue
            // "Kohlensäure" is not a kind of "Säuerungsmittel"; class words say nothing about a compound
            if (ingredient.isClass) continue
            return IngredientMatch(ingredient, inferred = true, modifier = word.substring(0, split))
        }
        return null
    }

    /** "Kartoffelpulver" is potato in another form. Not "…extrakt": extracts may be made with alcohol. */
    private fun formMatch(word: String): IngredientMatch? {
        val suffix = FORM_SUFFIXES.firstOrNull { word.endsWith(it) && word.length - it.length >= 3 } ?: return null
        val ingredient = lookupNormalized(word.dropLast(suffix.length)) ?: return null
        if (ingredient.isClass) return null
        return IngredientMatch(ingredient, inferred = true)
    }

    private fun lookupNormalized(normalized: String): Ingredient? {
        if (normalized.isEmpty()) return null
        direct(normalized)?.let { return it }
        for (ending in PLURAL_ENDINGS) {
            if (normalized.endsWith(ending) && normalized.length - ending.length >= 4) {
                direct(normalized.dropLast(ending.length))?.let { return it }
            }
        }
        return null
    }

    private fun direct(normalized: String): Ingredient? =
        primaryNames[normalized] ?: synonyms[normalized] ?: compactNames[normalized.replace(" ", "")]

    companion object {
        private const val MIN_HEAD = 4
        private const val MIN_MODIFIER = 2
        private val PLURAL_ENDINGS = listOf("en", "n", "e", "s", "es", "er")
        private val FORM_SUFFIXES = listOf(
            "pulver", "flocken", "granulat", "grieß", "mehl", "püree", "mark", "stückchen", "stücke",
            "würfel", "konzentrat", "paste", "saft", "raspeln", "splitter", "chips"
        )
        private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

        /** E or INS number as a whole token; suffix is a letter (e150d) or roman numeral (e322i, e450iii). */
        private val E_NUMBER = Regex("(?<![\\p{L}\\p{N}])(?:e|ins) ?(\\d{3,4}) ?(iii|ii|iv|vi|v|i|[a-h])?(?![\\p{L}\\p{N}])")

        private val STOPWORDS = setOf(
            "und", "oder", "mit", "aus", "von", "vom", "der", "die", "das", "den", "dem", "des", "in", "im",
            "an", "auf", "zu", "ohne", "bei", "nach", "sowie", "inkl", "inklusive", "einschließlich",
            "and", "or", "with", "from", "of", "the", "including",
            "de", "du", "la", "le", "les", "et", "a", "al", "ve", "ile"
        )

        fun normalize(text: String): String =
            text.lowercase(Locale.ROOT).replace(NON_ALPHANUMERIC, " ").trim()
    }
}
