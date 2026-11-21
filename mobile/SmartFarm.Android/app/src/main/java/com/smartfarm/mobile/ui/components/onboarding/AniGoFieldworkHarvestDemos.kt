package com.smartfarm.mobile.ui.components.onboarding

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Phase 5: two *illustrative* demonstrations controlled by a finite 0..1 progress value.
 *
 * Nothing here calls a ViewModel, repository, database, Room sync worker or API.
 * In particular, "saved locally" represents a pending example, NOT a server upload.
 * Harvest figures represent two distinct partial harvests in the same unit (kg).
 */
private val FieldGreen: Color @Composable get() = MaterialTheme.colorScheme.primary
private val FieldGold: Color @Composable get() = MaterialTheme.colorScheme.secondary
private val FieldMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val FieldGrey: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val FieldAmber: Color @Composable get() = MaterialTheme.colorScheme.secondary

/** Checklist builds in order; final state explicitly means queued, not synchronized. */
@Composable
fun AniGoFieldworkDemo(progress: Float, modifier: Modifier = Modifier, compact: Boolean = false) {
    val p = progress.coerceIn(0f, 1f)
    val stage = when {
        p >= .82f -> 4
        p >= .57f -> 3
        p >= .30f -> 2
        else -> 1
    }
    Box(modifier.semantics {
        contentDescription = uiText(R.string.ui_sample_fieldwork_step_1_s_of_4_local_save_pending_sy_bfadacc, "Sample fieldwork, step %1\$s of 4; local save pending sync; demonstration only", (stage))
    }) {
        ExampleCard(uiText(R.string.ui_today_s_fieldwork_e324ea2, "Today's fieldwork"), uiText(R.string.ui_sample_north_plot_fd4a252, "Sample: North Plot")) {
            if (p >= .10f) {
                IntroRow(
                    title = uiText(R.string.ui_watering_2ee1114, "Watering"),
                    subtext = uiText(R.string.ui_activity_recorded_aa3013c, "Activity recorded"),
                    opacity = reveal(p, .10f), compact = compact
                )
            }
            if (p >= .36f) {
                IntroRow(
                    title = uiText(R.string.ui_leaf_inspection_716922b, "Leaf inspection"),
                    subtext = uiText(R.string.ui_observation_added_3488920, "Observation added"),
                    opacity = reveal(p, .36f), compact = compact
                )
            }
            if (p >= .59f) {
                IntroRow(
                    title = uiText(R.string.ui_photo_d84eeba, "Photo"),
                    subtext = uiText(R.string.ui_attached_to_record_c67770e, "Attached to record"),
                    opacity = reveal(p, .59f), compact = compact
                )
            }
            // The example shows the important distinction between recording on a
            // phone and having the server acknowledge that record.
            if (p >= .79f) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .graphicsLayer { alpha = reveal(p, .79f) },
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        uiText(R.string.ui_saved_on_device_2e2c6ae, "Saved on device"),
                        color = FieldGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        uiText(R.string.ui_pending_sync_e45ceb7, "Pending sync"),
                        color = FieldAmber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** Two independent partial harvests (50 kg + 35 kg) become an 85 kg summary.
 * The supplies view is separate; this visualization does not invent an
 * automatic inventory deduction or a confirmed server-side harvest.
 */
@Composable
fun AniGoHarvestDemo(progress: Float, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    val stage = when {
        p >= .82f -> 4
        p >= .60f -> 3
        p >= .30f -> 2
        else -> 1
    }
    val firstRecorded = p >= .28f
    val secondRecorded = p >= .58f
    val totalKg = (if (firstRecorded) 50 else 0) + (if (secondRecorded) 35 else 0)
    Box(modifier.semantics {
        contentDescription = uiText(R.string.ui_sample_harvest_step_1_s_of_4_two_separate_harvest_ba_0ca2a5c, "Sample harvest, step %1\$s of 4; two separate harvest batches total %2\$s kilograms; demonstration only", (stage), (totalKg))
    }) {
        ExampleCard(uiText(R.string.ui_harvest_records_cfad96b, "Harvest records"), uiText(R.string.ui_sample_rice_crop_9f5293c, "Sample: Rice crop")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    uiText(R.string.ui_1_s_kg_a289dfb, "%1\$s kg", (totalKg)),
                    color = FieldGreen,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    uiText(R.string.ui_recorded_3d96a45, "recorded"),
                    color = FieldMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(5.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(Modifier.weight(50f).height(5.dp).clip(CircleShape)
                    .background(if (firstRecorded) FieldGold else FieldGrey))
                Box(Modifier.weight(35f).height(5.dp).clip(CircleShape)
                    .background(if (secondRecorded) FieldGreen else FieldGrey))
            }
            if (firstRecorded) {
                IntroRow(
                    title = uiText(R.string.ui_batch_1_50_kg_3ba3986, "Batch 1 · 50 kg"),
                    subtext = uiText(R.string.ui_partial_harvest_41db432, "Partial harvest"),
                    opacity = reveal(p, .28f),
                    color = FieldGold
                )
            }
            if (secondRecorded) {
                IntroRow(
                    title = uiText(R.string.ui_batch_2_35_kg_9071662, "Batch 2 · 35 kg"),
                    subtext = uiText(R.string.ui_partial_harvest_41db432, "Partial harvest"),
                    opacity = reveal(p, .58f)
                )
            }
            if (p >= .80f) {
                Text(
                    uiText(R.string.ui_view_farm_supplies_separately_2304a46, "View farm supplies separately"),
                    style = MaterialTheme.typography.labelSmall,
                    color = FieldGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.graphicsLayer { alpha = reveal(p, .80f) }
                )
            }
        }
    }
}
