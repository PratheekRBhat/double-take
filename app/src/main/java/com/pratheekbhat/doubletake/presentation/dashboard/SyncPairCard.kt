package com.pratheekbhat.doubletake.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratheekbhat.doubletake.ui.theme.OrangePrimary

enum class SyncStatus {
    SYNCED,
    SYNCING,
    ERROR
}

@Composable
fun SyncPairCard(
    localFolderName: String,
    driveFolderName: String,
    status: SyncStatus,
    statusText: String,
    detailText: String,
    progress: Float = 1f,
    onDelete: () -> Unit,
    onSyncNow: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val statusColor = when (status) {
        SyncStatus.SYNCED -> Color(0xFF22C55E)
        SyncStatus.SYNCING -> OrangePrimary
        SyncStatus.ERROR -> Color(0xFFEF4444)
    }

    val progressColor = when (status) {
        SyncStatus.SYNCED -> OrangePrimary
        SyncStatus.SYNCING -> OrangePrimary
        SyncStatus.ERROR -> Color(0xFFEF4444)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status dot
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = statusColor
                ) {}

                Spacer(modifier = Modifier.width(8.dp))

                // Local folder name
                Text(
                    text = localFolderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Sync icon
                Icon(
                    imageVector = Icons.Filled.SyncAlt,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Drive folder name
                Text(
                    text = driveFolderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                // Overflow menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sync Now") },
                            onClick = {
                                showMenu = false
                                onSyncNow()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Status text + detail
            Row {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
                Text(
                    text = "  ·  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
