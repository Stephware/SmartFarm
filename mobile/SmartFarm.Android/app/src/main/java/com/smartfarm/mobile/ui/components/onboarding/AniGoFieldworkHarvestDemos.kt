package com.smartfarm.mobile.ui.components.onboarding

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
private val FieldGreen = Color(0xFF145239)
private val FieldGold = Color(0xFFCBA14E)
private val FieldInk = Color(0xFF173D2B)
private val FieldMuted = Color(0xFF66756A)
private val FieldGrey = Color(0xFFE3E9DF)
private val FieldAmber = Color(0xFF966823)

/** Checklist builds in order; final state explicitly means queued, not synchronized. */
@Composable
fun AniGoFieldworkDemo(progress: Float, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    val stage = when {
        p >= .82f -> 4
        p >= .57f -> 3
        p >= .30f -> 2
        else -> 1
    }
    Box(modifier.semantics {
        contentDescription = "Sample fieldwork, step $stage of 4; local save pending sync; demonstration only"
    }) {
        ExampleCard("Today's fieldwork", "Sample: North Plot", "Inspection example") {
            if (p >= .10f) {
                IntroRow(
                    title = "Watering",
                    subtext = "Activity recorded",
                    opacity = reveal(p, .10f)
                )
            }
            if (p >= .36f) {
                IntroRow(
                    title = "Leaf inspection",
                    subtext = "Observation added",
                    opacity = reveal(p, .36f)
                )
            }
            if (p >= .59f) {
                IntroRow(
                    title = "Photo",
                    subtext = "Attached to record",
                    opacity = reveal(p, .59f)
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
                        "Saved on device",
                        color = FieldGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Pending sync",
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
        contentDescription = "Sample harvest, step $stage of 4; two separate harvest batches total $totalKg kilograms; demonstration only"
    }) {
        ExampleCard("Harvest records", "Sample: Rice crop", "Harvest example") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$totalKg kg",
                    color = FieldGreen,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "recorded",
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
                    title = "Batch 1 · 50 kg",
                    subtext = "Partial harvest",
                    opacity = reveal(p, .28f),
                    color = FieldGold
                )
            }
            if (secondRecorded) {
                IntroRow(
                    title = "Batch 2 · 35 kg",
                    subtext = "Partial harvest",
                    opacity = reveal(p, .58f)
                )
            }
            if (p >= .80f) {
                Text(
                    "View farm supplies separately",
                    style = MaterialTheme.typography.labelSmall,
                    color = FieldGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.graphicsLayer { alpha = reveal(p, .80f) }
                )
            }
        }
    }
}
