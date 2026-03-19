package com.pratheekbhat.doubletake.domain.repository

import com.pratheekbhat.doubletake.data.local.SyncPairEntity
import kotlinx.coroutines.flow.Flow

interface SynPairRepository {
    suspend fun createPair(localUri: String, localName: String, driveId: String, driveName: String): Result<SyncPairEntity>
    fun getAllPairs(): Flow<List<SyncPairEntity>>
    fun getEnabledPairs(): Flow<List<SyncPairEntity>>
    suspend fun deletePair(pairId: Long)
    suspend fun updateEnabled(pairId: Long, enabled: Boolean)
}