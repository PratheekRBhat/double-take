package com.pratheekbhat.doubletake.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pratheekbhat.doubletake.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncedFileDao {
    @Upsert
    suspend fun insertOrReplace(syncedFile: SyncedFileEntity)

    @Query("SELECT * FROM synced_files WHERE syncPairId = :syncPairId AND relativePath = :relativePath")
    suspend fun getByRelativePath(syncPairId: Long, relativePath: String): SyncedFileEntity?

    @Query("SELECT * FROM synced_files WHERE syncPairId = :syncPairId")
    fun getAllBySyncPair(syncPairId: Long): Flow<List<SyncedFileEntity>>

    @Query("SELECT * FROM synced_files WHERE syncPairId = :syncPairId AND syncStatus = :status")
    fun getAllByStatus(syncPairId: Long, status: SyncStatus): Flow<List<SyncedFileEntity>>

    @Query("DELETE FROM synced_files WHERE syncPairId = :syncPairId AND relativePath = :relativePath")
    suspend fun deleteByRelativePath(syncPairId: Long, relativePath: String)

    @Query("DELETE FROM synced_files WHERE syncPairId = :syncPairId")
    suspend fun deleteAllBySyncPair(syncPairId: Long)
}