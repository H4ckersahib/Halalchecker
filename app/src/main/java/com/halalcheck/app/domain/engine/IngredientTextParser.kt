package com.halalcheck.app.domain.engine

/**
 * A parsed entry of an ingredient list.
 *
 * - "Lecithine (Soja)": name "Lecithine", qualifiers ["Soja"]. [children] holds the same words parsed
 *   as ingredients, used if the name itself is unknown ("Getreide (Weizen, Reis)").
 * - "Schokolade (Zucker, Kakaobutter)": [children] is the composition.
 * - "Fruchtsaft: Apfel, Orange": [isListHead], its composition follows as separate entries.
 */
data class ParsedIngredient(
    val text: String,
    val name: String,
    val qualifiers: List<String> = emptyList(),
    val children: List<ParsedIngredient> = emptyList(),
    val isClass: Boolean = false,
    val isListHead: Boolean = false
)

/**
 * Splits an ingredient list into [ParsedIngredient]s. Knows functional classes ("Emulgator",
 * "Säuerungsmittel", ...) through [isClassName] so that "Säuerungsmittel: Citronensäure" and
 * "Emulgator (E471)" yield the concrete additive instead of the class.
 */
class IngredientTextParser(
    private val isClassName: (String) -> Boolean,
    private val isKnown: (String) -> Boolean
) {

    fun parse(ingredientsText: String): List<ParsedIngredient> {
        val cleaned = clean(ingredientsText)
        if (cleaned.isBlank()) return emptyList()
        val items = splitTopLevel(cleaned, ',')
        // "Farbstoff, Zuckerkulör (E150d)": a bare class name must be followed by the additive it
        // names (EU labeling rules), so a comma there is a typo for a colon. Only a trailing class
        // stays as "not specified".
        return items
            .filterIndexed { i, item -> i == items.lastIndex || !isClassName(cleanName(item)) }
            .flatMap { parseTopLevelItem(it) }
    }

    private fun clean(text: String): String {
        var t = text
            .replace('[', '(').replace('{', '(').replace(']', ')').replace('}', ')')
            .replace(Regex("[*_†°]"), "")
            .replace(PERCENTAGE, "")
            .replace(Regex("\\(\\s*\\)"), "")
            .replace(Regex("[;•·]"), ",")
            .replace(LIST_DASH, ", ")
            .replace(Regex("\\s+"), " ")
            .trim()
        t = t.replace(LEADING_LABEL, "")
        // Keep sentences that are not notes ("Kann Spuren von ... enthalten.", "Mandeln aus Spanien.")
        val sentences = splitTopLevel(t, '.')
        return sentences.filterIndexed { i, s -> i == 0 || !isNoteSentence(s) }.joinToString(", ")
    }

    private fun isNoteSentence(sentence: String): Boolean =
        NOTE_SENTENCE.containsMatchIn(sentence) || ORIGIN_SENTENCE.containsMatchIn(sentence.trim())

    private fun parseTopLevelItem(item: String): List<ParsedIngredient> {
        val text = item.trim()
        if (text.length <= 1 || NOTE_ITEM.containsMatchIn(text)) return emptyList()
        val colon = indexOfTopLevel(text, ':')
        if (colon > 0) {
            val head = text.substring(0, colon).trim()
            val tail = text.substring(colon + 1).trim()
            val tailItems = parseItem(tail)
            if (isClassName(head)) return tailItems
            // "Kakao: mindestens 30 %" has nothing after the colon worth keeping
            if (tailItems.isEmpty()) return parseItem(head)
            return parseItem(head).map { it.copy(isListHead = true) } + tailItems
        }
        return parseItem(text)
    }

    private fun parseItem(raw: String): List<ParsedIngredient> {
        val text = raw.trim().removeSuffix(".").trim()
        if (text.length <= 1 || NOTE_ITEM.containsMatchIn(text)) return emptyList()

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
        if (main.isBlank()) return items.flatMap { parseItem(it) }
        val name = stripClassPrefix(main)

        // "Emulgator (E471)": the E-number is the ingredient
        if (items.size == 1 && E_NUMBER_ONLY.matches(items[0])) {
            return listOf(ParsedIngredient(text = text, name = items[0]))
        }

        val isVitamins = name.startsWith("vitamin", ignoreCase = true)
        val children = items.flatMap { item ->
            val childText = if (isVitamins && item.length <= 4) "Vitamin $item" else item
            parseItem(childText)
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
     * Splits at [separator] outside parentheses. A comma between digits ("2,5") and a period
     * not followed by a space or the end ("ca.5", "2.5") do not split.
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
                '.' -> next == null || next == ' '
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
        val LIST_DASH = Regex(" - (?!(und|oder|and|or)\b)", RegexOption.IGNORE_CASE)
        val PERCENTAGE = Regex("<?\\s*\\d+(?:[.,]\\d+)?\\s*%")
        val LEADING_LABEL = Regex("^(zutaten|zusammensetzung|ingredients|ingrédients|içindekiler)\\s*:\\s*", RegexOption.IGNORE_CASE)
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
    }
}
