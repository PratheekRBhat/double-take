package com.pratheekbhat.doubletake.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pratheekbhat.doubletake.domain.usecase.PerformSyncUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val performSyncUseCase: PerformSyncUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SYNC_PAIR_ID = "sync_pair_id"
    }

    override suspend fun doWork(): Result {
        val syncPairId = inputData.getLong(KEY_SYNC_PAIR_ID, -1L)

        val result = if (syncPairId != -1L) {
            performSyncUseCase(syncPairId)
        } else {
            performSyncUseCase.invokeAll()
        }

        return when {
            result.isSuccess -> Result.success()
            else -> {
                val error = result.exceptionOrNull()
                if (error?.message?.contains("not signed in") == true) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        }
    }
}