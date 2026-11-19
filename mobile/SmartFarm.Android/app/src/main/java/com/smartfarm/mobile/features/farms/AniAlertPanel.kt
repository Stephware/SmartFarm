package com.smartfarm.mobile.features.farms

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.smartfarm.mobile.ui.components.onboarding.AniGoCarabao
import com.smartfarm.mobile.ui.components.onboarding.AniGoCarabaoPose
import com.smartfarm.mobile.features.tasks.AniAlertState
import com.smartfarm.mobile.ui.components.FarmSectionCard

@Composable
internal fun AniAlertPanel(state: AniAlertState, actorId: String?, onWeather: () -> Unit, onAcknowledge: (String) -> Unit) {
    if (actorId == null) return
    val prefs = LocalContext.current.getSharedPreferences("ani_alert_later_v1", android.content.Context.MODE_PRIVATE)
    var revision by remember(actorId) { mutableIntStateOf(0) }
    val alert = remember(state.items, actorId, revision) {
        state.items.firstOrNull { !prefs.getBoolean("$actorId:${it.id}", false) }
    }
    if (alert == null) {
        state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        return
    }
    FarmSectionCard(alert.title) {
        AniGoCarabao(AniGoCarabaoPose.GUIDE, Modifier.size(64.dp))
        Text(alert.message)
        Text(alert.source, style = MaterialTheme.typography.bodySmall)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onWeather) { Text("View advisory") }
            TextButton(onClick = { onAcknowledge(alert.id) }, enabled = !state.busy) { Text("Acknowledge") }
            TextButton(onClick = {
                prefs.edit().putBoolean("$actorId:${alert.id}", true).apply(); revision++
            }) { Text("Not now") }
        }
    }
}
