package com.pratheekbhat.doubletake

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pratheekbhat.doubletake.data.local.DoubleTakeDatabase
import com.pratheekbhat.doubletake.data.local.SyncPairDao
import com.pratheekbhat.doubletake.data.local.SyncPairEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncPairDaoTest {

    private lateinit var db: DoubleTakeDatabase
    private lateinit var dao: SyncPairDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DoubleTakeDatabase::class.java
        ).build()
        dao = db.syncPairDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetAll() = runTest {
        val pair = buildSyncPair()
        dao.insert(pair)

        val result = dao.getAll().first()
        assertEquals(1, result.size)
        assertEquals(pair.localFolderName, result[0].localFolderName)
    }

    @Test
    fun getAllEnabled_onlyReturnsEnabledPairs() = runTest {
        dao.insert(buildSyncPair(isEnabled = true))
        dao.insert(buildSyncPair(localFolderName = "disabled-folder", isEnabled = false))
        val result = dao.getAllEnabled().first()

        assertEquals(1, result.size)
        assertTrue(result[0].isEnabled)
    }

    @Test
    fun delete_removesPairFromDb() = runTest {
        val id = dao.insert(buildSyncPair())
        val inserted = dao.getById(id)!!
        dao.delete(inserted)
        assertNull(dao.getById(id))
    }

    @Test
    fun updateLastSyncedAt_updatesTimestamp() = runTest {
        val id = dao.insert(buildSyncPair())
        val timestamp = 1234567890L
        dao.updateLastSyncedAt(id, timestamp)
        val result = dao.getById(id)
        assertEquals(timestamp, result?.lastSyncedAt)
    }

    private fun buildSyncPair(
        localFolderName: String = "test-folder",
        isEnabled: Boolean = true
    ) = SyncPairEntity(
        localFolderUri = "content://test/folder",
        localFolderName = localFolderName,
        driveFolderId = "drive-folder-id",
        driveFolderName = "Drive Folder",
        createdAt = System.currentTimeMillis(),
        isEnabled = isEnabled
    )
}