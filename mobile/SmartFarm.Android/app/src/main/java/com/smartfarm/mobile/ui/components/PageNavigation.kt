package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Supplied per back-stack entry, never inferred from the currently animating screen. */
data class PageNavigation(val title: String, val backLabel: String, val onBack: (() -> Unit)?)
val LocalPageNavigation = compositionLocalOf<PageNavigation?> { null }

@Composable
fun PageNavigationHeader(title: String? = null, onBack: (() -> Unit)? = null,
    backLabel: String = "Back", modifier: Modifier = Modifier) {
    val page = LocalPageNavigation.current
    val action = if (page != null) page.onBack else onBack
    val heading = page?.title ?: title
    if (action == null || heading == null) return
    val label = page?.backLabel ?: backLabel
    val tint = MaterialTheme.colorScheme.onSurface
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = action, modifier = Modifier.size(48.dp).testTag("page-back")
                .semantics { contentDescription = label }) {
                Canvas(Modifier.size(24.dp)) {
                    val u = size.minDimension / 24f
                    fun line(x: Float, y: Float, endX: Float, endY: Float) =
                        drawLine(tint, Offset(x*u, y*u), Offset(endX*u, endY*u), 2*u, StrokeCap.Round)
                    line(19f,12f,5f,12f); line(5f,12f,12f,5f); line(5f,12f,12f,19f)
                }
            }
            Text(heading, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() })
        }
    }
}
