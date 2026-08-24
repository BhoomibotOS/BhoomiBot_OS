package com.bhoomibot.os.feature.autonomous.skills.library.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun SkillLibraryScreen(
    onBack: () -> Unit,
    viewModel: SkillLibraryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Skill Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SignalGreen)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(state.skills) { skill ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(skill.name, fontWeight = FontWeight.Bold)
                                Text("${skill.steps.size} recorded steps", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { /* Run Skill */ }) {
                                Icon(Icons.Default.PlayArrow, null, tint = SignalGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}
