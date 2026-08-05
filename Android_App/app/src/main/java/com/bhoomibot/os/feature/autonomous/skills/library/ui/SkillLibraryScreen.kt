package com.bhoomibot.os.feature.autonomous.skills.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.feature.autonomous.skills.models.ActionType
import com.bhoomibot.os.feature.autonomous.skills.models.DemonstratedSkill
import com.bhoomibot.os.feature.autonomous.skills.models.SkillStep
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SignalGreen
import com.bhoomibot.os.ui.theme.SafetyRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillLibraryScreen(
    onBackClick: () -> Unit,
    viewModel: SkillLibraryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skill Library", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.selectedSkill != null) {
            SkillDetailViewer(
                skill = state.selectedSkill!!,
                onClose = { viewModel.selectSkill(null) }
            )
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SignalGreen)
                    }
                } else if (state.skills.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No learned skills yet. Teach one via Agent AI!", color = MutedText)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(state.skills) { skill ->
                            SkillCard(skill, onClick = { viewModel.selectSkill(skill) })
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun SkillCard(skill: DemonstratedSkill, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = SignalGreen.copy(alpha = 0.2f)
            ) {
                Icon(Icons.Default.Psychology, null, tint = SignalGreen, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(skill.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${skill.steps.size} logical steps", style = MaterialTheme.typography.labelSmall, color = MutedText)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MutedText)
        }
    }
}

@Composable
fun SkillDetailViewer(
    skill: DemonstratedSkill, 
    onClose: () -> Unit,
    playbackViewModel: com.bhoomibot.os.feature.mission.PlaybackViewModel = viewModel()
) {
    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(skill.createdTimestamp))
    var virtualMode by remember { mutableStateOf(true) }
    val playbackState by playbackViewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
            Column(Modifier.weight(1f)) {
                Text(skill.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Learned on $date", style = MaterialTheme.typography.labelSmall, color = MutedText)
            }
        }
        
        Spacer(Modifier.height(16.dp))

        // Visual Map Preview with Ghost Robot
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            val hasGps = skill.steps.any { it.latitude != null && it.latitude != 0.0 }
            if (hasGps) {
                com.bhoomibot.os.feature.map.SkillMapLayer(
                    skill = skill,
                    ghostPose = playbackState.virtualRobotPose
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Map, null, tint = MutedText.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No GPS Data for this Skill", color = MutedText, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        // Mode Selector: Virtual vs Physical
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (virtualMode) "VIRTUAL PREVIEW ACTIVE" else "PHYSICAL REPLAY READY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (virtualMode) Color.White else SignalGreen
                )
                Text(
                    text = if (virtualMode) "Simulation only, no hardware action" else "Robot will move physically!",
                    fontSize = 10.sp,
                    color = MutedText
                )
            }
            Switch(
                checked = virtualMode,
                onCheckedChange = { virtualMode = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MutedText,
                    uncheckedThumbColor = SignalGreen,
                    uncheckedTrackColor = SignalGreen.copy(alpha = 0.2f)
                )
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("LOGICAL WORKFLOW", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = SignalGreen)
        Spacer(Modifier.height(12.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(skill.steps.sortedBy { it.sequence }) { step ->
                StepItem(
                    step = step,
                    isActive = playbackState.currentStepIndex == step.sequence && playbackState.isVirtualMode
                )
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (playbackState.isVirtualMode) {
                OutlinedButton(
                    onClick = { playbackViewModel.stopVirtualValidation() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafetyRed)
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("STOP", fontWeight = FontWeight.Black)
                }
            }

            Button(
                onClick = { 
                    if (playbackState.isVirtualMode) return@Button
                    
                    if (virtualMode) {
                        playbackViewModel.startVirtualValidation(skill.steps)
                    } else {
                        playbackViewModel.startPhysicalReplay(skill)
                    }
                },
                modifier = Modifier.weight(2f).height(56.dp),
                enabled = !playbackState.isVirtualMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (virtualMode) Color.White else SignalGreen,
                    contentColor = if (virtualMode) Color.Black else Color.White
                )
            ) {
                if (playbackState.isVirtualMode) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(if (virtualMode) Icons.Default.Visibility else Icons.Default.PlayArrow, null)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (playbackState.isVirtualMode) "VALIDATING..." 
                    else if (virtualMode) "RUN VIRTUAL VALIDATION" 
                    else "START AUTONOMOUS REPLAY", 
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun StepItem(step: SkillStep, isActive: Boolean = false) {
    val icon = when (step.actionType) {
        ActionType.NAVIGATE -> Icons.Default.LocationOn
        ActionType.ATTACH -> Icons.Default.Inbox
        ActionType.DETACH -> Icons.Default.Outbox
        ActionType.WAIT -> Icons.Default.HourglassEmpty
        ActionType.ACTUATE_PTO -> Icons.Default.Power
    }
    
    val color = if (isActive) SignalGreen else if (step.actionType == ActionType.NAVIGATE) MutedText else SignalGreen
    val bgColor = if (isActive) SignalGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(32.dp)) {
            if (isActive) {
                Icon(Icons.Default.PlayArrow, null, tint = SignalGreen, modifier = Modifier.size(12.dp))
            } else {
                Text("${step.sequence + 1}", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, SignalGreen) else null
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(step.actionType.name, fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 13.sp)
                    if (step.latitude != null) {
                        Text("${String.format("%.5f", step.latitude)}, ${String.format("%.5f", step.longitude)}", fontSize = 10.sp, color = MutedText)
                    } else if (step.parameters.isNotEmpty()) {
                        Text(step.parameters.toString(), fontSize = 10.sp, color = MutedText)
                    }
                }
            }
        }
    }
}
