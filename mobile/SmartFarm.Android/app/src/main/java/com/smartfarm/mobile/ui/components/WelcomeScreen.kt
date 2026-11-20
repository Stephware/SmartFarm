package com.smartfarm.mobile.ui.components

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

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
        headline = uiText(R.string.ui_your_farm_starts_here_affbd4f, "Your farm starts here."),
        description = uiText(R.string.ui_plan_crops_record_fieldwork_and_track_harvests_in_on_725071b, "Plan crops, record fieldwork and track harvests in one place."),
        formTitle = uiText(R.string.ui_get_started_61e8d44, "Get started"),
        formSubtitle = uiText(R.string.ui_manage_your_own_farm_or_join_an_existing_team_with_o_e43acdd, "Manage your own farm or join an existing team with one account.")
    ) {
        PhotoAuthPrimaryButton(label = uiText(R.string.ui_log_in_c189840, "Log in"), onClick = onLogin)
        OutlinedButton(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text(uiText(R.string.ui_create_account_798ca2c, "Create account")) }

        if (onPreviewGuide != null) {
            TextButton(onClick = onPreviewGuide, modifier = Modifier.fillMaxWidth()) {
                Text(uiText(R.string.ui_explore_anigo_with_our_farm_guide_23f29d5, "Explore AniGo with our farm guide"))
            }
        }

        // Diagnostics are never the prominent call-to-action and are stripped in release builds.
        if (BuildConfig.DEBUG) {
            TextButton(onClick = { showDiagnostics = !showDiagnostics }) {
                Text(if (showDiagnostics) uiText(R.string.ui_hide_developer_diagnostics_fa87423, "Hide developer diagnostics") else uiText(R.string.ui_developer_diagnostics_aab0a0a, "Developer diagnostics"))
            }
            if (showDiagnostics) {
                Text(status.message, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onCheckBackend, enabled = !status.checking) {
                    Text(if (status.checking) "Checking..." else uiText(R.string.ui_check_backend_82e9305, "Check backend"))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() { SmartFarmTheme { WelcomeScreen() } }
