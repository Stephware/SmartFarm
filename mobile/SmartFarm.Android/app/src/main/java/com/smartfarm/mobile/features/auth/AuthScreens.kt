package com.smartfarm.mobile.features.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(state: AuthUiState, onSubmit: (String, String) -> Unit, onRegister: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AuthScaffold("Log in") {
        OutlinedTextField(email, { email = it }, label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(password, { password = it }, label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Feedback(state)
        Button(onClick = { onSubmit(email, password) },
            enabled = !state.busy && email.isNotBlank() && password.isNotBlank()) { Text("Log in") }
        OutlinedButton(onClick = onRegister) { Text("Create account") }
    }
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onSubmit: (String, String, String, String) -> Unit,
    onLogin: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AuthScaffold("Create an account") {
        OutlinedTextField(name, { name = it }, label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(email, { email = it }, label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(password, { password = it }, label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("At least 10 characters, with uppercase, lowercase, and a number.")
        Feedback(state)
        Button(onClick = { onSubmit(name, email, password, confirm) },
            enabled = !state.busy && name.isNotBlank() && email.isNotBlank() &&
                password.length >= 10 && password == confirm) { Text("Register") }
        OutlinedButton(onClick = onLogin) { Text("Back to log in") }
    }
}

@Composable
fun AccountScreen(state: AuthUiState, onLogout: () -> Unit, onFarms: () -> Unit,
    onSync: () -> Unit, onNotifications: () -> Unit = {}) {
    AuthScaffold("My account") {
        Text("Welcome, ${state.account?.fullName.orEmpty()}")
        Text(state.account?.email.orEmpty())
        Button(onClick = onFarms) { Text("My farms") }
        OutlinedButton(onClick = onNotifications) { Text("Notifications") }
        OutlinedButton(onClick = onSync) { Text("Offline sync status") }
        OutlinedButton(onClick = onLogout) { Text("Log out") }
    }
}

@Composable
private fun Feedback(state: AuthUiState) {
    if (state.busy) CircularProgressIndicator()
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
    state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
}

@Composable
private fun AuthScaffold(title: String, content: @Composable () -> Unit) {
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            content()
            }
        }
    }
}


