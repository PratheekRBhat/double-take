package com.pratheekbhat.doubletake

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pratheekbhat.doubletake.data.local.DoubleTakeDatabase
import com.pratheekbhat.doubletake.data.local.SyncPairDao
import com.pratheekbhat.doubletake.data.local.SyncPairEntity
import com.pratheekbhat.doubletake.data.local.SyncedFileDao
import com.pratheekbhat.doubletake.data.local.SyncedFileEntity
import com.pratheekbhat.doubletake.domain.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncedFileDaoTest {

    private lateinit var db: DoubleTakeDatabase
    private lateinit var syncPairDao: SyncPairDao
    private lateinit var dao: SyncedFileDao
    private var syncPairId: Long = 0
    private var otherSyncPairId: Long = 0

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DoubleTakeDatabase::class.java
        ).build()
        syncPairDao = db.syncPairDao()
        dao = db.syncedFileDao()
        syncPairId = syncPairDao.insert(buildSyncPair("pair-one"))
        otherSyncPairId = syncPairDao.insert(buildSyncPair("pair-two"))
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertOrReplace_insertsNewFile() = runTest {
        dao.insertOrReplace(buildFile(syncPairId, "photos/img.jpg"))
        val result = dao.getByRelativePath(syncPairId, "photos/img.jpg")
        assertNotNull(result)
        assertEquals("photos/img.jpg", result?.relativePath)
    }

    @Test
    fun insertOrReplace_replacesExistingFileAtSamePath() = runTest {
        dao.insertOrReplace(buildFile(syncPairId, "photos/img.jpg", SyncStatus.PENDING_UPLOAD))
        dao.insertOrReplace(buildFile(syncPairId, "photos/img.jpg", SyncStatus.SYNCED))
        val result = dao.getByRelativePath(syncPairId, "photos/img.jpg")
        assertEquals(SyncStatus.SYNCED, result?.syncStatus)
        assertEquals(1, dao.getAllBySyncPair(syncPairId).first().size)
    }

    @Test
    fun getAllByStatus_filtersCorrectly() = runTest {
        dao.insertOrReplace(buildFile(syncPairId, "a.jpg",
            SyncStatus.PENDING_UPLOAD))
        dao.insertOrReplace(buildFile(syncPairId, "b.jpg",
            SyncStatus.SYNCED))
        dao.insertOrReplace(buildFile(syncPairId, "c.jpg",
            SyncStatus.PENDING_UPLOAD))
        val result = dao.getAllByStatus(syncPairId,
            SyncStatus.PENDING_UPLOAD).first()
        assertEquals(2, result.size)
    }

    @Test
    fun deleteByRelativePath_removesCorrectFile() = runTest {
        dao.insertOrReplace(buildFile(syncPairId, "photos/img.jpg"))
        dao.insertOrReplace(buildFile(syncPairId, "photos/other.jpg"))
        dao.deleteByRelativePath(syncPairId, "photos/img.jpg")
        assertNull(dao.getByRelativePath(syncPairId, "photos/img.jpg"))
        assertNotNull(dao.getByRelativePath(syncPairId,
            "photos/other.jpg"))
    }

    @Test
    fun filesAreIsolatedBetweenSyncPairs() = runTest {
        dao.insertOrReplace(buildFile(syncPairId, "photos/img.jpg"))
        dao.insertOrReplace(buildFile(otherSyncPairId, "photos/img.jpg"))
        assertEquals(1, dao.getAllBySyncPair(syncPairId).first().size)
        assertEquals(1,
            dao.getAllBySyncPair(otherSyncPairId).first().size)
        assertNotNull(dao.getByRelativePath(syncPairId, "photos/img.jpg"))
        assertNotNull(dao.getByRelativePath(otherSyncPairId,
            "photos/img.jpg"))
    }

    private fun buildSyncPair(name: String) = SyncPairEntity(
        localFolderUri = "content://test/$name",
        localFolderName = name,
        driveFolderId = "drive-$name",
        driveFolderName = name,
        createdAt = System.currentTimeMillis()
    )

    private fun buildFile(
        syncPairId: Long,
        relativePath: String,
        status: SyncStatus = SyncStatus.PENDING_UPLOAD
    ) = SyncedFileEntity(
        syncPairId = syncPairId,
        fileName = relativePath.substringAfterLast("/"),
        relativePath = relativePath,
        localUri = null,
        driveFileId = null,
        md5Hash = null,
        localModifiedAt = System.currentTimeMillis(),
        remoteModifiedAt = System.currentTimeMillis(),
        lastSyncedAt = null,
        syncStatus = status
    )
}