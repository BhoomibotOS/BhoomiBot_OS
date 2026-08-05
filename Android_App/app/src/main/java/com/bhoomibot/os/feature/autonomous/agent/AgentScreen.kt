package com.bhoomibot.os.feature.autonomous.agent

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent as AndroidIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.ui.theme.SignalGreen
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgentScreen(
    onBackClick: () -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToPlayback: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    viewModel: AgentViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    var inputText by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Handle Navigation Events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is AgentNavigation.ToManual -> onNavigateToManual()
                is AgentNavigation.ToPlayback -> onNavigateToPlayback(event.missionId)
            }
        }
    }

    // Speech Recognizer
    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    LaunchedEffect(isListening) {
        if (isListening) {
            val intent = AndroidIntent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let { viewModel.sendCommand(it) }
                    viewModel.stopVoiceInput()
                }
                override fun onPartialResults(partialResults: Bundle) {
                    partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let { inputText = it }
                }
                override fun onError(error: Int) { viewModel.stopVoiceInput() }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer.startListening(intent)
        } else { recognizer.stopListening() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("BhoomiBot Agent", style = MaterialTheme.typography.titleMedium)
                        Text("AGENT OS v1.0", style = MaterialTheme.typography.labelSmall, color = SignalGreen)
                    }
                },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onNavigateToLibrary) {
                        Icon(Icons.Default.Psychology, "Skill Library", tint = SignalGreen)
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize().imePadding() 
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Spacer(Modifier.height(16.dp)) }
                items(messages) { msg -> ChatBubble(msg) }
                if (isProcessing) {
                    item { Text("Thinking...", style = MaterialTheme.typography.bodySmall, color = SignalGreen, modifier = Modifier.padding(start = 8.dp)) }
                }

                // AI-Fix: Suggestions Chips for easy selection
                if (suggestions.isNotEmpty() && !isProcessing) {
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { viewModel.sendCommand(suggestion) },
                                    label = { Text(suggestion, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        labelColor = SignalGreen
                                    )
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask something...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SignalGreen, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                        maxLines = 4,
                        trailingIcon = {
                            IconButton(onClick = { if (isListening) viewModel.stopVoiceInput() else viewModel.startVoiceInput() }) {
                                Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, null, tint = if (isListening) SafetyRed else SignalGreen)
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = { if (inputText.isNotBlank()) { viewModel.sendCommand(inputText); inputText = "" } },
                        containerColor = SignalGreen, contentColor = Color.Black, shape = CircleShape, modifier = Modifier.size(48.dp)
                    ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(color = color, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (message.isUser) 16.dp else 0.dp, bottomEnd = if (message.isUser) 0.dp else 16.dp)) {
            Text(text = message.text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = textColor, style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = if (message.isUser) "You" else "BhoomiBot", style = MaterialTheme.typography.labelSmall, color = MutedText, modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp))
    }
}
