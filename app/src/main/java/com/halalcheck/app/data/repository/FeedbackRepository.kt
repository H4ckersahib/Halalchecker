package com.halalcheck.app.data.repository

import com.halalcheck.app.data.local.dao.FeedbackDao
import com.halalcheck.app.data.local.entity.FeedbackEntity
import com.halalcheck.app.domain.model.HalalStatus
import com.halalcheck.app.domain.model.Madhab
import com.halalcheck.app.domain.model.ProductFeedback
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Testers' reports of wrong results, stored on the device until exported. */
@Singleton
class FeedbackRepository @Inject constructor(
    private val feedbackDao: FeedbackDao
) {
    suspend fun getFeedback(barcode: String): ProductFeedback? = feedbackDao.getFeedback(barcode)?.toDomain()

    suspend fun getAllFeedback(): List<ProductFeedback> = feedbackDao.getAllFeedback().map { it.toDomain() }

    val feedbackCount: Flow<Int> = feedbackDao.countFlow()

    suspend fun saveFeedback(feedback: ProductFeedback) {
        feedbackDao.saveFeedback(
            FeedbackEntity(
                barcode = feedback.barcode,
                expectedStatus = feedback.expectedStatus?.name,
                comment = feedback.comment.trim(),
                shownStatus = feedback.shownStatus.name,
                madhab = feedback.madhab.name,
                createdAt = feedback.createdAt
            )
        )
    }

    suspend fun deleteFeedback(barcode: String) = feedbackDao.deleteFeedback(barcode)
}

private fun FeedbackEntity.toDomain() = ProductFeedback(
    barcode = barcode,
    expectedStatus = expectedStatus?.let { HalalStatus.fromString(it) },
    comment = comment,
    shownStatus = HalalStatus.fromString(shownStatus),
    madhab = Madhab.fromString(madhab),
    createdAt = createdAt
)
