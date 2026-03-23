package com.pratheekbhat.doubletake

import android.net.Uri
import com.pratheekbhat.doubletake.data.local.SyncedFileEntity
import com.pratheekbhat.doubletake.domain.model.LocalFileMetadata
import com.pratheekbhat.doubletake.domain.model.RemoteFileMetadata
import com.pratheekbhat.doubletake.domain.model.SyncAction
import com.pratheekbhat.doubletake.domain.model.SyncStatus
import com.pratheekbhat.doubletake.domain.usecase.SyncDiffer
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncDifferTest {

    private lateinit var differ: SyncDiffer

    @Before
    fun setup() {
        differ = SyncDiffer()
    }

    // -- 8 diff cases --

    @Test
    fun `new local file only produces UPLOAD`() {
        val local = mapOf("file.txt" to buildLocal("file.txt"))
        val result = differ.diff(local, emptyMap(), emptyMap())

        assertEquals(1, result.size)
        assertEquals(SyncAction.UPLOAD, result[0].action)
    }

    @Test
    fun `new remote file only produces DOWNLOAD`() {
        val remote = mapOf("file.txt" to buildRemote("file.txt"))
        val result = differ.diff(emptyMap(), remote, emptyMap())

        assertEquals(1, result.size)
        assertEquals(SyncAction.DOWNLOAD, result[0].action)
    }

    @Test
    fun `all three present local modified produces UPLOAD`() {
        val local = mapOf("file.txt" to buildLocal("file.txt", lastModified = 2000L))
        val remote = mapOf("file.txt" to buildRemote("file.txt", modifiedTime = 1000L))
        val db = mapOf("file.txt" to buildDb("file.txt", localModifiedAt = 1000L, remoteModifiedAt = 1000L))

        val result = differ.diff(local, remote, db)

        assertEquals(1, result.size)
        assertEquals(SyncAction.UPLOAD, result[0].action)
    }

    @Test
    fun `all three present remote modified produces DOWNLOAD`() {
        val local = mapOf("file.txt" to buildLocal("file.txt", lastModified = 1000L))
        val remote = mapOf("file.txt" to buildRemote("file.txt", modifiedTime = 2000L))
        val db = mapOf("file.txt" to buildDb("file.txt", localModifiedAt = 1000L, remoteModifiedAt = 1000L))

        val result = differ.diff(local, remote, db)

        assertEquals(1, result.size)
        assertEquals(SyncAction.DOWNLOAD, result[0].action)
    }

    @Test
    fun `all three present both modified produces CONFLICT`() {
        val local = mapOf("file.txt" to buildLocal("file.txt", lastModified = 3000L))
        val remote = mapOf("file.txt" to buildRemote("file.txt", modifiedTime = 3000L))
        val db = mapOf("file.txt" to buildDb("file.txt", localModifiedAt = 1000L, remoteModifiedAt = 1000L))

        val result = differ.diff(local, remote, db)

        assertEquals(1, result.size)
        assertEquals(SyncAction.CONFLICT, result[0].action)
    }

    @Test
    fun `local and DB present remote missing produces TRASH_LOCAL`() {
        val local = mapOf("file.txt" to buildLocal("file.txt"))
        val db = mapOf("file.txt" to buildDb("file.txt"))

        val result = differ.diff(local, emptyMap(), db)

        assertEquals(1, result.size)
        assertEquals(SyncAction.TRASH_LOCAL, result[0].action)
    }

    @Test
    fun `remote and DB present local missing produces TRASH_REMOTE`() {
        val remote = mapOf("file.txt" to buildRemote("file.txt"))
        val db = mapOf("file.txt" to buildDb("file.txt"))

        val result = differ.diff(emptyMap(), remote, db)

        assertEquals(1, result.size)
        assertEquals(SyncAction.TRASH_REMOTE, result[0].action)
    }

    @Test
    fun `DB only produces CLEANUP_DB`() {
        val db = mapOf("file.txt" to buildDb("file.txt"))

        val result = differ.diff(emptyMap(), emptyMap(), db)

        assertEquals(1, result.size)
        assertEquals(SyncAction.CLEANUP_DB, result[0].action)
    }

    // -- Edge cases --

    @Test
    fun `empty maps produce empty result`() {
        val result = differ.diff(emptyMap(), emptyMap(), emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `unchanged file produces NO_OP`() {
        val local = mapOf("file.txt" to buildLocal("file.txt", lastModified = 1000L))
        val remote = mapOf("file.txt" to buildRemote("file.txt", modifiedTime = 1000L))
        val db = mapOf("file.txt" to buildDb("file.txt", localModifiedAt = 1000L, remoteModifiedAt = 1000L))

        val result = differ.diff(local, remote, db)

        assertEquals(1, result.size)
        assertEquals(SyncAction.NO_OP, result[0].action)
    }

    @Test
    fun `new on both sides with different timestamps produces UPLOAD when local newer`() {
        val local = mapOf("file.txt" to buildLocal("file.txt", lastModified = 5000L))
        val remote = mapOf("file.txt" to buildRemote("file.txt", modifiedTime = 1000L))

        val result = differ.diff(local, remote, emptyMap())

        assertEquals(1, result.size)
        assertEquals(SyncAction.UPLOAD, result[0].action)
    }

    @Test
    fun `multiple files each get correct action`() {
        val local = mapOf(
            "new.txt" to buildLocal("new.txt"),
            "existing.txt" to buildLocal("existing.txt", lastModified = 1000L)
        )
        val remote = mapOf(
            "remote-new.txt" to buildRemote("remote-new.txt"),
            "existing.txt" to buildRemote("existing.txt", modifiedTime = 1000L)
        )
        val db = mapOf(
            "existing.txt" to buildDb("existing.txt", localModifiedAt = 1000L, remoteModifiedAt = 1000L),
            "deleted.txt" to buildDb("deleted.txt")
        )

        val result = differ.diff(local, remote, db)
        val actionMap = result.associateBy { it.relativePath }

        assertEquals(SyncAction.UPLOAD, actionMap["new.txt"]?.action)
        assertEquals(SyncAction.DOWNLOAD, actionMap["remote-new.txt"]?.action)
        assertEquals(SyncAction.NO_OP, actionMap["existing.txt"]?.action)
        assertEquals(SyncAction.CLEANUP_DB, actionMap["deleted.txt"]?.action)
    }

    // -- Builders --

    private fun buildLocal(
        name: String,
        lastModified: Long = 1000L
    ) = LocalFileMetadata(
        uri = mockk(relaxed = true),
        name = name,
        size = 100L,
        lastModified = lastModified,
        mimeType = "text/plain"
    )

    private fun buildRemote(
        name: String,
        modifiedTime: Long = 1000L
    ) = RemoteFileMetadata(
        driveFileId = "drive-id-$name",
        fileName = name,
        md5Checksum = "abc123",
        modifiedTime = modifiedTime
    )

    private fun buildDb(
        relativePath: String,
        localModifiedAt: Long = 1000L,
        remoteModifiedAt: Long = 1000L
    ) = SyncedFileEntity(
        syncPairId = 1L,
        fileName = relativePath,
        relativePath = relativePath,
        localUri = "content://test/$relativePath",
        driveFileId = "drive-id-$relativePath",
        md5Hash = "abc123",
        localModifiedAt = localModifiedAt,
        remoteModifiedAt = remoteModifiedAt,
        lastSyncedAt = 1000L,
        syncStatus = SyncStatus.SYNCED
    )
}
