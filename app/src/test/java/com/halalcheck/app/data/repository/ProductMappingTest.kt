package com.halalcheck.app.data.repository

import com.halalcheck.app.data.remote.OpenFoodFactsIngredient
import com.halalcheck.app.data.remote.OpenFoodFactsProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductMappingTest {

    @Test
    fun `OFF ingredient nodes without text are kept when they have an id (REG-13)`() {
        val product = OpenFoodFactsProduct(
            ingredients = listOf(
                OpenFoodFactsIngredient(id = "en:pork", text = null),
                OpenFoodFactsIngredient(id = null, text = "  ")
            )
        ).toDomain(barcode = "1", language = "de")
        assertEquals(listOf("en:pork"), product.sourceIngredients.map { it.id })
        assertEquals("pork", product.sourceIngredients.single().text)
    }

    @Test
    fun `blank texts fall back to other languages`() {
        val product = OpenFoodFactsProduct(ingredientsTextDe = "", ingredientsText = "Zucker, Salz").toDomain("1", "de")
        assertEquals("Zucker, Salz", product.ingredientsText)
    }
}
