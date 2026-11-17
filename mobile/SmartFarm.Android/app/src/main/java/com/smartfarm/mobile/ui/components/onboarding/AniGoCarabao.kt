package com.smartfarm.mobile.ui.components.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * AniGo's original, reusable, resolution-independent carabao illustration.
 *
 * One stable character silhouette is used for all poses: tan salakot, curved cream horns,
 * warm brown coat, dark-green AniGo kerchief and pale muzzle. Phase 2 uses still frames.
 * The four bounded frame variants are reserved for Phase 3's one-shot pose animations.
 *
 * All artwork uses Compose Canvas paths; no downloaded, unlicensed or remotely loaded files.
 */
enum class AniGoCarabaoPose {
    WAVE, GUIDE, PLANT, RECORD, HARVEST, CELEBRATE
}

private val Outline = Color(0xFF453020)
private val Body = Color(0xFFA98265)
private val BodyLight = Color(0xFFC09675)
private val BodyShade = Color(0xFF825C44)
private val Horn = Color(0xFFFFE9C2)
private val HornShade = Color(0xFFE9C99B)
private val Hat = Color(0xFFE7B562)
private val HatLight = Color(0xFFFFD584)
private val HatShade = Color(0xFFC48B3D)
private val Kerchief = Color(0xFF10583B)
private val Cream = Color(0xFFFFFCF1)
private val Gold = Color(0xFFF3B530)
private val Leaf = Color(0xFF428751)

