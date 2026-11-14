package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.smartfarm.mobile.BuildConfig
import com.smartfarm.mobile.features.status.StatusUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme

@Composable
fun WelcomeScreen(
    status: StatusUiState = StatusUiState(),
    onCheckBackend: () -> Unit = {},
    onLogin: () -> Unit = {},
    onRegister: () -> Unit = {}
) {
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = { FarmAppBar() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Welcome to AniGo", style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary)
                        Text("Your farms, fieldwork and harvests—organized in one place.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary)
                        Text("Plan crops · Record fieldwork · Track harvests",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                FarmSectionCard("Your farm workspace", subtitle = "Quick to learn and designed for work in the field.") {
                    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Log in") }
                    OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
                        Text("Create account")
                    }
                    Text("One account can access multiple farms. Your permissions follow each farm's role.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (BuildConfig.DEBUG) {
                    TextButton(onClick = { showDiagnostics = !showDiagnostics }) {
                        Text(if (showDiagnostics) "Hide developer diagnostics" else "Developer diagnostics")
                    }
                    if (showDiagnostics) {
                        Text(status.message, style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = onCheckBackend, enabled = !status.checking) {
                            Text(if (status.checking) "Checking..." else "Check backend")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() { SmartFarmTheme { WelcomeScreen() } }
