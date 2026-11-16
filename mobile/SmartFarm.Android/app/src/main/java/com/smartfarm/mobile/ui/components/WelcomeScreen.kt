package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.defaultMinSize
import com.smartfarm.mobile.R
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
import androidx.compose.material3.ButtonDefaults
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
import com.smartfarm.mobile.ui.theme.AniGoPalette
import com.smartfarm.mobile.ui.theme.LocalFarmAppearance

@Composable
fun WelcomeScreen(
    status: StatusUiState = StatusUiState(),
    onCheckBackend: () -> Unit = {},
    onLogin: () -> Unit = {},
    onRegister: () -> Unit = {}
) {
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }
    val darkMode = LocalFarmAppearance.current.dark
    Scaffold(topBar = { FarmAppBar() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(FarmSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(FarmSpacing.medium)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 238.dp)) {
                        // Existing, optimized agricultural photo shared with the MVC landing.
                        Image(painterResource(R.drawable.ani6_fields_hero),
                            contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize())
                        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                            listOf(Color(0xBF102018), Color(0xE6102F22)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(FarmSpacing.large),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("ONE CONNECTED WORKSPACE",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFF1CC7D))
                            Text("Welcome to AniGo",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White)
                            Text("Plan crops, record fieldwork, and track harvests in one place.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFF4F7F1))
                        }
                    }
                }
                FarmSectionCard("Get started", subtitle = "Use one account to manage a farm or join your team.") {
                    Button(onClick = onLogin,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        colors = if (darkMode) ButtonDefaults.buttonColors(
                            containerColor = AniGoPalette.darkAction,
                            contentColor = AniGoPalette.onDarkAction
                        ) else ButtonDefaults.buttonColors()
                    ) {
                        Text("Log in")
                    }
                    OutlinedButton(onClick = onRegister,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                        Text("Create account")
                    }
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
