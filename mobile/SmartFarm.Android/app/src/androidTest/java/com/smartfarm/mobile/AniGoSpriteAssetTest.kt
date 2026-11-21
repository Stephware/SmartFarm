package com.smartfarm.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.ui.components.onboarding.AniGoCarabaoPose
import com.smartfarm.mobile.ui.components.onboarding.aniGoSpriteIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks the actual packaged WebP sprites, not just the frame-index function.
 * A regression to a single still image would pass the old index-only tests
 * but would make walking and talking look frozen.
 */
@RunWith(AndroidJUnit4::class)
class AniGoSpriteAssetTest {
    @Test
    fun walkingAndTalkingFramesAreDifferentImages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val atlas = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ani_go_carabao_atlas,
            BitmapFactory.Options().apply { inScaled = false }
        )
        assertNotNull("The approved mascot atlas must be packaged into the APK", atlas)
        requireNotNull(atlas).useAtlas { image ->
            assertEquals(1024, image.width)
            assertEquals(768, image.height)
            assertTrue("Mascot must preserve transparency", image.hasAlpha())

            val walk = (0..3).map {
                aniGoSpriteIndex(AniGoCarabaoPose.WAVE, frame = it, walking = true)
            }
            assertEquals(listOf(0, 1, 2, 3), walk)
            for (frame in 0..2) {
                assertFalse(
                    "Walking frames ${frame} and ${frame + 1} are identical",
                    sameFrame(image, walk[frame], walk[frame + 1])
                )
            }
            val closed = aniGoSpriteIndex(
                AniGoCarabaoPose.WAVE, frame = 0, talking = true
            )
            val open = aniGoSpriteIndex(
                AniGoCarabaoPose.WAVE, frame = 1, talking = true
            )
            assertFalse(
                "Talking must alternate genuinely different open/closed-mouth images",
                sameFrame(image, closed, open)
            )
        }
    }

    private inline fun Bitmap.useAtlas(action: (Bitmap) -> Unit) {
        try { action(this) } finally { recycle() }
    }

    private fun sameFrame(source: Bitmap, first: Int, second: Int): Boolean {
        fun crop(index: Int) = Bitmap.createBitmap(
            source, (index % 4) * 256, (index / 4) * 256, 256, 256
        )
        val a = crop(first)
        val b = crop(second)
        return try { a.sameAs(b) } finally {
            a.recycle()
            b.recycle()
        }
    }
}
