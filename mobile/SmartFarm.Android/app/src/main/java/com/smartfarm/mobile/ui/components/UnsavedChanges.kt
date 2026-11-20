package com.smartfarm.mobile.ui.components

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable

/** Destination-local UI state only. This class never reads or deletes the offline queue. */
class ExitGuard(saved: Map<String, Any?> = emptyMap()) {
    private val changes = mutableStateMapOf<String, Boolean>()
    private val accepted = mutableMapOf<String, List<String>>()
    private val inputs = mutableMapOf<String, List<String>>()
    init {
        saved.forEach { (key, value) ->
            when {
                key.startsWith("dirty/") -> changes[key.removePrefix("dirty/")] = value as Boolean
                key.startsWith("accepted/") -> {
                    @Suppress("UNCHECKED_CAST")
                    accepted[key.removePrefix("accepted/")] = value as List<String>
                }
            }
        }
    }
    internal fun snapshot(): Map<String, Any> =
        changes.mapKeys { "dirty/${it.key}" } + accepted.mapKeys { "accepted/${it.key}" }
    var pending by mutableStateOf<(() -> Unit)?>(null)
        private set
    val dirty: Boolean get() = changes.values.any { it }
    fun update(form: String, dirty: Boolean, values: List<String> = emptyList()) {
        inputs[form] = values
        changes[form] = dirty && (values.isEmpty() || accepted[form] != values)
    }
    /** Called only by confirmed mutation success, never by a tap or a failed save. */
    fun acceptAfterSave(form: String): () -> Unit {
        val submitted = inputs[form].orEmpty()
        return {
            accepted[form] = submitted
            changes[form] = inputs[form] != submitted
        }
    }
    fun clear(form: String) { changes.remove(form) }
    fun request(action: () -> Unit) { if (dirty) pending = action else action() }
    fun requestForm(form: String, action: () -> Unit) {
        if (changes[form] == true) pending = action else action()
    }
    fun keepEditing() { pending = null }
    fun discard() { val action = pending; pending = null; action?.invoke() }
}

val LocalExitGuard = compositionLocalOf<ExitGuard?> { null }
@Composable fun rememberExitGuard(existing: ExitGuard? = null): ExitGuard = rememberSaveable(saver = mapSaver(
    save = { it.snapshot() }, restore = { existing ?: ExitGuard(it) }
)) { existing ?: ExitGuard() }

/** Retain registrations when a LazyColumn form scrolls off-screen; its draft still exists. */
@Composable fun UnsavedChanges(form: String, dirty: Boolean, vararg inputs: String) {
    val guard = LocalExitGuard.current
    SideEffect { guard?.update(form, dirty, inputs.toList()) }
}

@Composable fun DiscardChangesDialog(guard: ExitGuard) {
    if (guard.pending != null) AlertDialog(
        onDismissRequest = guard::keepEditing,
        title = { Text(uiText(R.string.ui_discard_unsaved_changes_470e7ee, "Discard unsaved changes?")) },
        text = { Text(uiText(R.string.ui_your_unfinished_input_will_be_lost_records_already_s_0214fb5, "Your unfinished input will be lost. Records already saved on this phone will stay safe.")) },
        confirmButton = { TextButton(onClick = guard::discard) { Text(uiText(R.string.ui_discard_changes_f9bfa3d, "Discard changes")) } },
        dismissButton = { TextButton(onClick = guard::keepEditing) { Text(uiText(R.string.ui_keep_editing_e76fd2a, "Keep editing")) } }
    )
}
