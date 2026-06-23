package com.halalcheck.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halalcheck.app.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY scanned_at DESC")
    fun getAllScansFlow(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY scanned_at DESC LIMIT :limit")
    fun getRecentScansFlow(limit: Int): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE halal_status = :status ORDER BY scanned_at DESC")
    fun getScansByStatusFlow(status: String): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE product_name LIKE '%' || :query || '%' ORDER BY scanned_at DESC")
    fun searchScansFlow(query: String): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistoryEntity)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScan(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans()

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getScanCount(): Int

    @Query("SELECT COUNT(*) FROM scan_history WHERE scanned_at >= :since")
    suspend fun getScanCountSince(since: Long): Int
}
