package com.pratheekbhat.doubletake.presentation.setup

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratheekbhat.doubletake.domain.usecase.AddSyncPairUseCase
import com.pratheekbhat.doubletake.worker.SyncWorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddSyncPairUiState(
    val isLocalFolderChosen: Boolean = false,
    val localFolderUri: String? = null,
    val localFolderName: String? = null,
    val isDriveFolderSelected: Boolean = false,
    val driveFolderId: String? = null,
    val driveFolderName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pairCreated: Boolean = false
)

@HiltViewModel
class AddSyncPairViewModel @Inject constructor(
    private val addSyncPairUseCase: AddSyncPairUseCase,
    private val syncWorkManager: SyncWorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSyncPairUiState())
    val uiState: StateFlow<AddSyncPairUiState> = _uiState.asStateFlow()

    fun onLocalFolderChosen(uri: Uri, folderName: String) {
        _uiState.update {
            it.copy(
                isLocalFolderChosen = true,
                localFolderUri = uri.toString(),
                localFolderName = folderName
            )
        }
    }

    fun onDriveFolderSelected(folderId: String, folderName: String) {
        _uiState.update {
            it.copy(
                isDriveFolderSelected = true,
                driveFolderId = folderId,
                driveFolderName = folderName
            )
        }
    }

    fun onAddPair() {
        val state = _uiState.value
        val localUri = state.localFolderUri ?: return
        val driveFolderId = state.driveFolderId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            addSyncPairUseCase(
                localFolderUri = Uri.parse(localUri),
                localFolderName = state.localFolderName ?: "Local Folder",
                driveFolderId = driveFolderId,
                driveFolderName = state.driveFolderName ?: "Drive Folder"
            )
                .onSuccess {
                    Log.d("[AddSyncPair]", "Pair created, triggering sync")
                    syncWorkManager.triggerImmediateSync()
                    _uiState.update { it.copy(isLoading = false, pairCreated = true) }
                }
                .onFailure { e ->
                    Log.e("[AddSyncPair]", "Failed to create pair", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
