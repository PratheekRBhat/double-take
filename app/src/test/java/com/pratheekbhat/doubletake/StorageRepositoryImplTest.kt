package com.pratheekbhat.doubletake

import android.content.ContentResolver
import android.content.Context
import android.content.UriPermission
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.pratheekbhat.doubletake.data.repository.StorageRepositoryImpl

class StorageRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var repo: StorageRepositoryImpl

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        repo = StorageRepositoryImpl(context)
    }

    @Test
    fun `validateUriPermission returns true when read and write granted`() {
        val uri = mockk<Uri>()
        val permission = mockk<UriPermission>()
        every { permission.uri } returns uri
        every { permission.isReadPermission } returns true
        every { permission.isWritePermission } returns true
        every { contentResolver.persistedUriPermissions } returns listOf(permission)

        assertTrue(repo.validateUriPermission(uri))
    }

    @Test
    fun `validateUriPermission returns false when only read granted`() {
        val uri = mockk<Uri>()
        val permission = mockk<UriPermission>()
        every { permission.uri } returns uri
        every { permission.isReadPermission } returns true
        every { permission.isWritePermission } returns false
        every { contentResolver.persistedUriPermissions } returns listOf(permission)

        assertFalse(repo.validateUriPermission(uri))
    }

    @Test
    fun `validateUriPermission returns false when no permissions exist`() {
        val uri = mockk<Uri>()
        every { contentResolver.persistedUriPermissions } returns emptyList()

        assertFalse(repo.validateUriPermission(uri))
    }

    @Test
    fun `validateUriPermission returns false when permission is for different uri`() {
        val uri = mockk<Uri>()
        val otherUri = mockk<Uri>()
        val permission = mockk<UriPermission>()
        every { permission.uri } returns otherUri
        every { permission.isReadPermission } returns true
        every { permission.isWritePermission } returns true
        every { contentResolver.persistedUriPermissions } returns listOf(permission)

        assertFalse(repo.validateUriPermission(uri))
    }
}
