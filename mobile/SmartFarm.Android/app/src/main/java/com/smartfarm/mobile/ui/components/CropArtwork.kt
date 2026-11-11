package com.smartfarm.mobile.ui.components

import androidx.annotation.DrawableRes
import com.smartfarm.mobile.R
import java.util.Locale

/** Visual category only; does not describe plant maturity or crop health. */
enum class CropArtworkKind { Tomato, Eggplant, Rice, Corn, Generic }

fun cropArtworkKind(name: String?): CropArtworkKind {
    val lower = name?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return when {
        "tomato" in lower || "kamatis" in lower -> CropArtworkKind.Tomato
        "eggplant" in lower || "talong" in lower || "aubergine" in lower -> CropArtworkKind.Eggplant
        "rice" in lower || "palay" in lower || "paddy" in lower -> CropArtworkKind.Rice
        "corn" in lower || "maize" in lower || "mais" in lower -> CropArtworkKind.Corn
        else -> CropArtworkKind.Generic
    }
}

@DrawableRes
fun cropArtworkRes(name: String?): Int = when (cropArtworkKind(name)) {
    CropArtworkKind.Tomato -> R.drawable.crop_tomato
    CropArtworkKind.Eggplant -> R.drawable.crop_eggplant
    CropArtworkKind.Rice -> R.drawable.crop_rice
    CropArtworkKind.Corn -> R.drawable.crop_corn
    CropArtworkKind.Generic -> R.drawable.crop_sprout
}
