package io.github.saikumar1.nextup.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Hosts the app UI and animates theme changes as a circular reveal: the new light theme
 * grows up from the bottom edge, the new dark theme grows down from the top edge.
 *
 * [content] receives a `toggleTheme` callback; it snapshots the current frame, flips the
 * theme via [onThemeChange], then reveals the new theme over the snapshot.
 */
@Composable
fun ThemeRevealHost(
    isDark: Boolean,
    onThemeChange: (dark: Boolean) -> Unit,
    content: @Composable (toggleTheme: () -> Unit) -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(1f) }
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var fromBottom by remember { mutableStateOf(true) }
    val currentIsDark by rememberUpdatedState(isDark)

    val toggleTheme: () -> Unit = toggle@{
        if (snapshot != null) return@toggle // already animating
        scope.launch {
            val goingDark = !currentIsDark
            snapshot = layer.toImageBitmap()
            fromBottom = !goingDark // light rises from the bottom, dark falls from the top
            progress.snapTo(0f)
            onThemeChange(goingDark)
            try {
                progress.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
            } finally {
                snapshot = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                // Always record the live UI so a snapshot of the old theme is available.
                layer.record { this@drawWithContent.drawContent() }
                val old = snapshot
                if (old == null) {
                    drawLayer(layer)
                } else {
                    drawImage(old)
                    val center = Offset(size.width / 2f, if (fromBottom) size.height else 0f)
                    val radius = hypot(size.width / 2f, size.height) * progress.value
                    val circle = Path().apply { addOval(Rect(center, radius)) }
                    clipPath(circle) { drawLayer(layer) }
                }
            },
    ) {
        content(toggleTheme)
    }
}
