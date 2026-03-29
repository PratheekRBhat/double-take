package com.pratheekbhat.doubletake

import com.pratheekbhat.doubletake.data.local.SyncPairEntity
import com.pratheekbhat.doubletake.domain.repository.SyncPairRepository
import com.pratheekbhat.doubletake.presentation.dashboard.DashboardViewModel
import com.pratheekbhat.doubletake.worker.SyncWorkManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DashboardViewModelTest {

    private lateinit var syncPairRepository: SyncPairRepository
    private lateinit var syncWorkManager: SyncWorkManager
    private lateinit var viewModel: DashboardViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

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
        Dispatchers.setMain(testDispatcher)
        syncPairRepository = mockk(relaxed = true)
        syncWorkManager = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // -- Initial state --

    @Test
    fun `initial state has empty sync pairs`() = runTest {
        every { syncPairRepository.getAllPairs() } returns flowOf(emptyList())

        viewModel = DashboardViewModel(syncPairRepository, syncWorkManager)

        assertEquals(emptyList<SyncPairEntity>(), viewModel.uiState.value.syncPairs)
        assertNull(viewModel.uiState.value.error)
    }

    // -- Repository flow --

    @Test
    fun `sync pairs update from repository flow`() = runTest {
        val pairs = listOf(testPair, testPair.copy(id = 2L, localFolderName = "Second Folder"))
        every { syncPairRepository.getAllPairs() } returns flowOf(pairs)

        viewModel = DashboardViewModel(syncPairRepository, syncWorkManager)

        assertEquals(pairs, viewModel.uiState.value.syncPairs)
    }

    // -- onSyncNow --

    @Test
    fun `onSyncNow triggers SyncWorkManager with correct pair id`() = runTest {
        every { syncPairRepository.getAllPairs() } returns flowOf(emptyList())
        viewModel = DashboardViewModel(syncPairRepository, syncWorkManager)

        viewModel.onSyncNow(testPair.id)

        verify(exactly = 1) { syncWorkManager.triggerImmediateSync(testPair.id) }
    }

    // -- onDeletePair --

    @Test
    fun `onDeletePair calls repository deletePair with correct id`() = runTest {
        every { syncPairRepository.getAllPairs() } returns flowOf(emptyList())
        coEvery { syncPairRepository.deletePair(testPair.id) } returns Unit
        viewModel = DashboardViewModel(syncPairRepository, syncWorkManager)

        viewModel.onDeletePair(testPair.id)

        coVerify(exactly = 1) { syncPairRepository.deletePair(testPair.id) }
    }

    @Test
    fun `delete failure sets error state`() = runTest {
        val errorMessage = "Database error"
        every { syncPairRepository.getAllPairs() } returns flowOf(emptyList())
        coEvery { syncPairRepository.deletePair(testPair.id) } throws RuntimeException(errorMessage)
        viewModel = DashboardViewModel(syncPairRepository, syncWorkManager)

        viewModel.onDeletePair(testPair.id)

        assertEquals(errorMessage, viewModel.uiState.value.error)
    }
}
