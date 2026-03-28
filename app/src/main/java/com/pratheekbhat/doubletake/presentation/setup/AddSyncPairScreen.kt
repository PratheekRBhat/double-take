package com.pratheekbhat.doubletake.presentation.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSyncPairScreen(
    uiState: AddSyncPairUiState,
    onChooseLocalFolder: () -> Unit,
    onSelectDriveFolder: () -> Unit,
    onAddPair: () -> Unit,
    onBack: () -> Unit
) {
    val allSelected = uiState.isLocalFolderChosen && uiState.isDriveFolderSelected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Sync Pair", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Folder Selection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose folders to sync together",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            SetupCard(
                icon = Icons.Filled.Folder,
                title = "Choose Local Folder",
                subtitle = if (uiState.isLocalFolderChosen) uiState.localFolderName ?: "Folder selected" else "Select where to sync files",
                enabled = true,
                onClick = onChooseLocalFolder
            )

            Spacer(modifier = Modifier.height(12.dp))

            SetupCard(
                icon = Icons.Filled.Cloud,
                title = "Select Drive Folder",
                subtitle = if (uiState.isDriveFolderSelected) uiState.driveFolderName ?: "Folder selected" else "Pick or create a folder",
                enabled = uiState.isLocalFolderChosen,
                onClick = onSelectDriveFolder
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onAddPair,
                enabled = allSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "ADD PAIR")
            }
        }
    }
}
