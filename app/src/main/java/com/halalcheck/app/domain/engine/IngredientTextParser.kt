package com.halalcheck.app.domain.engine

/**
 * A parsed entry of an ingredient list.
 *
 * - "Lecithine (Soja)": name "Lecithine", qualifiers ["Soja"]. [children] holds the same words parsed
 *   as ingredients, used if the name itself is unknown ("Getreide (Weizen, Reis)").
 * - "Schokolade (Zucker, Kakaobutter)": [children] is the composition.
 * - "Fruchtsaft: Apfel, Orange": [isListHead], its composition follows as separate entries.
 * - "Schweinegelatine (E428)": [eNumber] next to the name; the engine compares both.
 */
data class ParsedIngredient(
    val text: String,
    val name: String,
    val qualifiers: List<String> = emptyList(),
    val children: List<ParsedIngredient> = emptyList(),
    val isClass: Boolean = false,
    val isListHead: Boolean = false,
    val eNumber: String? = null
)

/** [notes] is the label text dropped as no ingredient (address, storage, footnotes), shown separately. */
data class ParseResult(val items: List<ParsedIngredient>, val notes: List<String>)

/**
 * Splits an ingredient list into [ParsedIngredient]s. Knows functional classes ("Emulgator",
 * "Säuerungsmittel", ...) through [isClassName] so that "Säuerungsmittel: Citronensäure" and
 * "Emulgator (E471)" yield the concrete additive instead of the class.
 */
