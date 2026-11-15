package io.github.saikumar1.nextup.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Plays an animated WebP from res/raw on a loop (Android 9+). Older devices show its
 * first frame as a still image.
 */
@Composable
fun AnimatedEmoji(
    @RawRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        },
        update = { view ->
            view.contentDescription = contentDescription
            if (view.tag != resId) {
                view.tag = resId
                (view.drawable as? Animatable)?.stop()
                view.setImageDrawable(loadDrawable(view.context, resId))
                (view.drawable as? Animatable)?.start()
            }
        },
        onRelease = { view -> (view.drawable as? Animatable)?.stop() },
        modifier = modifier,
    )
}

private fun loadDrawable(context: Context, @RawRes resId: Int): Drawable? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, resId)).also {
            (it as? AnimatedImageDrawable)?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        }
    } else {
        BitmapFactory.decodeResource(context.resources, resId)
            ?.let { BitmapDrawable(context.resources, it) }
    }
