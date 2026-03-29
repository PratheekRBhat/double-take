package com.pratheekbhat.doubletake.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {

    @Insert
    suspend fun insertLog(log: SyncLogEntity)

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_logs WHERE syncPairId = :syncPairId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsBySyncPair(syncPairId: Long, limit: Int = 50): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_logs WHERE action = :action ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByAction(action: String, limit: Int = 50): Flow<List<SyncLogEntity>>

    @Query("SELECT COUNT(*) FROM sync_logs")
    suspend fun getLogCount(): Int

    @Query("DELETE FROM sync_logs WHERE id NOT IN (SELECT id FROM sync_logs ORDER BY timestamp DESC LIMIT :maxCount)")
    suspend fun deleteOldestBeyond(maxCount: Int = 500)
}
