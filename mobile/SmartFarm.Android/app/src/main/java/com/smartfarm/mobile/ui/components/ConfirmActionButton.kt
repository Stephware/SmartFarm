package com.smartfarm.mobile.ui.components

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Destructive actions must not silently close their confirmation before the result is known.
 * [enabled] reflects the caller's busy state. A changed [successKey] explicitly acknowledges
 * success when the action leaves the current row visible (for example, regenerating a code).
 * Failure keeps the dialog and allows retry once the caller is ready.
 */
@Composable
fun ConfirmActionButton(
    label: String,
    message: String,
    enabled: Boolean = true,
    successKey: Any? = null,
    onConfirm: () -> Unit
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    var sawBusy by rememberSaveable { mutableStateOf(false) }
    var startedWithKey by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(enabled, submitting, successKey) {
        if (!submitting) return@LaunchedEffect
        when {
            successKey != null && successKey != startedWithKey -> {
                confirming = false
                submitting = false
                sawBusy = false
            }
            !enabled -> sawBusy = true
            sawBusy && enabled -> {
                // Busy completed but no explicit success was observed. Keep the dialog
                // and let the user inspect any reported error before trying again.
                submitting = false
                sawBusy = false
            }
        }
    }

    OutlinedButton(
        onClick = { confirming = true },
        enabled = enabled && !submitting,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) { Text(label) }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { if (!submitting && enabled) confirming = false },
            title = { Text("$label?") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    enabled = enabled && !submitting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        if (!submitting) {
                            startedWithKey = successKey
                            submitting = true
                            onConfirm()
                        }
                    }
                ) { Text(uiText(R.string.ui_confirm_eebdd24, "Confirm")) }
            },
            dismissButton = {
                TextButton(
                    enabled = enabled && !submitting,
                    onClick = { confirming = false }
                ) { Text(uiText(R.string.ui_cancel_19766ed, "Cancel")) }
            }
        )
    }
}