@Composable
fun AniGoCarabao(
    pose: AniGoCarabaoPose,
    modifier: Modifier = Modifier,
    frame: Int = 0
) {
    val key = ((frame % 4) + 4) % 4
    Canvas(modifier) {
        val sx = size.width / 400f
        val sy = size.height / 400f
        withTransform({ scale(sx, sy, pivot = Offset.Zero) }) {
            drawOval(Color(0x17392518), Offset(83f, 369f), Size(246f, 15f))
            drawTail()
            drawLegs()
            drawTorso()
            drawArms(pose, key)
            drawEars()
            drawHorns()
            drawHead()
            drawFace(pose, key)
            drawHat()
            drawBandana()
            when (pose) {
                AniGoCarabaoPose.PLANT -> drawSeedling(key)
                AniGoCarabaoPose.RECORD -> drawClipboard(key)
                AniGoCarabaoPose.HARVEST -> drawHarvestBasket()
                else -> Unit
            }
            if (pose == AniGoCarabaoPose.WAVE || pose == AniGoCarabaoPose.CELEBRATE ||
                pose == AniGoCarabaoPose.GUIDE
            ) {
                val lift = if (key == 2) -4f else 0f
                drawPath(
                    path {
                        moveTo(336f, 137f + lift); lineTo(347f, 120f + lift)
                        moveTo(350f, 156f + lift); lineTo(372f, 153f + lift)
                        moveTo(330f, 118f + lift); lineTo(332f, 101f + lift)
                    }, Gold, style = Stroke(7f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

private fun path(build: Path.() -> Unit): Path = Path().apply(build)

private fun DrawScope.shape(path: Path, fill: Color, strokeWidth: Float = 4f) {
    drawPath(path, fill)
    drawPath(path, Outline, style = Stroke(
        width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round
    ))
}

private fun DrawScope.line(path: Path, color: Color = Outline, width: Float = 5f) {
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawTail() {
    shape(path {
        moveTo(127f, 311f); cubicTo(83f, 328f, 72f, 363f, 49f, 345f)
        cubicTo(41f, 338f, 41f, 322f, 47f, 313f)
        cubicTo(51f, 330f, 56f, 336f, 64f, 329f)
        cubicTo(74f, 319f, 90f, 291f, 127f, 292f); close()
    }, BodyShade)
}

private fun DrawScope.drawLegs() {
    shape(path {
        moveTo(130f, 310f); cubicTo(137f, 307f, 183f, 307f, 193f, 318f)
        lineTo(184f, 368f); quadraticTo(178f, 375f, 143f, 375f)
        quadraticTo(129f, 373f, 126f, 359f); close()
    }, BodyShade)
    shape(path {
        moveTo(211f, 319f); quadraticTo(232f, 309f, 270f, 315f)
        lineTo(283f, 361f); quadraticTo(287f, 375f, 270f, 377f)
        lineTo(236f, 377f); quadraticTo(222f, 370f, 218f, 349f); close()
    }, BodyShade)
    drawPath(path {
        moveTo(128f, 352f); quadraticTo(149f, 362f, 185f, 356f)
        moveTo(223f, 353f); quadraticTo(252f, 365f, 281f, 351f)
    }, Color(0xFF674532), style = Stroke(4f, cap = StrokeCap.Round))
}

private fun DrawScope.drawTorso() {
    shape(path {
        moveTo(145f, 241f)
        cubicTo(119f, 251f, 107f, 287f, 112f, 333f)
        cubicTo(118f, 359f, 158f, 364f, 206f, 362f)
        cubicTo(276f, 366f, 305f, 346f, 306f, 307f)
        cubicTo(307f, 276f, 288f, 251f, 259f, 241f)
        close()
    }, Body)
    drawOval(BodyLight, Offset(142f, 267f), Size(131f, 76f))
}

private fun DrawScope.drawArms(pose: AniGoCarabaoPose, frame: Int) {
    val lift = if (frame == 1 || frame == 3) 7f else if (frame == 2) 14f else 0f
    val bothUp = pose == AniGoCarabaoPose.CELEBRATE
    if (bothUp) {
        shape(path {
            moveTo(151f, 270f); cubicTo(111f, 255f, 78f, 213f - lift, 70f, 185f - lift)
            cubicTo(65f, 167f - lift, 87f, 161f - lift, 99f, 178f - lift)
            lineTo(162f, 236f); close()
        }, Body)
    } else {
        shape(path {
            moveTo(148f, 266f); cubicTo(110f, 278f, 96f, 309f, 108f, 324f)
            cubicTo(124f, 335f, 144f, 308f, 164f, 294f); close()
        }, Body)
    }
    when (pose) {
        AniGoCarabaoPose.WAVE, AniGoCarabaoPose.CELEBRATE -> shape(path {
            moveTo(268f, 256f)
            cubicTo(310f, 222f, 321f, 170f - lift, 330f, 144f - lift)
            cubicTo(334f, 124f - lift, 358f, 130f - lift, 358f, 151f - lift)
            cubicTo(355f, 196f, 333f, 266f, 284f, 296f); close()
        }, Body)
        AniGoCarabaoPose.GUIDE -> shape(path {
            moveTo(273f, 257f); cubicTo(306f, 246f, 326f, 240f - lift, 353f, 222f - lift)
            cubicTo(369f, 212f - lift, 377f, 231f - lift, 365f, 244f - lift)
            cubicTo(339f, 273f, 302f, 295f, 284f, 293f); close()
        }, Body)
        else -> shape(path {
            moveTo(270f, 263f); cubicTo(299f, 273f, 320f, 311f, 307f, 327f)
            cubicTo(296f, 342f, 272f, 311f, 256f, 296f); close()
        }, Body)
    }
}

private fun DrawScope.drawEars() {
    shape(path {
        moveTo(120f, 173f); cubicTo(74f, 143f, 48f, 177f, 69f, 208f)
        cubicTo(89f, 231f, 120f, 209f, 145f, 187f); close()
    }, BodyShade)
    drawOval(Color(0xFFD59A89), Offset(76f, 176f), Size(47f, 23f))
    shape(path {
        moveTo(274f, 183f); cubicTo(312f, 160f, 352f, 177f, 337f, 205f)
        cubicTo(319f, 226f, 286f, 219f, 262f, 194f); close()
    }, BodyShade)
    drawOval(Color(0xFFD59A89), Offset(289f, 184f), Size(42f, 20f))
}

private fun DrawScope.drawHorns() {
    shape(path {
        moveTo(125f, 149f); cubicTo(82f, 156f, 47f, 143f, 54f, 101f)
        cubicTo(58f, 78f, 74f, 70f, 84f, 66f)
        cubicTo(68f, 103f, 95f, 116f, 138f, 112f); close()
    }, Horn)
    shape(path {
        moveTo(268f, 150f); cubicTo(311f, 153f, 345f, 142f, 344f, 99f)
        cubicTo(343f, 76f, 331f, 65f, 318f, 63f)
        cubicTo(333f, 98f, 304f, 116f, 257f, 112f); close()
    }, Horn)
    line(path {
        moveTo(72f, 121f); quadraticTo(97f, 138f, 125f, 125f)
        moveTo(282f, 131f); quadraticTo(306f, 139f, 328f, 125f)
    }, HornShade, 5f)
}

private fun DrawScope.drawHead() {
    shape(path {
        moveTo(111f, 126f)
        cubicTo(129f, 95f, 182f, 89f, 216f, 96f)
        cubicTo(295f, 99f, 323f, 155f, 310f, 219f)
        cubicTo(304f, 256f, 262f, 279f, 208f, 276f)
        cubicTo(143f, 279f, 96f, 244f, 98f, 189f)
        cubicTo(98f, 161f, 103f, 143f, 111f, 126f); close()
    }, Body)
    drawOval(BodyLight, Offset(124f, 129f), Size(159f, 108f))
    shape(path {
        moveTo(171f, 178f); cubicTo(192f, 162f, 241f, 163f, 263f, 178f)
        cubicTo(292f, 203f, 281f, 238f, 250f, 249f)
        cubicTo(203f, 263f, 157f, 240f, 157f, 208f)
        cubicTo(157f, 195f, 161f, 186f, 171f, 178f); close()
    }, Color(0xFFD4AD89), 3f)
}

private fun DrawScope.drawFace(pose: AniGoCarabaoPose, frame: Int) {
    val happy = pose == AniGoCarabaoPose.GUIDE || pose == AniGoCarabaoPose.HARVEST ||
        pose == AniGoCarabaoPose.CELEBRATE
    line(path {
        moveTo(146f, 171f); quadraticTo(160f, 150f, 173f, 169f)
        moveTo(227f, 164f); quadraticTo(242f, 146f, 256f, 165f)
    }, Outline, 7f)
    drawCircle(Outline, 4f, Offset(204f, 206f))
    if (happy) {
        shape(path {
            moveTo(187f, 218f)
            quadraticTo(211f, 249f, 240f, 218f)
            quadraticTo(220f, 259f, 199f, 250f)
            quadraticTo(187f, 239f, 187f, 218f); close()
        }, Color(0xFF7F3B2A), 3f)
        drawOval(Color(0xFFDE7863), Offset(207f, 236f), Size(24f, 12f))
    } else {
        line(path { moveTo(189f, 218f); quadraticTo(214f, 239f, 240f, 214f) }, Outline, 5f)
    }
    if (pose == AniGoCarabaoPose.RECORD || pose == AniGoCarabaoPose.PLANT) {
        // A small neutral brow hint keeps task-focused poses gentle instead of cartoonish.
        line(path { moveTo(153f, 143f); quadraticTo(164f, 139f + frame, 176f, 146f) }, Outline, 3f)
    }
}

private fun DrawScope.drawHat() {
    shape(path {
        moveTo(116f, 128f); lineTo(135f, 84f)
        cubicTo(148f, 47f, 172f, 34f, 198f, 34f)
        cubicTo(241f, 30f, 267f, 54f, 280f, 106f)
        lineTo(289f, 124f); close()
    }, Hat)
    shape(path {
        moveTo(128f, 113f); cubicTo(172f, 93f, 242f, 98f, 281f, 119f)
        cubicTo(308f, 130f, 309f, 142f, 299f, 149f)
        cubicTo(283f, 156f, 260f, 145f, 221f, 139f)
        cubicTo(170f, 134f, 137f, 156f, 113f, 149f)
        cubicTo(93f, 144f, 96f, 127f, 128f, 113f); close()
    }, HatLight)
    line(path { moveTo(137f, 98f); quadraticTo(204f, 78f, 273f, 104f) }, HatShade, 5f)
    line(path { moveTo(155f, 70f); quadraticTo(199f, 59f, 242f, 75f) }, Color(0xFFFAE2A9), 5f)
}

private fun DrawScope.drawBandana() {
    shape(path {
        moveTo(152f, 258f); quadraticTo(197f, 278f, 258f, 254f)
        lineTo(224f, 332f); quadraticTo(212f, 340f, 204f, 328f)
        lineTo(174f, 300f); close()
    }, Kerchief, 3f)
    // Simple farm-A emblem; identical motif across all poses.
    drawPath(path {
        moveTo(194f, 308f); lineTo(205f, 285f); lineTo(219f, 309f)
        moveTo(199f, 302f); lineTo(215f, 302f)
    }, Cream, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    line(path {
        moveTo(191f, 315f); quadraticTo(209f, 307f, 224f, 317f)
    }, Color(0xFFBFD8BA), 2f)
}

private fun DrawScope.drawSeedling(frame: Int) {
    val bob = if (frame == 2) -5f else 0f
    drawOval(Color(0xFFC9A168), Offset(286f, 355f), Size(83f, 18f))
    line(path { moveTo(327f, 357f); quadraticTo(325f, 324f + bob, 330f, 297f + bob) },
        Color(0xFF286E43), 5f)
    shape(path {
        moveTo(329f, 331f + bob); cubicTo(289f, 320f + bob, 293f, 301f + bob, 296f, 298f + bob)
        cubicTo(317f, 297f + bob, 330f, 308f + bob, 329f, 331f + bob); close()
    }, Leaf, 2f)
    shape(path {
        moveTo(328f, 321f + bob); cubicTo(336f, 290f + bob, 354f, 288f + bob, 364f, 290f + bob)
        cubicTo(361f, 313f + bob, 347f, 326f + bob, 328f, 321f + bob); close()
    }, Color(0xFF74B96B), 2f)
}

private fun DrawScope.drawClipboard(frame: Int) {
    val y = if (frame == 2) -3f else 0f
    shape(path {
        moveTo(264f, 270f + y); lineTo(343f, 272f + y)
        lineTo(339f, 365f + y); lineTo(267f, 362f + y); close()
    }, Color(0xFFDCAC70))
    shape(path {
        moveTo(272f, 280f + y); lineTo(335f, 282f + y)
        lineTo(331f, 354f + y); lineTo(275f, 352f + y); close()
    }, Cream, 2f)
    shape(path {
        moveTo(291f, 265f + y); quadraticTo(300f, 255f + y, 310f, 264f + y)
        lineTo(315f, 274f + y); lineTo(288f, 273f + y); close()
    }, Color(0xFF9B9D92), 2f)
    for (n in 0..2) {
        val yy = 298f + n * 17f + y
        drawRect(Color(0xFF5CA160), Offset(285f, yy), Size(11f, 11f))
        line(path { moveTo(300f, yy + 6f); lineTo(324f, yy + 6f) }, Color(0xFFBBB9A8), 3f)
    }
}

private fun DrawScope.drawHarvestBasket() {
    shape(path {
        moveTo(244f, 312f); quadraticTo(287f, 270f, 337f, 308f)
        lineTo(327f, 360f); quadraticTo(285f, 376f, 254f, 355f); close()
    }, Hat, 3f)
    shape(path {
        moveTo(252f, 299f); quadraticTo(278f, 269f, 298f, 299f)
        quadraticTo(313f, 267f, 329f, 303f)
        quadraticTo(350f, 274f, 346f, 311f); close()
    }, Leaf, 3f)
    drawOval(Color(0xFFFFC14A), Offset(268f, 279f), Size(25f, 43f))
    drawOval(Color(0xFFFFC14A), Offset(300f, 281f), Size(24f, 38f))
    drawCircle(Color(0xFFDE6D48), 15f, Offset(310f, 304f))
    line(path { moveTo(252f, 319f); quadraticTo(293f, 332f, 333f, 319f) }, HatShade, 4f)
    line(path { moveTo(264f, 338f); quadraticTo(292f, 348f, 321f, 337f) }, HatShade, 4f)
}

@Preview(showBackground = true)
@Composable
private fun CarabaoPreview() {
    AniGoCarabao(pose = AniGoCarabaoPose.WAVE, modifier = Modifier.size(280.dp))
}
