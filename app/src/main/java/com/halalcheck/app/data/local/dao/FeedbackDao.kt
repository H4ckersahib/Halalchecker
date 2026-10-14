package com.halalcheck.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halalcheck.app.data.local.entity.FeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {

    @Query("SELECT * FROM feedback WHERE barcode = :barcode")
    suspend fun getFeedback(barcode: String): FeedbackEntity?

    @Query("SELECT * FROM feedback ORDER BY created_at")
    suspend fun getAllFeedback(): List<FeedbackEntity>

    @Query("SELECT COUNT(*) FROM feedback")
    fun countFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFeedback(feedback: FeedbackEntity)

    @Query("DELETE FROM feedback WHERE barcode = :barcode")
    suspend fun deleteFeedback(barcode: String)
}
