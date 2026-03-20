package com.pratheekbhat.doubletake

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pratheekbhat.doubletake.data.local.DoubleTakeDatabase
import com.pratheekbhat.doubletake.data.local.SyncPairDao
import com.pratheekbhat.doubletake.data.local.SyncedFileDao
import com.pratheekbhat.doubletake.data.local.SyncedFileEntity
import com.pratheekbhat.doubletake.data.repository.SyncPairRepositoryImpl
import com.pratheekbhat.doubletake.domain.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncPairRepositoryImplTest {

    private lateinit var db: DoubleTakeDatabase
    private lateinit var syncPairDao: SyncPairDao
    private lateinit var syncedFileDao: SyncedFileDao
    private lateinit var repo: SyncPairRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DoubleTakeDatabase::class.java
        ).build()
        syncPairDao = db.syncPairDao()
        syncedFileDao = db.syncedFileDao()
        repo = SyncPairRepositoryImpl(syncPairDao)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun createPair_insertsAndReturnsEntityWithId() = runTest {
        val result = repo.createPair(
            localUri = "content://test/folder",
            localName = "Test Folder",
            driveId = "drive-123",
            driveName = "Drive Folder"
        )

        assertTrue(result.isSuccess)
        val entity = result.getOrNull()
        assertNotNull(entity)
        assertTrue(entity!!.id > 0)
        assertEquals("Test Folder", entity.localFolderName)
        assertEquals("drive-123", entity.driveFolderId)
    }

    @Test
    fun getAllPairs_returnsAllInsertedPairs() = runTest {
        repo.createPair("content://a", "Folder A", "drive-a", "Drive A")
        repo.createPair("content://b", "Folder B", "drive-b", "Drive B")

        val pairs = repo.getAllPairs().first()
        assertEquals(2, pairs.size)
    }

    @Test
    fun getEnabledPairs_onlyReturnsEnabled() = runTest {
        val result = repo.createPair("content://a", "Folder A", "drive-a", "Drive A")
        repo.createPair("content://b", "Folder B", "drive-b", "Drive B")

        val disabledId = result.getOrNull()!!.id
        repo.updateEnabled(disabledId, false)

        val enabled = repo.getEnabledPairs().first()
        assertEquals(1, enabled.size)
        assertEquals("Folder B", enabled[0].localFolderName)
    }

    @Test
    fun deletePair_removesPairFromDatabase() = runTest {
        val result = repo.createPair("content://a", "Folder A", "drive-a", "Drive A")
        val pairId = result.getOrNull()!!.id

        repo.deletePair(pairId)

        val pairs = repo.getAllPairs().first()
        assertTrue(pairs.isEmpty())
    }

    @Test
    fun deletePair_cascadeDeletesAssociatedFiles() = runTest {
        val result = repo.createPair("content://a", "Folder A", "drive-a", "Drive A")
        val pairId = result.getOrNull()!!.id

        syncedFileDao.insertOrReplace(
            SyncedFileEntity(
                syncPairId = pairId,
                fileName = "test.txt",
                relativePath = "test.txt",
                localUri = null,
                driveFileId = null,
                md5Hash = null,
                localModifiedAt = System.currentTimeMillis(),
                remoteModifiedAt = System.currentTimeMillis(),
                lastSyncedAt = null,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )

        repo.deletePair(pairId)

        val files = syncedFileDao.getAllBySyncPair(pairId).first()
        assertTrue(files.isEmpty())
    }
}
