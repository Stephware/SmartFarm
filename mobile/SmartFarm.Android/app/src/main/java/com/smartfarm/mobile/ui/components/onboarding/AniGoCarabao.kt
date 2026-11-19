package com.smartfarm.mobile.ui.components.onboarding

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.R
import com.smartfarm.mobile.ui.theme.SmartFarmTheme

/**
 * AniGo's approved, illustrated carabao, rendered using 12 transparent raster
 * frames from a single bundled WebP atlas (four columns by three rows).
 *
 * Replaces the old path-generated shapes, which could never faithfully match
 * the approved mascot's face, salakot, horns, proportions or soft shading.
 * The art is entirely local: no server, third-party dependency, audio, or GIF.
 */
enum class AniGoCarabaoPose {
    WAVE, GUIDE, PLANT, RECORD, HARVEST, CELEBRATE
}

private const val FramePx = 256
private const val Columns = 4
private const val FrameCount = 12

/** Atlas ordering: walk0..3; talk-closed/open, wave, point; plant, record, harvest, celebrate. */
internal fun aniGoSpriteIndex(
    pose: AniGoCarabaoPose,
    frame: Int = 0,
    walking: Boolean = false,
    talking: Boolean = false
): Int = when {
    walking -> Math.floorMod(frame, 4)
    talking -> if (Math.floorMod(frame, 2) == 0) 4 else 5
    else -> when (pose) {
        AniGoCarabaoPose.WAVE -> 6
        AniGoCarabaoPose.GUIDE -> 7
        AniGoCarabaoPose.PLANT -> 8
        AniGoCarabaoPose.RECORD -> 9
        AniGoCarabaoPose.HARVEST -> 10
        AniGoCarabaoPose.CELEBRATE -> 11
    }
}

@Composable
fun AniGoCarabao(
    pose: AniGoCarabaoPose,
    modifier: Modifier = Modifier,
    frame: Int = 0,
    walking: Boolean = false,
    talking: Boolean = false
) {
    val resources = LocalContext.current.resources
    val images = remember(resources) {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val sheet = requireNotNull(
            BitmapFactory.decodeResource(resources, R.drawable.ani_go_carabao_atlas, options)
        ) { "AniGo illustrated mascot atlas was not packaged with Android resources" }
        check(sheet.width == FramePx * Columns && sheet.height == FramePx * 3) {
            "AniGo sprite atlas dimensions must be 1024×768"
        }
        val frames = List(FrameCount) { position ->
            Bitmap.createBitmap(
                sheet,
                (position % Columns) * FramePx,
                (position / Columns) * FramePx,
                FramePx,
                FramePx
            ).asImageBitmap()
        }
        sheet.recycle()
        frames
    }
    val index = aniGoSpriteIndex(pose, frame, walking, talking)
    Image(
        bitmap = images[index],
        contentDescription = when {
            walking -> "AniGo carabao walking"
            talking -> "AniGo carabao speaking"
            pose == AniGoCarabaoPose.PLANT -> "AniGo carabao planting a seedling"
            pose == AniGoCarabaoPose.RECORD -> "AniGo carabao recording fieldwork"
            pose == AniGoCarabaoPose.HARVEST -> "AniGo carabao holding a harvest basket"
            pose == AniGoCarabaoPose.CELEBRATE -> "AniGo carabao celebrating"
            pose == AniGoCarabaoPose.GUIDE -> "AniGo carabao pointing to farm records"
            pose == AniGoCarabaoPose.WAVE -> "AniGo carabao waving"
            else -> "AniGo carabao mascot"
        },
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun AniGoCarabaoPreview() {
    SmartFarmTheme { AniGoCarabao(pose = AniGoCarabaoPose.WAVE, modifier = Modifier.size(220.dp)) }
}
