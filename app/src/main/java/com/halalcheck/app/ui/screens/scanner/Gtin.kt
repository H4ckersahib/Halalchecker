package com.halalcheck.app.ui.screens.scanner

/** Validation of manually entered product barcodes (GTIN-8/12/13/14 and UPC-E). */
object Gtin {

    enum class Problem { LENGTH, CHECK_DIGIT }

    /** null if [code] is a valid barcode. */
    fun problem(code: String): Problem? {
        if (code.isEmpty() || !code.all(Char::isDigit) || code.length !in VALID_LENGTHS) return Problem.LENGTH
        val valid = hasValidCheckDigit(code) || (code.length == 8 && isValidUpcE(code))
        return if (valid) null else Problem.CHECK_DIGIT
    }

    /** GS1 mod-10: from the right, digits before the check digit are weighted 3, 1, 3, ... */
    private fun hasValidCheckDigit(code: String): Boolean {
        val digits = code.map { it - '0' }
        val sum = digits.dropLast(1).reversed().withIndex().sumOf { (i, d) -> if (i % 2 == 0) d * 3 else d }
        return (10 - sum % 10) % 10 == digits.last()
    }

    /** UPC-E (8 digits, number system 0 or 1) is checked via its expanded UPC-A form. */
    private fun isValidUpcE(code: String): Boolean {
        if (code[0] != '0' && code[0] != '1') return false
        val d = code.substring(1, 7)
        val body = when (d[5]) {
            '0', '1', '2' -> d.substring(0, 2) + d[5] + "0000" + d.substring(2, 5)
            '3' -> d.substring(0, 3) + "00000" + d.substring(3, 5)
            '4' -> d.substring(0, 4) + "00000" + d[4]
            else -> d.substring(0, 5) + "0000" + d[5]
        }
        return hasValidCheckDigit(code[0] + body + code[7])
    }

    private val VALID_LENGTHS = setOf(8, 12, 13, 14)
}
