package com.pratheekbhat.doubletake.presentation.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratheekbhat.doubletake.data.local.SyncLogDao
import com.pratheekbhat.doubletake.data.local.SyncLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LogFilter(val label: String) {
    ALL("All"),
    UPLOADS("Uploads"),
    DOWNLOADS("Downloads"),
    DELETES("Deletes"),
    CONFLICTS("Conflicts")
}

data class SyncLogUiState(
    val logs: List<SyncLogEntity> = emptyList(),
    val selectedFilter: LogFilter = LogFilter.ALL,
    val isLoading: Boolean = false
)

@HiltViewModel
class SyncLogViewModel @Inject constructor(
    private val syncLogDao: SyncLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncLogUiState())
    val uiState: StateFlow<SyncLogUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    fun onFilterSelected(filter: LogFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            val filter = _uiState.value.selectedFilter
            val flow = when (filter) {
                LogFilter.ALL -> syncLogDao.getRecentLogs()
                LogFilter.UPLOADS -> syncLogDao.getLogsByAction("UPLOAD")
                LogFilter.DOWNLOADS -> syncLogDao.getLogsByAction("DOWNLOAD")
                LogFilter.DELETES -> syncLogDao.getLogsByAction("TRASH_LOCAL")
                LogFilter.CONFLICTS -> syncLogDao.getLogsByAction("CONFLICT")
            }
            flow.collect { logs ->
                _uiState.update { it.copy(logs = logs, isLoading = false) }
            }
        }
    }
}
