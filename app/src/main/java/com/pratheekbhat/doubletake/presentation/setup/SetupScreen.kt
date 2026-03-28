package com.pratheekbhat.doubletake.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(
    onConnectDrive: () -> Unit,
    onChooseLocalFolder: () -> Unit,
    onSelectDriveFolder: () -> Unit,
    onStartSync: () -> Unit,
    uiState: SetupUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // -- App icon --
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Cloud,
                contentDescription = "DoubleTake logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // -- Title --
        Text(
            text = "Welcome to DoubleTake",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // -- Subtitle --
        Text(
            text = "Seamlessly sync your world",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // -- Section header (left-aligned) --
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Setup Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connect your accounts to start syncing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // -- Setup cards --
        SetupCard(
            icon = Icons.Filled.Cloud,
            title = "Connect Google Drive",
            subtitle = if (uiState.isDriveConnected) "Connected" else "Not connected",
            enabled = true,
            onClick = onConnectDrive
        )

        Spacer(modifier = Modifier.height(12.dp))

        SetupCard(
            icon = Icons.Filled.Folder,
            title = "Choose Local Folder",
            subtitle = if (uiState.isLocalFolderChosen) "Folder selected" else "Select where to sync files ",
            enabled = uiState.isDriveConnected,
            onClick = onChooseLocalFolder
        )

        Spacer(modifier = Modifier.height(12.dp))

        SetupCard(
            icon = Icons.Filled.CreateNewFolder,
            title = "Select Drive Folder",
            subtitle = if (uiState.isDriveFolderSelected) "Folder selected" else "Pick or create a folder ",
            enabled = uiState.isDriveConnected,
            onClick = onSelectDriveFolder
        )

        // -- Push bottom content down --
        Spacer(modifier = Modifier.weight(1f))

        // -- Privacy notice --
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "We only request access to files we create. Your privacy and data security are our top priorities .",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // -- Start syncing button --
        val allSetupDone = uiState.isDriveConnected && uiState.isLocalFolderChosen && uiState.isDriveFolderSelected

        Button(
            onClick = onStartSync,
            enabled = allSetupDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "START SYNCING")
        }
    }
}