package com.bhoomibot.os.feature.mission

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhoomibot.os.model.MissionMetadata
import com.bhoomibot.os.model.MissionRecord
import com.bhoomibot.os.ui.theme.SignalGreen
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.feature.map.FieldMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionLibraryScreen(
    viewModel: MissionLibraryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecording: () -> Unit,
    onNavigateToPlayback: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mission Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Record new mission button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SignalGreen)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onNavigateToRecording() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Record new mission", tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Record New Mission", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Missions list
            if (uiState.isLoading) {
                Text("Loading missions...", color = MutedText)
            } else if (uiState.missions.isEmpty()) {
                Text("No missions recorded yet. Start recording to create your first mission.", color = MutedText)
            } else {
                Text(
                    "Saved Missions (${uiState.missions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (uiState.selectedMission == null) 1f else 0.4f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.missions.forEach { metadata ->
                        MissionCard(
                            metadata = metadata,
                            onSelect = { viewModel.selectForPlayback(metadata.id) },
                            onDelete = { viewModel.deleteMission(metadata.id) }
                        )
                    }
                }
            }

            // Selected mission details + Map
            uiState.selectedMission?.let { mission ->
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.weight(0.6f)) {
                    MissionDetailCard(
                        mission = mission,
                        onPlay = onNavigateToPlayback,
                        onExpand = { width, passes -> viewModel.expandMission(mission, width, passes) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Real Field Mapping View
                    FieldMap(
                        waypoints = mission.waypoints,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionCard(
    metadata: MissionMetadata,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(metadata.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${metadata.waypointCount} waypoints · ${metadata.commandCount} commands · ${metadata.durationSeconds}s",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete mission", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MissionDetailCard(
    mission: MissionRecord,
    onPlay: (String) -> Unit,
    onExpand: (Double, Int) -> Unit
) {
    var showAiDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mission Details", style = MaterialTheme.typography.titleSmall)
                Row {
                    IconButton(onClick = { showAiDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Expand", tint = SignalGreen)
                    }
                    Button(
                        onClick = { onPlay(mission.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = SignalGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("START REPLAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Map, null, modifier = Modifier.size(14.dp), tint = MutedText)
                Spacer(Modifier.width(4.dp))
                Text("Waypoints: ${mission.waypoints.size}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Recorded: ${mission.createdTimestamp}", style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
    }

    if (showAiDialog) {
        AiExpandDialog(
            onDismiss = { showAiDialog = false },
            onConfirm = { width, passes ->
                onExpand(width, passes)
                showAiDialog = false
            }
        )
    }
}

@Composable
private fun AiExpandDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Int) -> Unit
) {
    var width by remember { mutableStateOf("2.5") }
    var passes by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Field Coverage") },
        text = {
            Column {
                Text("Expand this pass to cover the entire field.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = { Text("Implement Width (meters)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = passes,
                    onValueChange = { passes = it },
                    label = { Text("Number of Parallel Passes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                onConfirm(width.toDoubleOrNull() ?: 2.5, passes.toIntOrNull() ?: 5)
            }) {
                Text("GENERATE PATTERN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
