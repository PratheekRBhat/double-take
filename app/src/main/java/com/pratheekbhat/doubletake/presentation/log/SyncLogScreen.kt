package com.pratheekbhat.doubletake.presentation.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pratheekbhat.doubletake.data.local.SyncLogEntity
import com.pratheekbhat.doubletake.ui.theme.OrangePrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLogScreen(
    uiState: SyncLogUiState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Log", fontWeight = FontWeight.Bold) },
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
        ) {
            // Filter chips - just display, interaction handled by parent
            FilterChipRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = {} // will be wired from NavHost
            )

            if (uiState.logs.isEmpty()) {
                EmptySyncLog(modifier = Modifier.weight(1f))
            } else {
                LogList(
                    logs = uiState.logs,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLogScreenWithCallbacks(
    uiState: SyncLogUiState,
    onFilterSelected: (LogFilter) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Log", fontWeight = FontWeight.Bold) },
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
        ) {
            FilterChipRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = onFilterSelected
            )

            if (uiState.logs.isEmpty()) {
                EmptySyncLog(modifier = Modifier.weight(1f))
            } else {
                LogList(
                    logs = uiState.logs,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    selectedFilter: LogFilter,
    onFilterSelected: (LogFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(LogFilter.entries) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun EmptySyncLog(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = OrangePrimary.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier
                        .padding(32.dp)
                        .size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No sync activity yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sync logs will appear here after your first sync is completed or failed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LogList(
    logs: List<SyncLogEntity>,
    modifier: Modifier = Modifier
) {
    val groupedLogs = groupLogsByDate(logs)

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groupedLogs.forEach { (dateLabel, logsInGroup) ->
            item {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(logsInGroup, key = { it.id }) { log ->
                SyncLogItem(log = log)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SyncLogItem(log: SyncLogEntity) {
    val (icon, iconTint, bgColor) = getLogVisuals(log)
    val timeText = formatTime(log.timestamp)
    val fileName = log.filePath.substringAfterLast("/")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Status icon
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = bgColor
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (log.details != null) {
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (log.result) {
                        "FAILURE" -> Color(0xFFEF4444)
                        else -> if (log.action == "CONFLICT") OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (log.sourceName != null && log.destinationName != null) {
                Text(
                    text = "${log.sourceName}  ->  ${log.destinationName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Time / status badge
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (log.result == "FAILURE") {
                Text(
                    text = "FAILED",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            } else if (log.action == "CONFLICT") {
                Text(
                    text = "CONFLICT",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class LogVisuals(
    val icon: ImageVector,
    val iconTint: Color,
    val bgColor: Color
)

private fun getLogVisuals(log: SyncLogEntity): LogVisuals {
    return when {
        log.result == "FAILURE" -> LogVisuals(
            icon = Icons.Filled.Error,
            iconTint = Color(0xFFEF4444),
            bgColor = Color(0xFFEF4444).copy(alpha = 0.1f)
        )
        log.action == "CONFLICT" -> LogVisuals(
            icon = Icons.Filled.Warning,
            iconTint = Color(0xFFF59E0B),
            bgColor = Color(0xFFF59E0B).copy(alpha = 0.1f)
        )
        log.action == "UPLOAD" -> LogVisuals(
            icon = Icons.Filled.CloudUpload,
            iconTint = OrangePrimary,
            bgColor = OrangePrimary.copy(alpha = 0.1f)
        )
        log.action == "DOWNLOAD" -> LogVisuals(
            icon = Icons.Filled.CloudDownload,
            iconTint = OrangePrimary,
            bgColor = OrangePrimary.copy(alpha = 0.1f)
        )
        log.action.startsWith("TRASH") -> LogVisuals(
            icon = Icons.Filled.Delete,
            iconTint = Color(0xFFEF4444),
            bgColor = Color(0xFFEF4444).copy(alpha = 0.1f)
        )
        else -> LogVisuals(
            icon = Icons.Filled.CheckCircle,
            iconTint = Color(0xFF22C55E),
            bgColor = Color(0xFF22C55E).copy(alpha = 0.1f)
        )
    }
}

private fun groupLogsByDate(logs: List<SyncLogEntity>): List<Pair<String, List<SyncLogEntity>>> {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    return logs.groupBy { log ->
        val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
        when {
            isSameDay(logCal, today) -> "TODAY"
            isSameDay(logCal, yesterday) -> "YESTERDAY"
            else -> dateFormat.format(Date(log.timestamp)).uppercase()
        }
    }.toList()
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        else -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
