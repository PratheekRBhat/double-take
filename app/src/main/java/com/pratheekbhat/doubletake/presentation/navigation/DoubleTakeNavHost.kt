package com.pratheekbhat.doubletake.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun DoubleTakeNavHost(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Setup.route,
        modifier = modifier
    ) {
        composable(Screen.Setup.route) {
            // TODO: SetupScreen()
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