package com.pratheekbhat.doubletake

import com.pratheekbhat.doubletake.data.local.AuthDataStore
import com.pratheekbhat.doubletake.data.repository.AuthRepositoryImpl
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private lateinit var authDataStore: AuthDataStore
    private lateinit var credential: GoogleAccountCredential
    private lateinit var repo: AuthRepositoryImpl

    @Before
    fun setup() {
        authDataStore = mockk(relaxed = true)
        credential = mockk(relaxed = true)
        every { authDataStore.isSignedIn } returns flowOf(false)
        repo = AuthRepositoryImpl(authDataStore, credential)
    }

    @Test
    fun `isSignedIn emits value from datastore`() = runTest {
        every { authDataStore.isSignedIn } returns flowOf(true)
        val repo = AuthRepositoryImpl(authDataStore, credential)

        val result = repo.isSignedIn.first()
        assertTrue(result)
    }

    @Test
    fun `isSignedIn emits false when not signed in`() = runTest {
        val result = repo.isSignedIn.first()
        assertFalse(result)
    }

    @Test
    fun `signOut clears credential and datastore`() = runTest {
        coEvery { authDataStore.clearAuthState() } returns Unit

        val result = repo.signOut()

        assertTrue(result.isSuccess)
        verify { credential.selectedAccountName = null }
        coVerify { authDataStore.clearAuthState() }
    }

    @Test
    fun `signOut returns failure on exception`() = runTest {
        coEvery { authDataStore.clearAuthState() } throws RuntimeException("DataStore error")

        val result = repo.signOut()

        assertTrue(result.isFailure)
        assertEquals("DataStore error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getCredential returns injected credential`() {
        val result = repo.getCredential()
        assertEquals(credential, result)
    }
}
