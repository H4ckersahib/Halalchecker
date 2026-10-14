package com.halalcheck.app.ui.screens.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GtinTest {

    @Test
    fun `real barcodes are valid`() {
        listOf(
            "4000522427909", // Pom-Bär, EAN-13
            "3017620422003", // Nutella, EAN-13
            "20724696", // EAN-8
            "036000291452", // UPC-A
            "04252614", // UPC-E
            "10036000291459" // GTIN-14
        ).forEach { assertNull(it, Gtin.problem(it)) }
    }

    @Test
    fun `wrong check digit and wrong length are told apart`() {
        assertEquals(Gtin.Problem.CHECK_DIGIT, Gtin.problem("4000522427908"))
        assertEquals(Gtin.Problem.LENGTH, Gtin.problem("400052242"))
        assertEquals(Gtin.Problem.LENGTH, Gtin.problem(""))
    }
}
