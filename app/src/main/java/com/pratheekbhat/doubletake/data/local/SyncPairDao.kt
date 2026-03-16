package com.pratheekbhat.doubletake.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncPairDao {

    @Insert
    suspend fun insert(syncPair: SyncPairEntity): Long

    @Update
    suspend fun update(syncPair: SyncPairEntity)

    @Delete
    suspend fun delete(syncPair: SyncPairEntity)

    @Query("SELECT * FROM sync_pairs WHERE id = :id")
    suspend fun getById(id: Long): SyncPairEntity?

    @Query("SELECT * FROM sync_pairs")
    fun getAll(): Flow<List<SyncPairEntity>>

    @Query("SELECT * FROM sync_pairs WHERE isEnabled = 1")
    fun getAllEnabled(): Flow<List<SyncPairEntity>>

    @Query("UPDATE sync_pairs SET lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateLastSyncedAt(id: Long, timestamp: Long)
}