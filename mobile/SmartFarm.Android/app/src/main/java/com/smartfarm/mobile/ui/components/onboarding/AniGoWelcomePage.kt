package com.smartfarm.mobile.ui.components.onboarding

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

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
    // -1 represents the resting wave; 0/1 are the closed/open talking frames.
    // Returning to -1 after the short greeting prevents a frozen talking bust.
    var talkingFrame by remember { mutableIntStateOf(-1) }
    LaunchedEffect(active, activityResumed, motionAllowed) {
        talkingFrame = -1
        if (!active || !activityResumed || !motionAllowed) return@LaunchedEffect
        repeat(8) {
            talkingFrame = it % 2
            delay(245L)
        }
        talkingFrame = -1
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
                uiText(R.string.ui_meet_ani_your_farm_buddy_f79f2b4, "MEET ANI  ·  YOUR FARM BUDDY"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                uiText(R.string.ui_welcome_to_anigo_f8d1e80, "Welcome to AniGo!"),
                style = MaterialTheme.typography.headlineMedium,
                color = foreground,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                uiText(R.string.ui_your_farm_crops_and_harvests_made_a_little_easier_2efa5f8, "Your farm, crops, and harvests—made a little easier."),
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
                    frame = talkingFrame.coerceAtLeast(0),
                    talking = active && activityResumed && motionAllowed && talkingFrame >= 0,
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
                        uiText(R.string.ui_hi_i_m_ani_your_farm_buddy_895d6c0, "Hi! I'm Ani, your farm buddy."),
                        color = foreground,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(17.dp))
            Text(
                uiText(R.string.ui_i_ll_show_you_four_simple_ways_to_get_started_e867872, "I'll show you four simple ways to get started."),
                color = foreground,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
