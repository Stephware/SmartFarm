package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    onRegister: () -> Unit = {},
    onPreviewGuide: (() -> Unit)? = null
) {
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }
    PhotoAuthScaffold(
        headline = "Your farm starts here.",
        description = "Plan crops, record fieldwork and track harvests in one place.",
        formTitle = "Get started",
        formSubtitle = "Manage your own farm or join an existing team with one account."
    ) {
        PhotoAuthPrimaryButton(label = "Log in", onClick = onLogin)
        OutlinedButton(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Create account") }

        if (onPreviewGuide != null) {
            TextButton(onClick = onPreviewGuide, modifier = Modifier.fillMaxWidth()) {
                Text("Explore AniGo with our farm guide")
            }
        }

        // Diagnostics are never the prominent call-to-action and are stripped in release builds.
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

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() { SmartFarmTheme { WelcomeScreen() } }
