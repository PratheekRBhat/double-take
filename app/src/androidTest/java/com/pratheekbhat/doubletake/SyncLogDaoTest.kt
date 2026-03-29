package com.pratheekbhat.doubletake

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pratheekbhat.doubletake.data.local.DoubleTakeDatabase
import com.pratheekbhat.doubletake.data.local.SyncLogDao
import com.pratheekbhat.doubletake.data.local.SyncLogEntity
import com.pratheekbhat.doubletake.data.local.SyncPairDao
import com.pratheekbhat.doubletake.data.local.SyncPairEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncLogDaoTest {

    private lateinit var db: DoubleTakeDatabase
    private lateinit var syncPairDao: SyncPairDao
    private lateinit var dao: SyncLogDao
    private var syncPairId: Long = 0
    private var otherSyncPairId: Long = 0

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DoubleTakeDatabase::class.java
        ).build()
        syncPairDao = db.syncPairDao()
        dao = db.syncLogDao()
        syncPairId = syncPairDao.insert(buildSyncPair("pair-one"))
        otherSyncPairId = syncPairDao.insert(buildSyncPair("pair-two"))
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertLog_getRecentLogs_returnsEntriesInDescendingTimestampOrder() = runTest {
        dao.insertLog(buildLog(syncPairId, timestamp = 1000L))
        dao.insertLog(buildLog(syncPairId, timestamp = 3000L))
        dao.insertLog(buildLog(syncPairId, timestamp = 2000L))

        val result = dao.getRecentLogs().first()

        assertEquals(3, result.size)
        assertTrue(result[0].timestamp >= result[1].timestamp)
        assertTrue(result[1].timestamp >= result[2].timestamp)
        assertEquals(3000L, result[0].timestamp)
        assertEquals(2000L, result[1].timestamp)
        assertEquals(1000L, result[2].timestamp)
    }

    @Test
    fun getRecentLogsBySyncPair_scopesCorrectlyToGivenSyncPairId() = runTest {
        dao.insertLog(buildLog(syncPairId, timestamp = 1000L))
        dao.insertLog(buildLog(syncPairId, timestamp = 2000L))
        dao.insertLog(buildLog(otherSyncPairId, timestamp = 3000L))

        val result = dao.getRecentLogsBySyncPair(syncPairId).first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.syncPairId == syncPairId })
    }

    @Test
    fun getLogsByAction_filtersCorrectlyByActionType() = runTest {
        dao.insertLog(buildLog(syncPairId, action = "UPLOAD"))
        dao.insertLog(buildLog(syncPairId, action = "DOWNLOAD"))
        dao.insertLog(buildLog(syncPairId, action = "UPLOAD"))
        dao.insertLog(buildLog(otherSyncPairId, action = "DOWNLOAD"))

        val uploadLogs = dao.getLogsByAction("UPLOAD").first()
        val downloadLogs = dao.getLogsByAction("DOWNLOAD").first()

        assertEquals(2, uploadLogs.size)
        assertTrue(uploadLogs.all { it.action == "UPLOAD" })
        assertEquals(2, downloadLogs.size)
        assertTrue(downloadLogs.all { it.action == "DOWNLOAD" })
    }

    @Test
    fun deleteOldestBeyond_capsLogsAtSpecifiedCount() = runTest {
        // Insert 5 logs with distinct, ascending timestamps so the keep/drop boundary is deterministic
        for (i in 1..5) {
            dao.insertLog(buildLog(syncPairId, timestamp = i * 1000L))
        }
        assertEquals(5, dao.getLogCount())

        dao.deleteOldestBeyond(maxCount = 3)

        val remaining = dao.getRecentLogs().first()
        assertEquals(3, remaining.size)
        // The three newest should be retained
        assertEquals(5000L, remaining[0].timestamp)
        assertEquals(4000L, remaining[1].timestamp)
        assertEquals(3000L, remaining[2].timestamp)
    }

    @Test
    fun getLogCount_returnsAccurateCount() = runTest {
        assertEquals(0, dao.getLogCount())

        dao.insertLog(buildLog(syncPairId))
        assertEquals(1, dao.getLogCount())

        dao.insertLog(buildLog(syncPairId))
        dao.insertLog(buildLog(otherSyncPairId))
        assertEquals(3, dao.getLogCount())
    }

    private fun buildSyncPair(name: String) = SyncPairEntity(
        localFolderUri = "content://test/$name",
        localFolderName = name,
        driveFolderId = "drive-$name",
        driveFolderName = name,
        createdAt = System.currentTimeMillis()
    )

    private fun buildLog(
        syncPairId: Long,
        timestamp: Long = System.currentTimeMillis(),
        action: String = "UPLOAD",
        filePath: String = "photos/img.jpg",
        result: String = "SUCCESS"
    ) = SyncLogEntity(
        syncPairId = syncPairId,
        timestamp = timestamp,
        action = action,
        filePath = filePath,
        result = result
    )
}
