package io.github.saikumar1.nextup.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.saikumar1.nextup.data.Priority

/** Short name: "High", "Medium", "Low", "None". */
val Priority.label: String
    get() = when (this) {
        Priority.HIGH -> "High"
        Priority.MEDIUM -> "Medium"
        Priority.LOW -> "Low"
        Priority.NONE -> "None"
    }

/** Group / badge name: "High", "Medium", "Low", "No priority". */
val Priority.groupLabel: String
    get() = if (this == Priority.NONE) "No priority" else label

/**
 * Jira-style shapes so priorities differ by shape, not only by color:
 * ⇈ high, = medium, ⇊ low, nothing for none.
 */
val Priority.icon: ImageVector?
    get() = when (this) {
        Priority.HIGH -> Icons.Filled.KeyboardDoubleArrowUp
        Priority.MEDIUM -> Icons.Filled.DragHandle
        Priority.LOW -> Icons.Filled.KeyboardDoubleArrowDown
        Priority.NONE -> null
    }

/** Crimson (the brand accent) for high, amber for medium, blue for low. */
@Composable
fun Priority.color(): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (this) {
        Priority.HIGH -> MaterialTheme.colorScheme.primary
        Priority.MEDIUM -> if (dark) Color(0xFFF0B455) else Color(0xFFB26A00)
        Priority.LOW -> if (dark) Color(0xFF8AB4F8) else Color(0xFF2F6FCC)
        Priority.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/** The priority's icon in its color; draws nothing for [Priority.NONE]. */
@Composable
fun PriorityIcon(
    priority: Priority,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    contentDescription: String? = "${priority.label} priority",
) {
    val icon = priority.icon ?: return
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = priority.color(),
        modifier = modifier.size(size),
    )
}

/** A small pill with icon and name, e.g. "⇈ High". */
@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (priority.icon != null) {
                PriorityIcon(priority, contentDescription = null)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                priority.groupLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (priority == Priority.NONE) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    priority.color()
                },
            )
        }
    }
}
