package com.pratheekbhat.doubletake

import android.net.Uri
import com.pratheekbhat.doubletake.data.local.SyncPairDao
import com.pratheekbhat.doubletake.data.local.SyncPairEntity
import com.pratheekbhat.doubletake.data.local.SyncedFileDao
import com.pratheekbhat.doubletake.data.local.SyncedFileEntity
import com.pratheekbhat.doubletake.data.remote.DriveServiceClient
import com.pratheekbhat.doubletake.data.repository.SyncRepositoryImpl
import com.pratheekbhat.doubletake.domain.model.LocalFileMetadata
import com.pratheekbhat.doubletake.domain.model.RemoteFileMetadata
import com.pratheekbhat.doubletake.domain.model.SyncAction
import com.pratheekbhat.doubletake.domain.model.SyncActionItem
import com.pratheekbhat.doubletake.domain.model.SyncStatus
import com.pratheekbhat.doubletake.domain.repository.StorageRepository
import com.pratheekbhat.doubletake.domain.usecase.ConflictResolver
import com.pratheekbhat.doubletake.domain.usecase.SyncDiffer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class SyncRepositoryImplTest {

    private lateinit var syncPairDao: SyncPairDao
    private lateinit var syncedFileDao: SyncedFileDao
    private lateinit var driveServiceClient: DriveServiceClient
    private lateinit var storageRepository: StorageRepository
    private lateinit var syncDiffer: SyncDiffer
    private lateinit var conflictResolver: ConflictResolver
    private lateinit var repository: SyncRepositoryImpl

    private val testPair = SyncPairEntity(
        id = 1L,
        localFolderUri = "content://test/folder",
        localFolderName = "Test Folder",
        driveFolderId = "drive-folder-id",
        driveFolderName = "Drive Folder",
        createdAt = 1000L,
        isEnabled = true
    )

    @Before
    fun setup() {
        syncPairDao = mockk(relaxed = true)
        syncedFileDao = mockk(relaxed = true)
        driveServiceClient = mockk(relaxed = true)
        storageRepository = mockk(relaxed = true)
        syncDiffer = mockk(relaxed = true)
        conflictResolver = mockk(relaxed = true)

        repository = SyncRepositoryImpl(
            syncPairDao, syncedFileDao, driveServiceClient,
            storageRepository, syncDiffer, conflictResolver
        )
    }

    @Test
    fun `performSync returns failure when pair not found`() = runTest {
        coEvery { syncPairDao.getById(99L) } returns null

        val result = repository.performSync(99L)

        assertTrue(result.isFailure)
        assertEquals("SyncPair not found: 99", result.exceptionOrNull()?.message)
    }

    @Test
    fun `performSync with no actions returns zero counts`() = runTest {
        setupBasicMocks(actions = emptyList())

        val result = repository.performSync(1L)

        assertTrue(result.isSuccess)
        val syncResult = result.getOrThrow()
        assertEquals(0, syncResult.uploaded)
        assertEquals(0, syncResult.downloaded)
        assertEquals(0, syncResult.failures)
    }

    @Test
    fun `performSync with upload action increments uploaded count`() = runTest {
        val localFile = buildLocal("file.txt")
        val remoteResult = buildRemote("file.txt")

        setupBasicMocks(
            actions = listOf(
                SyncActionItem("file.txt", SyncAction.UPLOAD, localFile, null, null)
            )
        )
        every { driveServiceClient.uploadFile(any(), any(), any()) } returns Result.success(remoteResult)

        val result = repository.performSync(1L)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().uploaded)
    }

    @Test
    fun `performSync with download action increments downloaded count`() = runTest {
        val remoteFile = buildRemote("file.txt")
        val inputStream = ByteArrayInputStream("content".toByteArray())

        setupBasicMocks(
            actions = listOf(
                SyncActionItem("file.txt", SyncAction.DOWNLOAD, null, remoteFile, null)
            )
        )
        every { driveServiceClient.downloadFile(any()) } returns Result.success(inputStream)
        every { storageRepository.writeFile(any(), any(), any()) } returns Result.success(mockk())
        every { storageRepository.listLocalFiles(any()) } returns Result.success(listOf(buildLocal("file.txt")))

        val result = repository.performSync(1L)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().downloaded)
    }

    @Test
    fun `performSync individual file failure does not block others`() = runTest {
        val localFile1 = buildLocal("fail.txt")
        val localFile2 = buildLocal("success.txt")
        val remoteResult = buildRemote("success.txt")

        setupBasicMocks(
            actions = listOf(
                SyncActionItem("fail.txt", SyncAction.UPLOAD, localFile1, null, null),
                SyncActionItem("success.txt", SyncAction.UPLOAD, localFile2, null, null)
            )
        )
        every { driveServiceClient.uploadFile(any(), any(), any()) } returns
                Result.failure(RuntimeException("Network error")) andThen
                Result.success(remoteResult)

        val result = repository.performSync(1L)

        assertTrue(result.isSuccess)
        val syncResult = result.getOrThrow()
        assertEquals(1, syncResult.uploaded)
        assertEquals(1, syncResult.failures)
    }

    @Test
    fun `performSync updates lastSyncedAt after completion`() = runTest {
        setupBasicMocks(actions = emptyList())

        repository.performSync(1L)

        coVerify { syncPairDao.updateLastSyncedAt(1L, any()) }
    }

    @Test
    fun `performSync with CLEANUP_DB removes db record`() = runTest {
        val dbRecord = buildDb("deleted.txt")

        setupBasicMocks(
            actions = listOf(
                SyncActionItem("deleted.txt", SyncAction.CLEANUP_DB, null, null, dbRecord)
            )
        )

        repository.performSync(1L)

        coVerify { syncedFileDao.deleteByRelativePath(1L, "deleted.txt") }
    }

    @Test
    fun `performSyncAll iterates all enabled pairs`() = runTest {
        val pair2 = testPair.copy(id = 2L)
        coEvery { syncPairDao.getAllEnabled() } returns flowOf(listOf(testPair, pair2))
        coEvery { syncPairDao.getById(1L) } returns testPair
        coEvery { syncPairDao.getById(2L) } returns pair2
        every { storageRepository.listLocalFiles(any()) } returns Result.success(emptyList())
        every { driveServiceClient.listRemoteFiles(any()) } returns Result.success(emptyList())
        coEvery { syncedFileDao.getAllBySyncPair(any()) } returns flowOf(emptyList())
        every { syncDiffer.diff(any(), any(), any()) } returns emptyList()

        val result = repository.performSyncAll()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }

    // -- Helpers --

    private fun setupBasicMocks(actions: List<SyncActionItem>) {
        coEvery { syncPairDao.getById(1L) } returns testPair
        every { storageRepository.listLocalFiles(any()) } returns Result.success(emptyList())
        every { driveServiceClient.listRemoteFiles(any()) } returns Result.success(emptyList())
        coEvery { syncedFileDao.getAllBySyncPair(1L) } returns flowOf(emptyList())
        every { syncDiffer.diff(any(), any(), any()) } returns actions
    }

    private fun buildLocal(name: String) = LocalFileMetadata(
        uri = mockk(relaxed = true),
        name = name,
        size = 100L,
        lastModified = 1000L,
        mimeType = "text/plain"
    )

    private fun buildRemote(name: String) = RemoteFileMetadata(
        driveFileId = "drive-id-$name",
        fileName = name,
        md5Checksum = "abc123",
        modifiedTime = 1000L
    )

    private fun buildDb(relativePath: String) = SyncedFileEntity(
        syncPairId = 1L,
        fileName = relativePath,
        relativePath = relativePath,
        localUri = "content://test/$relativePath",
        driveFileId = "drive-id-$relativePath",
        md5Hash = "abc123",
        localModifiedAt = 1000L,
        remoteModifiedAt = 1000L,
        lastSyncedAt = 1000L,
        syncStatus = SyncStatus.SYNCED
    )
}
