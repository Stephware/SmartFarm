package com.smartfarm.mobile.ui.components

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

private enum class PhotoDisplayState { Loading, Ready, Error }
private class PhotoBinding(val model: Any?)

/**
 * Reusable Compose-compatible Glide image with loading and failure placeholders.
 * Private photo content is memory-cached only; Glide disk caching is disabled.
 */
@Composable
fun GlidePhoto(model: Any?, contentDescription: String, modifier: Modifier = Modifier) {
    var status by remember(model) {
        mutableStateOf(if (model == null) PhotoDisplayState.Error else PhotoDisplayState.Loading)
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    this.contentDescription = contentDescription
                    adjustViewBounds = true
                }
            },
            onReset = { image ->
                Glide.with(image).clear(image)
                image.tag = null
            },
            onRelease = { image ->
                Glide.with(image).clear(image)
                image.tag = null
            },
            update = { image ->
                image.contentDescription = contentDescription
                // State updates must not restart the Glide request.
                if ((image.tag as? PhotoBinding)?.model != model) {
                    Glide.with(image).clear(image)
                    image.tag = PhotoBinding(model)
                    if (model == null) {
                        status = PhotoDisplayState.Error
                    } else {
                        status = PhotoDisplayState.Loading
                        Glide.with(image).load(model)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(false)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    error: GlideException?, model: Any?,
                                    target: Target<Drawable>, isFirstResource: Boolean
                                ): Boolean {
                                    if ((image.tag as? PhotoBinding)?.model == model) {
                                        status = PhotoDisplayState.Error
                                    }
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable, model: Any, target: Target<Drawable>,
                                    dataSource: DataSource, isFirstResource: Boolean
                                ): Boolean {
                                    if ((image.tag as? PhotoBinding)?.model == model) {
                                        status = PhotoDisplayState.Ready
                                    }
                                    return false
                                }
                            })
                            .into(image)
                    }
                }
            }
        )
        when (status) {
            PhotoDisplayState.Loading ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            PhotoDisplayState.Error ->
                Text("Image unavailable", modifier = Modifier.align(Alignment.Center)
                    .padding(8.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            PhotoDisplayState.Ready -> Unit
        }
    }
}
