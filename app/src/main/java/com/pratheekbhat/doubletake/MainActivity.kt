package com.pratheekbhat.doubletake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.pratheekbhat.doubletake.data.local.AuthDataStore
import com.pratheekbhat.doubletake.domain.repository.SyncPairRepository
import com.pratheekbhat.doubletake.presentation.navigation.BottomNavBar
import com.pratheekbhat.doubletake.presentation.navigation.DoubleTakeNavHost
import com.pratheekbhat.doubletake.presentation.navigation.Screen
import com.pratheekbhat.doubletake.ui.theme.DoubleTakeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authDataStore: AuthDataStore
    @Inject lateinit var credential: GoogleAccountCredential
    @Inject lateinit var syncPairRepository: SyncPairRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoubleTakeTheme {
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    // Restore credential
                    val accountName = authDataStore.accountName.first()
                    if (accountName != null) {
                        credential.selectedAccountName = accountName
                    }

                    // Determine start destination
                    val isSignedIn = authDataStore.isSignedIn.first()
                    val hasPairs = syncPairRepository.getAllPairs().first().isNotEmpty()
                    startDestination = if (isSignedIn && hasPairs) Screen.Dashboard.route else Screen.Setup.route
                }

                if (startDestination == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val navController = rememberNavController()
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                    val showBottomBar = currentRoute in listOf(
                        Screen.Dashboard.route,
                        Screen.SyncLog.route,
                        Screen.Settings.route
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            if (showBottomBar) {
                                BottomNavBar(navController)
                            }
                        }
                    ) { innerPadding ->
                        DoubleTakeNavHost(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding),
                            startDestination = startDestination!!
                        )
                    }
                }
            }
        }
    }
}
