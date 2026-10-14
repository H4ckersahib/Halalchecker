package com.halalcheck.app.domain.model

/** A tester's report that the result for a product is wrong. */
data class ProductFeedback(
    val barcode: String,
    /** What the tester thinks is right; null = "don't know, but this looks wrong". */
    val expectedStatus: HalalStatus?,
    val comment: String,
    /** The verdict the app showed when the feedback was given. */
    val shownStatus: HalalStatus,
    val madhab: Madhab,
    val createdAt: Long = System.currentTimeMillis()
)
