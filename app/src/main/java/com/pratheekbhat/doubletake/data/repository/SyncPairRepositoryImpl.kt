package com.pratheekbhat.doubletake.data.repository

import com.pratheekbhat.doubletake.data.local.SyncPairDao
import com.pratheekbhat.doubletake.data.local.SyncPairEntity
import com.pratheekbhat.doubletake.domain.repository.SyncPairRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPairRepositoryImpl @Inject constructor(
    private val syncPairDao: SyncPairDao
) : SyncPairRepository {

    override suspend fun createPair(
        localUri: String,
        localName: String,
        driveId: String,
        driveName: String
    ): Result<SyncPairEntity> {
        return try {
            val entity = SyncPairEntity(
                localFolderUri = localUri,
                localFolderName = localName,
                driveFolderId = driveId,
                driveFolderName = driveName,
                createdAt = System.currentTimeMillis()
            )
            val id = syncPairDao.insert(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllPairs(): Flow<List<SyncPairEntity>> = syncPairDao.getAll()

    override fun getEnabledPairs(): Flow<List<SyncPairEntity>> = syncPairDao.getAllEnabled()

    override suspend fun deletePair(pairId: Long) = syncPairDao.deleteByPairId(pairId)

    override suspend fun updateEnabled(pairId: Long, enabled: Boolean) = syncPairDao.updateEnabled(pairId, enabled)
}