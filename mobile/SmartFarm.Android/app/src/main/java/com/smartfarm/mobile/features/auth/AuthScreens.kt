package com.smartfarm.mobile.features.auth

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import com.smartfarm.mobile.ui.components.UnsavedChanges
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
    UnsavedChanges("login", email.isNotEmpty() || password.isNotEmpty())
    val ready = looksLikeEmail(email) && password.isNotBlank()

    PhotoAuthScaffold(
        headline = uiText(R.string.ui_good_to_have_you_back_911caf4, "Good to have you back."),
        description = uiText(R.string.ui_your_fields_crops_and_team_all_in_one_connected_plac_759a787, "Your fields, crops and team — all in one connected place."),
        formTitle = uiText(R.string.ui_log_in_c189840, "Log in"),
        formSubtitle = uiText(R.string.ui_pick_up_right_where_you_left_off_4af7027, "Pick up right where you left off.")
    ) {
        OutlinedTextField(
            email, { email = it }, label = { Text(uiText(R.string.ui_email_address_f2488fd, "Email address")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            isError = email.isNotBlank() && !looksLikeEmail(email), singleLine = true
        )
        if (email.isNotBlank() && !looksLikeEmail(email)) {
            Text(uiText(R.string.ui_enter_a_valid_email_address_8a966af, "Enter a valid email address."), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        PasswordInput(uiText(R.string.ui_password_e7cf3ef, "Password"), password, { password = it }, imeAction = ImeAction.Done)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword, enabled = !state.busy) {
                Text(uiText(R.string.ui_forgot_password_30c1d8d, "Forgot password?"))
            }
        }
        Feedback(state)
        PhotoAuthPrimaryButton(
            label = uiText(R.string.ui_log_in_c189840, "Log in"),
            onClick = { onSubmit(email.trim(), password) },
            enabled = !state.busy && ready
        )
        Text(uiText(R.string.ui_new_here_38c1c44, "New here?"), color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        OutlinedButton(
            onClick = onRegister, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text(uiText(R.string.ui_create_account_798ca2c, "Create account")) }
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
    UnsavedChanges("registration", name.isNotEmpty() || email.isNotEmpty() || password.isNotEmpty())
    val canCreate = registrationReady(name, email, password)

    PhotoAuthScaffold(
        headline = uiText(R.string.ui_grow_with_confidence_acd8219, "Grow with confidence."),
        description = uiText(R.string.ui_plan_crops_organize_fieldwork_and_manage_your_farm_i_6c3d320, "Plan crops, organize fieldwork and manage your farm in one place."),
        formTitle = uiText(R.string.ui_create_your_account_9e70934, "Create your account"),
        formSubtitle = uiText(R.string.ui_one_account_can_manage_a_farm_or_join_a_team_3972cf8, "One account can manage a farm or join a team."),
        compactHero = true
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text(uiText(R.string.ui_full_name_f13a64b, "Full name")) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = name.isNotBlank() && name.trim().length !in 2..120
        )
        OutlinedTextField(
            email, { email = it }, label = { Text(uiText(R.string.ui_email_address_f2488fd, "Email address")) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(14.dp),
            isError = email.isNotBlank() && !looksLikeEmail(email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        if (email.isNotBlank() && !looksLikeEmail(email)) {
            Text(uiText(R.string.ui_enter_a_valid_email_address_8a966af, "Enter a valid email address."), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        PasswordInput(
            uiText(R.string.ui_password_e7cf3ef, "Password"), password, { password = it },
            imeAction = ImeAction.Done,
            error = password.isNotBlank() && !meetsPasswordGuidance(password)
        )
        Text(uiText(R.string.ui_use_10_128_characters_with_uppercase_lowercase_and_a_47fe104, "Use 10–128 characters with uppercase, lowercase and a number."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        Feedback(state)
        PhotoAuthPrimaryButton(
            label = uiText(R.string.ui_create_account_798ca2c, "Create account"),
            onClick = { onSubmit(name.trim(), email.trim(), password) },
            enabled = !state.busy && canCreate
        )
        Text(uiText(R.string.ui_after_registering_sign_in_to_create_a_farm_or_reques_75ec908, "After registering, sign in to create a farm or request to join one."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = onLogin, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text(uiText(R.string.ui_log_in_instead_9b718a8, "Log in instead")) }
    }
}

/** The API returns neutral feedback for known and unknown accounts alike. */
@Composable
fun ForgotPasswordScreen(
    state: AuthUiState,
    onRequest: (String) -> Unit,
    onLogin: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    val valid = looksLikeEmail(email)
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    PhotoAuthScaffold(
        headline = uiText(R.string.ui_need_help_signing_in_d860242, "Need help signing in?"),
        description = uiText(R.string.ui_keep_your_farm_work_within_reach_947bdf3,
            "Keep your farm work within reach."),
        formTitle = uiText(R.string.ui_forgot_your_password_afd25fd, "Forgot your password?"),
        formSubtitle = uiText(R.string.recovery_email_hint,
            "Enter your email to request password reset instructions."),
        compactHero = true
    ) {
        if (state.recoveryRequested) {
            Text(uiText(R.string.recovery_sent_title, "Check your email"),
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                }, style = MaterialTheme.typography.titleLarge)
            Text(uiText(R.string.recovery_sent_note,
                "If an active account exists and email delivery is configured, reset instructions will be sent. We cannot confirm whether an account exists for that address."),
                style = MaterialTheme.typography.bodyMedium)
            Text(uiText(R.string.recovery_follow_browser,
                "Open the secure email link in your browser to finish resetting your password."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        } else {
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text(uiText(R.string.recovery_email_label, "Email address")) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { focusManager.clearFocus() }),
                enabled = !state.recoveryBusy,
                isError = email.isNotBlank() && !valid,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("recovery-email")
            )
            if (email.isNotBlank() && !valid) {
                Text(uiText(R.string.ui_enter_a_valid_email_address_8a966af,
                    "Enter a valid email address."), color = MaterialTheme.colorScheme.error)
            }
            state.recoveryError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            }
            Button(
                onClick = { focusManager.clearFocus(); onRequest(email.trim()) },
                enabled = valid && !state.recoveryBusy,
                modifier = Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("request-password-reset")
            ) {
                if (state.recoveryBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 12.dp),
                        strokeWidth = 2.dp)
                }
                Text(if (state.recoveryBusy) uiText(R.string.recovery_requesting,
                    "Requesting reset instructions…") else
                    uiText(R.string.recovery_request, "Request reset instructions"))
            }
        }
        if (com.smartfarm.mobile.ui.components.LocalPageNavigation.current?.onBack == null) {
            TextButton(onClick = onLogin, enabled = !state.recoveryBusy) {
                Text(uiText(R.string.ui_back_to_log_in_30a8b4c, "Back to log in"))
            }
        }
    }
}

@Composable
fun AccountScreen(state: AuthUiState, onLogout: () -> Unit, onFarms: () -> Unit,
    onSync: () -> Unit, onNotifications: () -> Unit = {},
    pendingSyncCount: Int = 0, onReplayGuide: (() -> Unit)? = null) {
    var confirmingLogout by rememberSaveable { mutableStateOf(false) }
    val name = state.account?.fullName.orEmpty()
    AuthScaffold(uiText(R.string.ui_my_account_b53181a, "My account"), uiText(R.string.ui_your_details_and_personal_app_settings_2e54b7f, "Your details and personal app settings.")) {
        com.smartfarm.mobile.localization.LanguagePicker()
        FarmSectionCard(uiText(R.string.ui_signed_in_as_abc50e3, "Signed in as")) {
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
            Text(uiText(R.string.ui_farm_roles_and_membership_are_managed_in_each_farm_s_3301c40, "Farm roles and membership are managed in each farm's Team & access section."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onFarms,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Text(uiText(R.string.ui_my_farms_0517a09, "My farms"))
            }
        }
        FarmSectionCard(uiText(R.string.ui_appearance_3907fa7, "Appearance"), subtitle = uiText(R.string.ui_choose_a_comfortable_display_for_fieldwork_73b2bf0, "Choose a comfortable display for fieldwork.")) {
            val appearance = LocalFarmAppearance.current
            Row(Modifier.fillMaxWidth()
                .toggleable(value = appearance.dark, role = Role.Switch,
                    onValueChange = appearance.selectDark)
                .semantics { contentDescription = uiText(R.string.ui_dark_theme_3f11a5c, "Dark theme") }
                .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(uiText(R.string.ui_dark_theme_3f11a5c, "Dark theme"), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = appearance.dark, onCheckedChange = null)
            }
        }
        if (onReplayGuide != null) {
            FarmSectionCard(uiText(R.string.ui_help_onboarding_a9e1e52, "Help & onboarding"),
                subtitle = uiText(R.string.ui_review_how_to_organize_farms_crops_fieldwork_and_har_821be15, "Review how to organize farms, crops, fieldwork, and harvests.")) {
                OutlinedButton(
                    onClick = onReplayGuide,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                        .testTag("replay-anigo-guide")
                ) {
                    Text(uiText(R.string.ui_replay_anigo_guide_db21536, "Replay AniGo guide"))
                }
            }
        }
        if (pendingSyncCount > 0) {
            FarmSectionCard(uiText(R.string.ui_work_saved_on_this_phone_af769c5, "Work saved on this phone"),
                subtitle = uiText(R.string.ui_these_records_have_not_yet_been_confirmed_by_the_ser_bd2209e, "These records have not yet been confirmed by the server.")) {
                Text(uiText(R.string.ui_1_s_saved_2_s_waiting_or_needing_review_c26f284, "%1\$s saved %2\$s waiting or needing review.", (pendingSyncCount), (if (pendingSyncCount == 1) "record" else "records")),
                    style = MaterialTheme.typography.titleSmall)
                Button(onClick = onSync, modifier = Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)) {
                    Text(uiText(R.string.ui_review_offline_sync_31b3cbe, "Review offline sync"))
                }
            }
        }
        FarmSectionCard(uiText(R.string.ui_other_shortcuts_44725ba, "Other shortcuts")) {
            OutlinedButton(onClick = onNotifications,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Text(uiText(R.string.ui_notifications_7880118, "Notifications"))
            }
            if (pendingSyncCount == 0) {
                OutlinedButton(onClick = onSync,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                    Text(uiText(R.string.ui_offline_sync_843fdb9, "Offline sync"))
                }
            }
        }
        OutlinedButton(onClick = { confirmingLogout = true }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                .testTag("request-sign-out")) {
            Text(uiText(R.string.ui_sign_out_48f0d3d, "Sign out"))
        }
    }
    if (confirmingLogout) {
        AlertDialog(onDismissRequest = { confirmingLogout = false },
            title = { Text(uiText(R.string.ui_sign_out_of_your_account_b9a5c73, "Sign out of your account?")) },
            text = {
                Text(if (pendingSyncCount > 0)
                    uiText(R.string.ui_1_s_unsynced_2_s_saved_on_this_phone_for_this_accoun_2fe1db1, "%1\$s unsynced %2\$s saved on this phone for this account. ", (pendingSyncCount), (if (pendingSyncCount == 1) "record is" else "records are")) +
                        uiText(R.string.ui_they_will_not_sync_while_signed_out_sign_back_into_t_b536edf, "They will not sync while signed out. Sign back into the same account to resume. Unsaved form changes may be lost.")
                else uiText(R.string.ui_unsaved_form_changes_may_be_lost_your_server_confirm_637f7db, "Unsaved form changes may be lost. Your server-confirmed farm records will stay available when you sign back in."))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingLogout = false
                    onLogout()
                }, modifier = Modifier.testTag("confirm-sign-out")) { Text(uiText(R.string.ui_sign_out_48f0d3d, "Sign out")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingLogout = false }) { Text(uiText(R.string.ui_keep_working_5ed03f2, "Keep working")) }
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
                Text(if (visible) uiText(R.string.ui_hide_ac20a57, "Hide") else uiText(R.string.ui_show_0df6f1c, "Show"))
            }
        }
    )
}

@Composable
private fun Feedback(state: AuthUiState) {
    if (state.busy) CircularProgressIndicator()
    state.error?.let {
        Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
    }
    state.notice?.let {
        Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.primary,
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
