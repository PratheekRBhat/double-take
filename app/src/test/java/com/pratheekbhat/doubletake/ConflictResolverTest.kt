package com.pratheekbhat.doubletake

import android.net.Uri
import com.pratheekbhat.doubletake.domain.model.LocalFileMetadata
import com.pratheekbhat.doubletake.domain.model.RemoteFileMetadata
import com.pratheekbhat.doubletake.domain.model.SyncAction
import com.pratheekbhat.doubletake.domain.usecase.ConflictResolver
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ConflictResolverTest {

    private lateinit var resolver: ConflictResolver

    @Before
    fun setup() {
        resolver = ConflictResolver()
        resolver.clearLog()
    }

    @Test
    fun `local newer wins produces UPLOAD`() {
        val local = buildLocal(lastModified = 10000L)
        val remote = buildRemote(modifiedTime = 5000L)

        val result = resolver.resolve("file.txt", local, remote)

        assertEquals(SyncAction.UPLOAD, result.action)
        assertNull(result.conflictCopyName)
    }

    @Test
    fun `remote newer wins produces DOWNLOAD`() {
        val local = buildLocal(lastModified = 5000L)
        val remote = buildRemote(modifiedTime = 10000L)

        val result = resolver.resolve("file.txt", local, remote)

        assertEquals(SyncAction.DOWNLOAD, result.action)
        assertNull(result.conflictCopyName)
    }

    @Test
    fun `timestamps within tolerance with content produces CONFLICT`() {
        val local = buildLocal(lastModified = 5000L, mimeType = "text/plain")
        val remote = buildRemote(modifiedTime = 5500L, md5Checksum = "abc123")

        val result = resolver.resolve("file.txt", local, remote)

        assertEquals(SyncAction.CONFLICT, result.action)
        assertNotNull(result.conflictCopyName)
    }

    @Test
    fun `timestamps within tolerance no content produces NO_OP`() {
        val local = buildLocal(lastModified = 5000L, mimeType = null)
        val remote = buildRemote(modifiedTime = 5500L, md5Checksum = "")

        val result = resolver.resolve("file.txt", local, remote)

        assertEquals(SyncAction.NO_OP, result.action)
        assertNull(result.conflictCopyName)
    }

    @Test
    fun `conflict copy filename format with extension`() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val copyName = resolver.buildConflictCopyName("report.pdf")

        assertEquals("report (conflict $today).pdf", copyName)
    }

    @Test
    fun `conflict copy filename format without extension`() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val copyName = resolver.buildConflictCopyName("Makefile")

        assertEquals("Makefile (conflict $today)", copyName)
    }

    @Test
    fun `conflict copy filename with multiple dots`() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val copyName = resolver.buildConflictCopyName("archive.tar.gz")

        assertEquals("archive.tar (conflict $today).gz", copyName)
    }

    @Test
    fun `resolve logs conflict event`() {
        val local = buildLocal(lastModified = 10000L)
        val remote = buildRemote(modifiedTime = 5000L)

        resolver.resolve("file.txt", local, remote)

        assertEquals(1, resolver.conflictLog.size)
        assertEquals("file.txt", resolver.conflictLog[0].relativePath)
        assertEquals("LOCAL_WINS", resolver.conflictLog[0].resolution)
    }

    @Test
    fun `clearLog empties the log`() {
        val local = buildLocal(lastModified = 10000L)
        val remote = buildRemote(modifiedTime = 5000L)

        resolver.resolve("file.txt", local, remote)
        assertEquals(1, resolver.conflictLog.size)

        resolver.clearLog()
        assertTrue(resolver.conflictLog.isEmpty())
    }

    @Test
    fun `exactly at tolerance boundary produces CONFLICT`() {
        val local = buildLocal(lastModified = 7000L)
        val remote = buildRemote(modifiedTime = 5000L)

        val result = resolver.resolve("file.txt", local, remote)

        assertEquals(SyncAction.CONFLICT, result.action)
    }

    // -- Builders --

    private fun buildLocal(
        lastModified: Long = 1000L,
        mimeType: String? = "text/plain"
    ) = LocalFileMetadata(
        uri = mockk(relaxed = true),
        name = "file.txt",
        size = 100L,
        lastModified = lastModified,
        mimeType = mimeType
    )

    private fun buildRemote(
        modifiedTime: Long = 1000L,
        md5Checksum: String = "abc123"
    ) = RemoteFileMetadata(
        driveFileId = "drive-id",
        fileName = "file.txt",
        md5Checksum = md5Checksum,
        modifiedTime = modifiedTime
    )
}
