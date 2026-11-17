package com.smartfarm.mobile.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.theme.LocalFarmAppearance
import androidx.compose.material3.Switch
import com.smartfarm.mobile.ui.components.FarmAppBar
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.PhotoAuthScaffold
import com.smartfarm.mobile.ui.components.PhotoAuthPrimaryButton

@Composable
fun LoginScreen(
    state: AuthUiState,
    onSubmit: (String, String) -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    // Credentials must never enter saved instance state or local storage.
    var password by remember { mutableStateOf("") }
    val ready = looksLikeEmail(email) && password.isNotBlank()

    PhotoAuthScaffold(
        headline = "Good to have you back.",
        description = "Your fields, crops and team — all in one connected place.",
        formTitle = "Log in",
        formSubtitle = "Pick up right where you left off."
    ) {
        OutlinedTextField(
            email, { email = it }, label = { Text("Email address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            isError = email.isNotBlank() && !looksLikeEmail(email), singleLine = true
        )
        if (email.isNotBlank() && !looksLikeEmail(email)) {
            Text("Enter a valid email address.", color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        PasswordInput("Password", password, { password = it }, imeAction = ImeAction.Done)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword, enabled = !state.busy) {
                Text("Forgot password?")
            }
        }
        Feedback(state)
        PhotoAuthPrimaryButton(
            label = "Log in",
            onClick = { onSubmit(email.trim(), password) },
            enabled = !state.busy && ready
        )
        Text("New here?", color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        OutlinedButton(
            onClick = onRegister, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Create account") }
    }
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onSubmit: (String, String, String) -> Unit,
    onLogin: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // Preserve the existing three-field registration contract; never save passwords.
    var password by remember { mutableStateOf("") }
    val canCreate = registrationReady(name, email, password)

    PhotoAuthScaffold(
        headline = "Grow with confidence.",
        description = "Plan crops, organize fieldwork and manage your farm in one place.",
        formTitle = "Create your account",
        formSubtitle = "One account can manage a farm or join a team.",
        compactHero = true
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = name.isNotBlank() && name.trim().length !in 2..120
        )
        OutlinedTextField(
            email, { email = it }, label = { Text("Email address") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(14.dp),
            isError = email.isNotBlank() && !looksLikeEmail(email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        if (email.isNotBlank() && !looksLikeEmail(email)) {
            Text("Enter a valid email address.", color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        PasswordInput(
            "Password", password, { password = it },
            imeAction = ImeAction.Done,
            error = password.isNotBlank() && !meetsPasswordGuidance(password)
        )
        Text("Use 10–128 characters with uppercase, lowercase and a number.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        Feedback(state)
        PhotoAuthPrimaryButton(
            label = "Create account",
            onClick = { onSubmit(name.trim(), email.trim(), password) },
            enabled = !state.busy && canCreate
        )
        Text("After registering, sign in to create a farm or request to join one.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = onLogin, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Back to log in") }
    }
}

/**
 * Account recovery is deliberately informative until a verified email/token reset service exists.
 * Never collect an email or claim a reset link has been sent without a working backend.
 */
@Composable
fun ForgotPasswordScreen(onLogin: () -> Unit) {
    PhotoAuthScaffold(
        headline = "Need help signing in?",
        description = "Keep your farm work within reach.",
        formTitle = "Forgot your password?",
        formSubtitle = "Account recovery information",
        compactHero = true
    ) {
        Text(
            "Email password reset is not available yet.",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "No reset link can be sent yet. Contact your system administrator for account assistance.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        PhotoAuthPrimaryButton("Back to log in", onClick = onLogin)
    }
}

@Composable
fun AccountScreen(state: AuthUiState, onLogout: () -> Unit, onFarms: () -> Unit,
    onSync: () -> Unit, onNotifications: () -> Unit = {},
    pendingSyncCount: Int = 0, onReplayGuide: (() -> Unit)? = null) {
    var confirmingLogout by rememberSaveable { mutableStateOf(false) }
    val name = state.account?.fullName.orEmpty()
    AuthScaffold("My account", "Your details and personal app settings.") {
        FarmSectionCard("Signed in as") {
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
                    Text(state.account?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Farm roles and membership are managed in each farm's Team & access section.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onFarms,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Text("My farms")
            }
        }
        FarmSectionCard("Appearance", subtitle = "Choose a comfortable display for fieldwork.") {
            val appearance = LocalFarmAppearance.current
            Row(Modifier.fillMaxWidth()
                .toggleable(value = appearance.dark, role = Role.Switch,
                    onValueChange = appearance.selectDark)
                .semantics { contentDescription = "Dark theme" }
                .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dark theme", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = appearance.dark, onCheckedChange = null)
            }
        }
        if (onReplayGuide != null) {
            FarmSectionCard("Help & onboarding",
                subtitle = "Review how to organize farms, crops, fieldwork, and harvests.") {
                OutlinedButton(
                    onClick = onReplayGuide,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                        .testTag("replay-anigo-guide")
                ) {
                    Text("Replay AniGo guide")
                }
            }
        }
        if (pendingSyncCount > 0) {
            FarmSectionCard("Work saved on this phone",
                subtitle = "These records have not yet been confirmed by the server.") {
                Text("$pendingSyncCount saved ${if (pendingSyncCount == 1) "record" else "records"} waiting or needing review.",
                    style = MaterialTheme.typography.titleSmall)
                Button(onClick = onSync, modifier = Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)) {
                    Text("Review offline sync")
                }
            }
        }
        FarmSectionCard("Other shortcuts") {
            OutlinedButton(onClick = onNotifications,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Text("Notifications")
            }
            if (pendingSyncCount == 0) {
                OutlinedButton(onClick = onSync,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                    Text("Offline sync")
                }
            }
        }
        OutlinedButton(onClick = { confirmingLogout = true }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                .testTag("request-sign-out")) {
            Text("Sign out")
        }
    }
    if (confirmingLogout) {
        AlertDialog(onDismissRequest = { confirmingLogout = false },
            title = { Text("Sign out of your account?") },
            text = {
                Text(if (pendingSyncCount > 0)
                    "$pendingSyncCount unsynced ${if (pendingSyncCount == 1) "record is" else "records are"} saved on this phone for this account. " +
                        "They will not sync while signed out. Sign back into the same account to resume. Unsaved form changes may be lost."
                else "Unsaved form changes may be lost. Your server-confirmed farm records will stay available when you sign back in.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingLogout = false
                    onLogout()
                }, modifier = Modifier.testTag("confirm-sign-out")) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingLogout = false }) { Text("Keep working") }
            }
        )
    }
}

@Composable
private fun PasswordInput(label: String, value: String, onChange: (String) -> Unit,
    imeAction: ImeAction, error: Boolean = false) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
    Scaffold(topBar = { FarmAppBar() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FarmHero(title, subtitle)
                content()
            }
        }
    }
}
