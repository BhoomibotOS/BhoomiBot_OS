package com.bhoomibot.os.feature.autonomous.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.ui.theme.SignalGreen

@Composable
fun AgentScreen(
    onBackClick: () -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToPlayback: (String, com.bhoomibot.sdk.TaskPlan?) -> Unit,
    viewModel: AgentViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when messages or processing state changes
    LaunchedEffect(messages.size, isProcessing) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { nav ->
            when (nav) {
                is AgentNavigation.ToManual -> onNavigateToManual()
                is AgentNavigation.ToPlayback -> onNavigateToPlayback(nav.missionName, nav.plan)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Chat Header
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, null) }
            Text("BhoomiBot Agent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isProcessing) {
                item { CircularProgressIndicator(Modifier.size(24.dp).padding(8.dp), strokeWidth = 2.dp) }
            }
        }

        // Suggestions
        Row(Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState())) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { viewModel.sendCommand(suggestion) }, 
                    label = { Text(suggestion) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Input Area
        var text by remember { mutableStateOf("") }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask the robot anything...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { 
                    if (text.isNotBlank()) {
                        viewModel.sendCommand(text)
                        text = ""
                    }
                },
                containerColor = SignalGreen
            ) { Icon(Icons.Default.Send, null, tint = Color.White) }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val align = if (msg.isUser) Alignment.End else Alignment.Start
    val color = if (msg.isUser) SignalGreen else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = align) {
        Surface(color = color, shape = RoundedCornerShape(12.dp)) {
            Text(msg.text, color = textColor, modifier = Modifier.padding(12.dp))
        }
    }
}
