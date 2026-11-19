package com.smartfarm.mobile.ui.components.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.theme.LocalFarmAppearance
import kotlinx.coroutines.delay

/**
 * Separate welcome step following the animated intro. The talking mascot swaps
 * its real open- and closed-mouth images, like TUKI, without simulating speech
 * or blocking navigation. The loop is cancelled when this page is left.
 */
@Composable
internal fun AniGoWelcomePage(
    active: Boolean,
    activityResumed: Boolean,
    motionAllowed: Boolean,
    modifier: Modifier = Modifier
) {
    var talkingFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(active, activityResumed, motionAllowed) {
        if (!active || !activityResumed || !motionAllowed) {
            talkingFrame = 0
            return@LaunchedEffect
        }
        repeat(8) {
            talkingFrame = it % 2
            delay(245L)
        }
        talkingFrame = 0
    }

    val dark = LocalFarmAppearance.current.dark
    val foreground = MaterialTheme.colorScheme.onBackground
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewHeight = maxHeight
        val small = maxWidth < 340.dp || viewHeight < 390.dp
        val mascotSize = if (small) 206.dp else 252.dp
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewHeight)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Welcome to AniGo!",
                style = MaterialTheme.typography.headlineMedium,
                color = foreground,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Your friendly farm companion for smarter and simpler farm management.",
                style = MaterialTheme.typography.bodyLarge,
                color = secondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier.fillMaxWidth().height(if (small) 252.dp else 296.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            if (dark) listOf(Color(0xFF253F31), Color(0xFF183D2A))
                            else listOf(Color(0xFFFFF9E9), Color(0xFFE3F3D7))
                        )
                    )
            ) {
                AniGoCarabao(
                    pose = AniGoCarabaoPose.WAVE,
                    frame = talkingFrame,
                    talking = active && activityResumed && motionAllowed,
                    modifier = Modifier.align(Alignment.BottomStart).size(mascotSize)
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        .fillMaxWidth(.52f),
                    shape = RoundedCornerShape(
                        topStart = 20.dp, topEnd = 20.dp,
                        bottomStart = 6.dp, bottomEnd = 20.dp
                    ),
                    color = if (dark) Color(0xFF345340) else Color.White,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        "Hi! I'm Ani, your farm buddy.",
                        color = foreground,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(17.dp))
            Text(
                "Let's care for your farm, one step at a time.",
                color = foreground,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
