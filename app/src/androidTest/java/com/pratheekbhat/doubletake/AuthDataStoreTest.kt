package com.pratheekbhat.doubletake

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pratheekbhat.doubletake.data.local.AuthDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthDataStoreTest {

    private lateinit var authDataStore: AuthDataStore

    @Before
    fun setup() {
        authDataStore = AuthDataStore(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun isSignedIn_defaultsToFalse() = runTest {
        val result = authDataStore.isSignedIn.first()
        assertFalse(result)
    }

    @Test
    fun saveAuthState_setsSignedInAndAccountName() = runTest {
        authDataStore.saveAuthState("test@gmail.com")

        val isSignedIn = authDataStore.isSignedIn.first()
        val accountName = authDataStore.accountName.first()

        assertTrue(isSignedIn)
        assertEquals("test@gmail.com", accountName)
    }

    @Test
    fun clearAuthState_setsSignedOutAndRemovesAccountName() = runTest {
        authDataStore.saveAuthState("test@gmail.com")
        authDataStore.clearAuthState()

        val isSignedIn = authDataStore.isSignedIn.first()
        val accountName = authDataStore.accountName.first()

        assertFalse(isSignedIn)
        assertNull(accountName)
    }

    @Test
    fun saveAuthState_roundTrip_preservesAccountName() = runTest {
        val email = "pratheek@gmail.com"
        authDataStore.saveAuthState(email)

        val retrieved = authDataStore.accountName.first()
        assertEquals(email, retrieved)
    }
}
