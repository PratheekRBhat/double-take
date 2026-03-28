package com.pratheekbhat.doubletake.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratheekbhat.doubletake.data.local.SyncPairEntity
import com.pratheekbhat.doubletake.domain.repository.SyncPairRepository
import com.pratheekbhat.doubletake.worker.SyncWorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val syncPairs: List<SyncPairEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val syncPairRepository: SyncPairRepository,
    private val syncWorkManager: SyncWorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeSyncPairs()
    }

    private fun observeSyncPairs() {
        viewModelScope.launch {
            syncPairRepository.getAllPairs().collect { pairs ->
                _uiState.update { it.copy(syncPairs = pairs, isLoading = false) }
            }
        }
    }

    fun onSyncNow(syncPairId: Long) {
        Log.d("[DashboardViewModel]", "Triggering sync for pair $syncPairId")
        syncWorkManager.triggerImmediateSync(syncPairId)
    }

    fun onDeletePair(pairId: Long) {
        viewModelScope.launch {
            try {
                syncPairRepository.deletePair(pairId)
                Log.d("[DashboardViewModel]", "Deleted sync pair $pairId")
            } catch (e: Exception) {
                Log.e("[DashboardViewModel]", "Failed to delete pair $pairId", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
