package com.pratheekbhat.doubletake

import com.pratheekbhat.doubletake.data.local.SyncLogDao
import com.pratheekbhat.doubletake.data.local.SyncLogEntity
import com.pratheekbhat.doubletake.presentation.log.LogFilter
import com.pratheekbhat.doubletake.presentation.log.SyncLogViewModel
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
import org.junit.Before
import org.junit.Test

class SyncLogViewModelTest {

    private lateinit var syncLogDao: SyncLogDao
    private lateinit var viewModel: SyncLogViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun buildLog(id: Long, action: String) = SyncLogEntity(
        id = id,
        syncPairId = 1L,
        timestamp = 1000L + id,
        action = action,
        filePath = "file_$id.txt",
        result = "SUCCESS"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        syncLogDao = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // -- Initial state --

    @Test
    fun `initial state has ALL filter selected and empty logs`() = runTest {
        every { syncLogDao.getRecentLogs() } returns flowOf(emptyList())

        viewModel = SyncLogViewModel(syncLogDao)

        assertEquals(LogFilter.ALL, viewModel.uiState.value.selectedFilter)
        assertEquals(emptyList<SyncLogEntity>(), viewModel.uiState.value.logs)
    }

    // -- Logs load from DAO --

    @Test
    fun `logs load from DAO on init`() = runTest {
        val logs = listOf(
            buildLog(1L, "UPLOAD"),
            buildLog(2L, "DOWNLOAD")
        )
        every { syncLogDao.getRecentLogs() } returns flowOf(logs)

        viewModel = SyncLogViewModel(syncLogDao)

        assertEquals(logs, viewModel.uiState.value.logs)
    }

    // -- Filter changes --

    @Test
    fun `filter change to UPLOADS loads upload logs`() = runTest {
        val uploadLogs = listOf(buildLog(1L, "UPLOAD"))
        every { syncLogDao.getRecentLogs() } returns flowOf(emptyList())
        every { syncLogDao.getLogsByAction("UPLOAD") } returns flowOf(uploadLogs)

        viewModel = SyncLogViewModel(syncLogDao)
        viewModel.onFilterSelected(LogFilter.UPLOADS)

        assertEquals(LogFilter.UPLOADS, viewModel.uiState.value.selectedFilter)
        assertEquals(uploadLogs, viewModel.uiState.value.logs)
        verify(exactly = 1) { syncLogDao.getLogsByAction("UPLOAD") }
    }

    @Test
    fun `filter change to DOWNLOADS loads download logs`() = runTest {
        val downloadLogs = listOf(buildLog(2L, "DOWNLOAD"))
        every { syncLogDao.getRecentLogs() } returns flowOf(emptyList())
        every { syncLogDao.getLogsByAction("DOWNLOAD") } returns flowOf(downloadLogs)

        viewModel = SyncLogViewModel(syncLogDao)
        viewModel.onFilterSelected(LogFilter.DOWNLOADS)

        assertEquals(LogFilter.DOWNLOADS, viewModel.uiState.value.selectedFilter)
        assertEquals(downloadLogs, viewModel.uiState.value.logs)
        verify(exactly = 1) { syncLogDao.getLogsByAction("DOWNLOAD") }
    }

    @Test
    fun `filter change to CONFLICTS loads conflict logs`() = runTest {
        val conflictLogs = listOf(buildLog(3L, "CONFLICT"))
        every { syncLogDao.getRecentLogs() } returns flowOf(emptyList())
        every { syncLogDao.getLogsByAction("CONFLICT") } returns flowOf(conflictLogs)

        viewModel = SyncLogViewModel(syncLogDao)
        viewModel.onFilterSelected(LogFilter.CONFLICTS)

        assertEquals(LogFilter.CONFLICTS, viewModel.uiState.value.selectedFilter)
        assertEquals(conflictLogs, viewModel.uiState.value.logs)
        verify(exactly = 1) { syncLogDao.getLogsByAction("CONFLICT") }
    }
}
