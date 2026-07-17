package com.bhoomibot.os.feature.diagnostics
import androidx.compose.runtime.Composable
import com.bhoomibot.os.feature.common.OperationalScreen
@Composable fun DiagnosticsScreen(onBackClick: () -> Unit) = OperationalScreen("Diagnostics", "System health", onBackClick)
