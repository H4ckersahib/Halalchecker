package com.halalcheck.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A tester's report that a result is wrong. One per product; saving again replaces it.
 * Stays on the device until the user exports it (Settings).
 */
@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey
    @ColumnInfo(name = "barcode")
    val barcode: String,

    /** What the tester thinks is right: HALAL, HARAM, MASHBOOH, or null for "don't know". */
    @ColumnInfo(name = "expected_status")
    val expectedStatus: String? = null,

    @ColumnInfo(name = "comment")
    val comment: String = "",

    /** The verdict the app showed when the feedback was given. */
    @ColumnInfo(name = "shown_status")
    val shownStatus: String,

    @ColumnInfo(name = "madhab")
    val madhab: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