class IngredientTextParser(
    private val isClassName: (String) -> Boolean,
    private val isKnown: (String) -> Boolean,
    private val isAdditive: (String) -> Boolean = { false },
    /** The text names at least one ingredient somewhere ("Alkohol", "Gelatine vom Schwein"). */
    private val mentionsIngredient: (String) -> Boolean = isKnown
) {

    fun parse(ingredientsText: String): List<ParsedIngredient> = parseWithNotes(ingredientsText).items

    fun parseWithNotes(ingredientsText: String): ParseResult {
        val notes = mutableListOf<String>()
        val cleaned = clean(ingredientsText, notes)
        if (cleaned.isBlank()) return ParseResult(emptyList(), notes)
        val items = splitTopLevel(cleaned, ',')
        // "Farbstoff, Zuckerkulör (E150d)": a bare class name directly followed by an additive is a
        // typo for a colon. Otherwise ("Farbstoff, Zucker") the class stays as "not specified".
        val parsed = items
            .filterIndexed { i, item ->
                !isClassName(cleanName(item)) || items.getOrNull(i + 1)?.let { isAdditive(it) } != true
            }
            .flatMap { parseTopLevelItem(it, notes) }
        return ParseResult(parsed, notes)
    }

    private fun clean(text: String, notes: MutableList<String>): String {
        var t = decodeEntities(text)
            // Spring or brand names: Mineralwasser "Fonti di Crodo"
            .replace(QUOTED, " ")
            .replace('[', '(').replace('{', '(').replace(']', ')').replace('}', ')')
            .replace(PERCENTAGE, "")
            .replace(Regex("\\(\\s*\\)"), "")
            .replace(Regex("[;•·]"), ",")
            .replace(DIGIT_BREAK, "$1-")
            .let(::joinBrokenWords)
            .replace(LIST_DASH, ", ")
            .replace(Regex("\\s+"), " ")
            .trim()
        // "Mars GmbH, Postfach … Zutaten: Süßungsmittel …": photographed labels often start before the list
        LABEL.findAll(t).firstOrNull { depthAt(t, it.range.first) == 0 }?.let { label ->
            t.substring(0, label.range.first).trim().takeIf { it.isNotEmpty() }?.let { notes += it }
            t = t.substring(label.range.last + 1).trim()
        }
        // Keep sentences that are not notes ("Kann Spuren von ... enthalten.", "Mandeln aus Spanien.",
        // footnotes "*aus Fruchtsaftkonzentrat"); then drop footnote marks, allergen markup ("_Weizenmehl_") and stray quotes
        val sentences = splitTopLevel(t, '.')
        return sentences.mapIndexedNotNull { i, sentence -> if (i == 0) sentence else keptFromSentence(sentence, notes) }
            .joinToString(", ")
            .replace(Regex("[*_†°\"„“”«»]"), "")
    }

    /** A sentence after the first one: notes and footnotes are dropped, label text up to the ingredients before it. */
    private fun keptFromSentence(sentence: String, notes: MutableList<String>): String? {
        val trimmed = sentence.trim()
        if (NOTE_SENTENCE.containsMatchIn(trimmed) || ORIGIN_SENTENCE.containsMatchIn(trimmed)) return null
        if (FOOTNOTE.containsMatchIn(trimmed)) {
            notes += trimmed
            return null
        }
        return withoutLabelText(trimmed, notes)
    }

    /**
     * Label text in a sentence or item. A nutrition table drops it all ("Fett 100 g 731 kJ"); other label
     * text keeps the ingredients written before it: OCR joins "Zucker, Alkohol" and "Kühl lagern" when it
     * loses the line break, and the alcohol must not go with the storage note. Text in parentheses is
     * left to the parse of the composition.
     */
    private fun withoutLabelText(text: String, notes: MutableList<String>): String? {
        if (NUTRITION.containsMatchIn(text)) {
            notes += text
            return null
        }
        val junk = JUNK.findAll(text).firstOrNull { depthAt(text, it.range.first) == 0 } ?: return text
        // Cut at the start of the word: "Ungeöffnet" must not leave "Un" (Turkish for flour)
        var cut = junk.range.first
        while (cut > 0 && text[cut - 1].isLetter()) cut--
        val before = text.substring(0, cut).trim().trimEnd(',', '.', ':', ';', '-').trim()
        val kept = before.takeIf { it.isNotEmpty() && mentionsIngredient(it) }
        notes += if (kept != null) text.substring(cut).trim() else text
        return kept
    }

    /**
     * "Natri - umcyclamat", "fett - armer Kakao": a word broken at the end of a line (OFF writes the hyphen
     * with spaces). Joined when the first part is no word on its own or the joined word is known, so
     * "Gewürze - natürliches Aroma" and "Mono - und Diglyceride" stay apart.
     */
    private fun joinBrokenWords(text: String): String =
        BROKEN_WORD.replace(text) { m ->
            val (first, second, next) = m.destructured
            val joined = first + second
            if (!isKnown(first) || isKnown(joined) || isKnown(joined + next)) joined else m.value
        }

    /** Open Food Facts sometimes keeps HTML entities: Mineralwasser &quot;Fonti di Crodo&quot;. */
    private fun decodeEntities(text: String): String =
        HTML_ENTITY.replace(text) { m ->
            val name = m.groupValues[1]
            val char = when {
                name.startsWith("#x", ignoreCase = true) -> name.drop(2).toIntOrNull(16)
                name.startsWith("#") -> name.drop(1).toIntOrNull()
                else -> null
            }
            char?.takeIf { Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) }
                ?: NAMED_ENTITIES[name.lowercase()]
                ?: m.value
        }

    private fun depthAt(text: String, index: Int): Int {
        var depth = 0
        for (i in 0 until index) {
            when (text[i]) {
                '(' -> depth++
                ')' -> depth = (depth - 1).coerceAtLeast(0)
            }
        }
        return depth
    }

    private fun parseTopLevelItem(item: String, notes: MutableList<String>): List<ParsedIngredient> {
        if (item.trim().length <= 1 || NOTE_ITEM.containsMatchIn(item.trim())) return emptyList()
        val text = withoutLabelText(item.trim(), notes) ?: return emptyList()
        val colon = indexOfTopLevel(text, ':')
        if (colon > 0) {
            val head = text.substring(0, colon).trim()
            val tail = text.substring(colon + 1).trim()
            val tailItems = parseItem(tail, notes)
            if (isClassName(head)) return tailItems
            // "Kakao: mindestens 30 %" has nothing after the colon worth keeping
            if (tailItems.isEmpty()) return parseItem(head, notes)
            return parseItem(head, notes).map { it.copy(isListHead = true) } + tailItems
        }
        return parseItem(text, notes)
    }

    private fun parseItem(raw: String, notes: MutableList<String>): List<ParsedIngredient> {
        val trimmed = raw.trim().removeSuffix(".").trim()
        if (trimmed.length <= 1 || NOTE_ITEM.containsMatchIn(trimmed)) return emptyList()
        val text = withoutLabelText(trimmed, notes) ?: return emptyList()

        val open = text.indexOf('(')
        val close = if (open >= 0) matchingParen(text, open) else -1
        if (open < 0 || close < 0) {
            val name = stripClassPrefix(cleanName(text))
            if (name.isBlank()) return emptyList()
            return listOf(ParsedIngredient(text = text, name = name, isClass = isClassName(name)))
        }

        val main = cleanName(text.substring(0, open) + " " + text.substring(close + 1))
        val content = text.substring(open + 1, close)
        val items = splitTopLevel(content, ',').map { it.trim() }.filter { it.isNotEmpty() }
        if (main.isBlank()) return items.flatMap { parseItem(it, notes) }
        val name = stripClassPrefix(main)

        // "Emulgator (E471)": the E-number is the ingredient. "Schweinegelatine (E428)": the name is more
        // specific and must not get lost, the engine compares it with the number.
        if (items.size == 1 && E_NUMBER_ONLY.matches(items[0])) {
            if (isClassName(name) || !mentionsIngredient(name)) return listOf(ParsedIngredient(text = text, name = items[0]))
            return listOf(ParsedIngredient(text = text, name = name, eNumber = items[0]))
        }

        val isVitamins = name.startsWith("vitamin", ignoreCase = true)
        val children = items.flatMap { item ->
            val childText = if (isVitamins && item.length <= 4) "Vitamin $item" else item
            parseItem(childText, notes)
        }

        // "Gelatine (Rind)", "Lecithine (Soja)": parentheses only name the source
        if (items.all { Qualifiers.kindOf(it) != null && it.split(' ').size <= 3 }) {
            return listOf(
                ParsedIngredient(text = text, name = name, qualifiers = items, children = children, isClass = isClassName(name))
            )
        }
        return listOf(ParsedIngredient(text = text, name = name, children = children, isClass = isClassName(name)))
    }

    /** "Emulgator Lecithine" -> "Lecithine", "Farbstoff E 150d" -> "E 150d". */
    private fun stripClassPrefix(name: String): String {
        if (isKnown(name)) return name
        val words = name.split(' ')
        for (prefixLength in minOf(3, words.size - 1) downTo 1) {
            if (isClassName(words.take(prefixLength).joinToString(" "))) {
                return stripClassPrefix(words.drop(prefixLength).joinToString(" "))
            }
        }
        return name
    }

    private fun cleanName(name: String): String =
        name.replace(Regex("\\s+"), " ")
            .trim()
            .replace(LEADING_FILLER, "")
            .trim(' ', ',', '.', ':', '-')

    private fun indexOfTopLevel(text: String, char: Char): Int {
        var depth = 0
        text.forEachIndexed { i, c ->
            when (c) {
                '(' -> depth++
                ')' -> depth = (depth - 1).coerceAtLeast(0)
                char -> if (depth == 0) return i
            }
        }
        return -1
    }

    private fun matchingParen(text: String, open: Int): Int {
        var depth = 0
        for (i in open until text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> if (--depth == 0) return i
            }
        }
        return -1
    }

    /**
     * Splits at [separator] outside parentheses. A comma between digits ("2,5"), a period not followed
     * by a space or the end ("ca.5", "2.5") and the period of an abbreviation ("bei max. 7 °C") do not split.
     */
    private fun splitTopLevel(text: String, separator: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        text.forEachIndexed { i, c ->
            val prev = text.getOrNull(i - 1)
            val next = text.getOrNull(i + 1)
            val splits = c == separator && depth == 0 && when (separator) {
                ',' -> !(prev?.isDigit() == true && next?.isDigit() == true)
                '.' -> (next == null || next == ' ') && current.takeLastWhile { it.isLetter() }.toString().lowercase() !in ABBREVIATIONS
                else -> true
            }
            when {
                c == '(' -> { depth++; current.append(c) }
                c == ')' -> { depth = (depth - 1).coerceAtLeast(0); current.append(c) }
                splits -> { parts += current.toString(); current.clear() }
                else -> current.append(c)
            }
        }
        parts += current.toString()
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private companion object {
        /** " - " separates entries, except in "Frucht - und Pflanzenkonzentrate". */
        val LIST_DASH = Regex(" - (?!(und|oder|and|or)\\b)", RegexOption.IGNORE_CASE)
        val PERCENTAGE = Regex("<?\\s*\\d+(?:[.,]\\d+)?\\s*%")
        val LABEL = Regex("(?<!\\p{L})(zutaten|zusammensetzung|ingredients|ingrédients|içindekiler)\\s*:", RegexOption.IGNORE_CASE)
        val LEADING_FILLER = Regex("^(enthält|contains|aus|from|davon|thereof)\\s+", RegexOption.IGNORE_CASE)
        val E_NUMBER_ONLY = Regex("(?:E|INS)\\s*-?\\s*\\d{3,4}\\s*[a-z]{0,3}", RegexOption.IGNORE_CASE)
        val NOTE_SENTENCE = Regex(
            "spuren|kann .*enthalten|enthalten sein|may contain|traces|hergestellt|produziert|verpackt|" +
                "ursprung|herkunft|origin|mindestens|landwirtschaft|kontrolliert|organic|^\\s*ohne\\b|frei von|" +
                "without|free from|^\\s*bio\\b",
            RegexOption.IGNORE_CASE
        )
        val ORIGIN_SENTENCE = Regex("\\b(aus|from)\\s+\\p{Lu}\\p{L}+$")
        val NOTE_ITEM = Regex("^(mindestens|min\\b|at least|kann\\b|may contain|spuren|traces?\\b)", RegexOption.IGNORE_CASE)

        /**
         * Text around the list on photographed labels: address, web links ("ra.org"), certification
         * ("zertifziert" is a common typo), storage and use.
         */
        val JUNK = Regex(
            "www\\.|https?://|\\S@\\S|\\b[\\p{L}\\d-]+\\.(?:org|de|com|net|eu|at|ch|info)\\b|\\bpostfach\\b|\\bgmbh\\b|" +
                "zertif|certified|informationen|verzehr|phenylalanin|" +
                "lebensweise|ernährung|\\bk[üu]hl\\b|\\blagern\\b|lagerung|aufbewahr|\\bbewahren\\b|geöffnet|öffnen|" +
                "\\bschützen\\b|verbrauchen|\\d\\s*°\\s*c\\b|recycel",
            RegexOption.IGNORE_CASE
        )

        /** The nutrition table after the list: its "Fett" and "Zucker" are nutrients, not ingredients. */
        val NUTRITION = Regex("\\bkcal\\b|\\bkj\\b|brennwert|nährwert|nutrition", RegexOption.IGNORE_CASE)

        /** A sentence that starts with a footnote mark ("* Rainforest Alliance-zertifiziert"); OCR turns "**" into a quote. */
        val FOOTNOTE = Regex("^[*\"'„“”«»†¹²³]")
        val BROKEN_WORD = Regex("(\\p{L}+) - (?!(?:und|oder|bzw|sowie|and|or)\\b)(\\p{Ll}+)(?=((?: \\p{L}+)?))")

        val ABBREVIATIONS = setOf("max", "min", "mind", "ca", "inkl", "bzw", "ggf", "evtl", "vgl", "zzgl", "gem", "nr", "std", "approx")

        /** "Natriumstearoyl - 2-lactylat" */
        val DIGIT_BREAK = Regex("(\\p{L}) - (?=\\d+-\\p{L})")
        val QUOTED = Regex("[\"„“”«»][^\"„“”«»,()]{1,60}[\"„“”«»]")
        val HTML_ENTITY = Regex("&(#\\d{1,7}|#x[0-9a-f]{1,6}|[a-z]{2,8});", RegexOption.IGNORE_CASE)
        val NAMED_ENTITIES = mapOf(
            "quot" to "\"", "amp" to "&", "apos" to "'", "lt" to "<", "gt" to ">", "nbsp" to " ",
            "auml" to "ä", "ouml" to "ö", "uuml" to "ü", "szlig" to "ß"
        )
    }
}
