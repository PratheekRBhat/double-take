package com.pratheekbhat.doubletake.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncWorkManager @Inject constructor(
    private val workManager: WorkManager
) {

    companion object {
        const val PERIODIC_SYNC_TAG = "periodic_sync"
        const val MANUAL_SYNC_TAG = "manual_sync"
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(PERIODIC_SYNC_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun triggerImmediateSync(syncPairId: Long? = null) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val builder = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(MANUAL_SYNC_TAG)

        if (syncPairId != null) {
            builder.setInputData(workDataOf(SyncWorker.KEY_SYNC_PAIR_ID to syncPairId))
        }

        workManager.enqueue(builder.build())
    }

    fun cancelAllSync() {
        workManager.cancelAllWorkByTag(PERIODIC_SYNC_TAG)
        workManager.cancelAllWorkByTag(MANUAL_SYNC_TAG)
    }
}