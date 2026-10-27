package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import com.smartfarm.mobile.BuildConfig
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.R
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import com.smartfarm.mobile.features.status.StatusUiState

@Composable
fun WelcomeScreen(
    status: StatusUiState = StatusUiState(),
    onCheckBackend: () -> Unit = {},
    onLogin: () -> Unit = {},
    onRegister: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.welcome), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.welcome_description), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Log in") }
            OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
            if (BuildConfig.DEBUG) {
                Text(status.message, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onCheckBackend, enabled = !status.checking) {
                    Text(if (status.checking) "Checking..." else "Check backend")
                }
            }
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() { SmartFarmTheme { WelcomeScreen() } }


