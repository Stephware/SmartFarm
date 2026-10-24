package com.smartfarm.mobile.ui.components

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * Uses Glide's decoded-bitmap memory cache without persisting private images
 * to disk. API photos are fetched with authenticated Retrofit requests first.
 */
@Composable
fun GlidePhoto(model: Any?, contentDescription: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                this.contentDescription = contentDescription
                adjustViewBounds = true
            }
        },
        onReset = { image -> Glide.with(image).clear(image) },
        onRelease = { image -> Glide.with(image).clear(image) },
        update = { image ->
            if (model == null) Glide.with(image).clear(image)
            else Glide.with(image).load(model)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(false)
                .into(image)
        }
    )
}
