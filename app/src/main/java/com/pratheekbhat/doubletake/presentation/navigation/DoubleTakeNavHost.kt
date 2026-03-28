package com.pratheekbhat.doubletake.presentation.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pratheekbhat.doubletake.presentation.setup.DriveFolderPickerScreen
import com.pratheekbhat.doubletake.presentation.setup.DriveFolderPickerViewModel
import com.pratheekbhat.doubletake.presentation.setup.SetupScreen
import com.pratheekbhat.doubletake.presentation.setup.SetupViewModel

@Composable
fun DoubleTakeNavHost(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Setup.route,
        modifier = modifier
    ) {
        composable(Screen.Setup.route) {
            val viewModel: SetupViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            val authLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.onAuthResult(result.data)
                    } else {
                        viewModel.onAuthResult(null)
                    }
                }

            val folderPickerLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
                    if (uri != null) {
                        val documentFile = DocumentFile.fromTreeUri(context, uri)
                        viewModel.onLocalFolderChosen(uri, documentFile?.name ?: "Selected Folder")
                    }
                }

            LaunchedEffect(Unit) {
                viewModel.authIntentEvent.collect { result ->
                    authLauncher.launch(result)
                }
            }

            val driveFolderId = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.getStateFlow<String?>("drive_folder_id", null)
                ?.collectAsStateWithLifecycle()

            val driveFolderName = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.getStateFlow<String?>("drive_folder_name", null)
                ?.collectAsStateWithLifecycle()

            LaunchedEffect(driveFolderId?.value, driveFolderName?.value) {
                val id = driveFolderId?.value
                val name = driveFolderName?.value
                if (id != null && name != null) {
                    viewModel.onDriveFolderSelected(id, name)
                }
            }

            SetupScreen(
                uiState = uiState,
                onConnectDrive = { viewModel.onConnectDrive(context as Activity) },
                onChooseLocalFolder = { folderPickerLauncher.launch(null) },
                onSelectDriveFolder = { navController.navigate(Screen.DriveFolderPicker.route) },
                onStartSync = {
                    viewModel.onStartSync()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DriveFolderPicker.route) {
            val pickerViewModel: DriveFolderPickerViewModel = hiltViewModel()
            val pickerState by pickerViewModel.uiState.collectAsStateWithLifecycle()

            DriveFolderPickerScreen(
                folders = pickerState.folders,
                isLoading = pickerState.isLoading,
                error = pickerState.error,
                onFolderSelected = { id, name ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("drive_folder_id", id)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("drive_folder_name", name)
                    navController.popBackStack()
                },
                onCreateFolder = { name -> pickerViewModel.createFolder(name) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            // TODO: DashboardScreen()
        }

        composable(Screen.AddSyncPair.route) {
            // TODO: AddSyncPairScreen()
        }

        composable(Screen.SyncLog.route) {
            // TODO: SyncLogScreen()
        }

        composable(Screen.Settings.route) {
            // TODO: SettingsScreen()
        }
    }
}