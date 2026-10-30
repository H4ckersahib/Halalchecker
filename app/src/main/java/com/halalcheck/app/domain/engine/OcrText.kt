package com.halalcheck.app.domain.engine

/**
 * Turns the text recognized on a photo into one ingredient text for [IngredientTextParser].
 * Kept free of ML Kit types so it runs in JVM tests.
 */
object OcrText {

    private val KEEP_HYPHEN_BEFORE = setOf("und", "oder", "bzw", "sowie", "and", "or")

    /**
     * [blocks] are the recognized text blocks, each a list of lines. The lines of a block become one
     * line; a word broken at the line end ("Natri-" / "umcyclamat") is joined, "Mono-" / "und" keeps
     * its hyphen. Blocks become sentences, so the parser's note rules can drop a nutrition table or an
     * address next to the list.
     */
    fun join(blocks: List<List<String>>): String =
        blocks.map(::joinLines)
            .map { it.trimEnd('.', ' ') }
            .filter { it.isNotBlank() }
            .joinToString(". ")

    private fun joinLines(lines: List<String>): String {
        val text = StringBuilder()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (text.isNotEmpty()) {
                val firstWord = line.takeWhile { it.isLetter() }.lowercase()
                val brokenWord = text.endsWith("-") && text.length >= 2 && text[text.length - 2].isLetter() &&
                    line.first().isLowerCase() && firstWord !in KEEP_HYPHEN_BEFORE
                if (brokenWord) text.setLength(text.length - 1) else text.append(' ')
            }
            text.append(line)
        }
        return text.toString()
    }
}
