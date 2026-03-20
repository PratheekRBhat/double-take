package com.pratheekbhat.doubletake

import android.app.Activity
import com.pratheekbhat.doubletake.data.remote.DriveServiceClient
import com.pratheekbhat.doubletake.domain.repository.AuthRepository
import com.pratheekbhat.doubletake.domain.usecase.SignInUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignInUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var driveServiceClient: DriveServiceClient
    private lateinit var useCase: SignInUseCase
    private lateinit var activity: Activity

    @Before
    fun setup() {
        authRepository = mockk(relaxed = true)
        driveServiceClient = mockk(relaxed = true)
        activity = mockk(relaxed = true)
        useCase = SignInUseCase(authRepository, driveServiceClient)
    }

    @Test
    fun `returns failure when play services unavailable`() = runTest {
        every { driveServiceClient.isPlayServicesAvailable() } returns false

        val result = useCase(activity)

        assertTrue(result.isFailure)
        assertEquals("Google Play Services is not available", result.exceptionOrNull()?.message)
    }

    @Test
    fun `delegates to authRepository when play services available`() = runTest {
        every { driveServiceClient.isPlayServicesAvailable() } returns true
        coEvery { authRepository.signIn(activity) } returns Result.success(Unit)

        val result = useCase(activity)

        assertTrue(result.isSuccess)
        coVerify { authRepository.signIn(activity) }
    }

    @Test
    fun `does not call signIn when play services unavailable`() = runTest {
        every { driveServiceClient.isPlayServicesAvailable() } returns false

        useCase(activity)

        coVerify(exactly = 0) { authRepository.signIn(any()) }
    }

    @Test
    fun `propagates authRepository failure`() = runTest {
        every { driveServiceClient.isPlayServicesAvailable() } returns true
        coEvery { authRepository.signIn(activity) } returns Result.failure(RuntimeException("Auth failed"))

        val result = useCase(activity)

        assertTrue(result.isFailure)
        assertEquals("Auth failed", result.exceptionOrNull()?.message)
    }
}
