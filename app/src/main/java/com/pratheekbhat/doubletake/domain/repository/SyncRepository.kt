package com.pratheekbhat.doubletake.domain.repository

import com.pratheekbhat.doubletake.domain.model.SyncResult

interface SyncRepository {
    suspend fun performSync(syncPairId: Long): Result<SyncResult>
    suspend fun performSyncAll(): Result<Map<Long, SyncResult>>
}