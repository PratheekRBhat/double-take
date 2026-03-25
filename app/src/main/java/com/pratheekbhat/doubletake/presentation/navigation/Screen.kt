package com.pratheekbhat.doubletake.presentation.navigation

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Dashboard : Screen("dashboard")
    data object AddSyncPair : Screen("add_sync_pair")
    data object SyncLog : Screen("sync_log")
    data object Settings : Screen("settings")
}