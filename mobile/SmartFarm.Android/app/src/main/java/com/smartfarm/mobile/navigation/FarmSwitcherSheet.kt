package com.smartfarm.mobile.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.remote.dto.FarmDto
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone

/** Switch farms in one tap without changing or clearing saved work. */
@Composable
internal fun FarmSwitcherSheet(
    farms: List<FarmDto>, currentFarmId: String,
    onSelect: (String) -> Unit, onAllFarms: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 580.dp)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Switch farm", style = MaterialTheme.typography.headlineSmall)
            Text("Choose where you want to work.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            farms.forEach { farm ->
                OutlinedButton(onClick = { onSelect(farm.id) },
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(farm.name, style = MaterialTheme.typography.titleMedium)
                            Text(farm.locationText, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (farm.id == currentFarmId) {
                            FarmStatusChip("Current", FarmStatusTone.Active)
                        }
                    }
                }
            }
            if (farms.isEmpty()) Text("Open My farms to view or join a farm.")
            OutlinedButton(onClick = onAllFarms, modifier = Modifier.fillMaxWidth()) {
                Text("All farms · Create or join")
            }
        }
    }
}
