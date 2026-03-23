package com.pratheekbhat.doubletake.domain.usecase

import androidx.core.net.toUri
import com.pratheekbhat.doubletake.domain.model.SyncResult
import com.pratheekbhat.doubletake.domain.repository.AuthRepository
import com.pratheekbhat.doubletake.domain.repository.StorageRepository
import com.pratheekbhat.doubletake.domain.repository.SyncPairRepository
import com.pratheekbhat.doubletake.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PerformSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val syncPairRepository: SyncPairRepository,
    private val storageRepository: StorageRepository
) {

    suspend operator fun invoke(syncPairId: Long): Result<SyncResult> {
        if (!authRepository.isSignedIn.first()) {
            return Result.failure(Exception("User is not signed in"))
        }

        val pairs = syncPairRepository.getAllPairs().first()
        val pair = pairs.find { it.id == syncPairId } ?: return Result.failure(Exception("Sync pair not found: $syncPairId"))

        if (!storageRepository.validateUriPermission(pair.localFolderUri.toUri())) {
            return Result.failure(Exception("Folder permission lost for: ${pair.localFolderName}"))
        }

        return syncRepository.performSync(syncPairId)
    }

    suspend fun invokeAll(): Result<Map<Long, SyncResult>> {
        if (!authRepository.isSignedIn.first()) {
            return Result.failure(Exception("User is not signed in"))
        }
        return syncRepository.performSyncAll()
    }
}