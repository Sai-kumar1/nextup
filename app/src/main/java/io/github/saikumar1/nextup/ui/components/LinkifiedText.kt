package io.github.saikumar1.nextup.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import io.github.saikumar1.nextup.util.Link
import io.github.saikumar1.nextup.util.findLinks

/**
 * Text whose web links are highlighted: tap a link to open it in the browser,
 * long-press it to copy it to the clipboard.
 */
@Composable
fun LinkifiedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val links = remember(text) { findLinks(text) }
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, links, linkColor) {
        buildAnnotatedString {
            append(text)
            links.forEach { link ->
                addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    link.range.first,
                    link.range.last + 1,
                )
            }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

    fun linkAt(position: Offset): Link? {
        val result = layout ?: return null
        val line = result.getLineForVerticalPosition(position.y)
        // Ignore touches past the end of a line, which would otherwise snap to its last character.
        if (position.x < result.getLineLeft(line) || position.x > result.getLineRight(line)) return null
        val offset = result.getOffsetForPosition(position)
        return links.firstOrNull { offset in it.range }
    }

    val gestures = if (links.isEmpty()) {
        Modifier
    } else {
        Modifier.pointerInput(links) {
            detectTapGestures(
                onTap = { position -> linkAt(position)?.let { openLink(context, it.url) } },
                onLongPress = { position ->
                    linkAt(position)?.let { link ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboard.setText(AnnotatedString(link.url))
                        // Android 13+ shows its own "Copied" confirmation.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
        }
    }

    Text(
        text = annotated,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        onTextLayout = { layout = it },
        modifier = modifier.then(gestures),
    )
}

private fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
    }
}
