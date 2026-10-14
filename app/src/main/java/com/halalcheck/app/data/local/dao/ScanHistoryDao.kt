package com.halalcheck.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halalcheck.app.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY scanned_at DESC LIMIT :limit")
    fun getRecentScansFlow(limit: Int): Flow<List<ScanHistoryEntity>>

    /** [status] null = all statuses; an empty [query] matches every name. */
    @Query(
        """SELECT * FROM scan_history WHERE (:status IS NULL OR halal_status = :status)
           AND product_name LIKE '%' || :query || '%' ORDER BY scanned_at DESC"""
    )
    fun searchScansFlow(query: String, status: String?): Flow<List<ScanHistoryEntity>>

    /** Replaces an earlier scan of the same barcode (unique index), so it moves to the top. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistoryEntity)

    /** Refreshes the stored result without changing when the product was scanned. */
    @Query(
        """UPDATE scan_history SET product_name = :productName, brand = :brand, image_url = :imageUrl,
           halal_status = :halalStatus WHERE barcode = :barcode"""
    )
    suspend fun updateScan(barcode: String, productName: String, brand: String?, imageUrl: String?, halalStatus: String)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScan(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans()

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getScanCount(): Int

    @Query("SELECT COUNT(*) FROM scan_history WHERE scanned_at >= :since")
    suspend fun getScanCountSince(since: Long): Int
}
