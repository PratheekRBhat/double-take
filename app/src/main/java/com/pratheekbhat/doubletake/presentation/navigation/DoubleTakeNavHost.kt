package com.pratheekbhat.doubletake.presentation.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pratheekbhat.doubletake.presentation.setup.SetupScreen
import com.pratheekbhat.doubletake.presentation.setup.SetupUiState
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

            val authLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.onAuthResult(result.data)
                } else {
                    viewModel.onAuthResult(null)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.authIntentEvent.collect { result ->
                    authLauncher.launch(result)
                }
            }

            SetupScreen(
                uiState = uiState,
                onConnectDrive = { viewModel.onConnectDrive(context as Activity) },
                onChooseLocalFolder = { /* TODO */ },
                onSelectDriveFolder = { /* TODO */ },
                onStartSync = {
                    viewModel.onStartSync()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
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