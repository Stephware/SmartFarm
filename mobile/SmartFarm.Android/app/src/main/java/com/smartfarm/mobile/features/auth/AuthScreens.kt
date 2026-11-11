package com.smartfarm.mobile.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.CropArtworkImage
import com.smartfarm.mobile.ui.components.FarmSectionCard

@Composable
fun LoginScreen(state: AuthUiState, onSubmit: (String, String) -> Unit, onRegister: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    // Never save credentials to saved instance state.
    var password by remember { mutableStateOf("") }
    val ready = looksLikeEmail(email) && password.isNotBlank()
    AuthScaffold("Welcome back", "Log in to open your farms and pick up today's work.") {
        FarmSectionCard("Log in", subtitle = "Use your AniGo account details.") {
            OutlinedTextField(
                email, { email = it }, label = { Text("Email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth(),
                isError = email.isNotBlank() && !looksLikeEmail(email), singleLine = true
            )
            if (email.isNotBlank() && !looksLikeEmail(email))
                Text("Enter a valid email address.", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            PasswordInput("Password", password, { password = it }, imeAction = ImeAction.Done)
            Feedback(state)
            Button(onClick = { onSubmit(email.trim(), password) },
                enabled = !state.busy && ready, modifier = Modifier.fillMaxWidth()) {
                Text("Log in to my farms")
            }
            Text("New to AniGo?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onRegister, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) { Text("Create an account") }
        }
    }
}

@Composable
fun RegisterScreen(state: AuthUiState, onSubmit: (String, String, String) -> Unit,
    onLogin: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords are intentionally never stored with rememberSaveable.
    var password by remember { mutableStateOf("") }
    val canCreate = registrationReady(name, email, password)
    AuthScaffold("Grow with AniGo", "Create an account to manage your farm or join a team.") {
        FarmSectionCard("Create your account", subtitle = "One account can work with multiple farms.") {
            OutlinedTextField(name, { name = it }, label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = name.isNotBlank() && name.trim().length !in 2..120)
            OutlinedTextField(email, { email = it }, label = { Text("Email address") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = email.isNotBlank() && !looksLikeEmail(email),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next))
            if (email.isNotBlank() && !looksLikeEmail(email))
                Text("Enter a valid email address.", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            PasswordInput("Password", password, { password = it }, imeAction = ImeAction.Next,
                error = password.isNotBlank() && !meetsPasswordGuidance(password))
            Text("Use 10–128 characters with uppercase, lowercase and a number.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            Feedback(state)
            Button(onClick = { onSubmit(name.trim(), email.trim(), password) },
                enabled = !state.busy && canCreate, modifier = Modifier.fillMaxWidth()) {
                Text("Create account")
            }
            Text("After registering, sign in to create a farm or request to join one.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onLogin, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) { Text("Back to log in") }
        }
    }
}

@Composable
fun AccountScreen(state: AuthUiState, onLogout: () -> Unit, onFarms: () -> Unit,
    onSync: () -> Unit, onNotifications: () -> Unit = {}) {
    AuthScaffold("My account", "Your profile and shortcuts to farm operations.") {
        FarmSectionCard("Account information", subtitle = "Signed in to AniGo") {
            val name = state.account?.fullName.orEmpty()
            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A"
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(initial, modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.headlineMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(state.account?.email.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Farm roles and team permissions are managed within each farm.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FarmSectionCard("Your workspace", subtitle = "Open your farm or review saved fieldwork.") {
            Button(onClick = onFarms, modifier = Modifier.fillMaxWidth()) { Text("My farms") }
            OutlinedButton(onClick = onNotifications, modifier = Modifier.fillMaxWidth()) {
                Text("Notifications")
            }
            OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
                Text("Offline sync status")
            }
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Log out")
        }
    }
}

@Composable
private fun PasswordInput(label: String, value: String, onChange: (String) -> Unit,
    imeAction: ImeAction, error: Boolean = false) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        isError = error, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Hide" else "Show")
            }
        }
    )
}

@Composable
private fun Feedback(state: AuthUiState) {
    if (state.busy) CircularProgressIndicator()
    state.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
    }
    state.notice?.let {
        Text(it, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
private fun AuthScaffold(title: String, subtitle: String,
    content: @Composable () -> Unit) {
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FarmHero(title, subtitle)
                content()
            }
        }
    }
}
