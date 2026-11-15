package io.github.saikumar1.nextup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Colors follow the design tokens of sai-kumar1.github.io (_sass/_tokens.scss):
 * no pure white screens and no pure black. Light is a soft gray canvas with white
 * "sheets" (cards, bottom sheets) and near-black text; dark uses layered dark grays
 * (lighter = raised) with softened text. The accent is the brand crimson.
 *
 * Cards and sheets use surfaceContainerLow, so it is the "sheet" color in both themes.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFFB8323F),              // --accent
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF6DFE2),     // --accent-soft, made visible as a fill
    onPrimaryContainer = Color(0xFF941F2C),   // --accent-strong
    inversePrimary = Color(0xFFFF7D88),
    secondary = Color(0xFF636368),            // --text-muted
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E8ED),
    onSecondaryContainer = Color(0xFF1D1D1F),
    tertiary = Color(0xFF941F2C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6DFE2),
    onTertiaryContainer = Color(0xFF941F2C),
    background = Color(0xFFF2F2F4),           // --canvas
    onBackground = Color(0xFF1D1D1F),         // --text
    surface = Color(0xFFF2F2F4),              // --canvas
    onSurface = Color(0xFF1D1D1F),            // --text
    surfaceVariant = Color(0xFFE8E8ED),
    onSurfaceVariant = Color(0xFF636368),     // --text-muted
    surfaceTint = Color(0xFFB8323F),
    inverseSurface = Color(0xFF2C2C30),
    inverseOnSurface = Color(0xFFF2F2F4),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFD2D2D7),       // --border
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF7F7F9),
    surfaceDim = Color(0xFFE4E4E8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),  // --bg / --surface: the white sheet
    surfaceContainer = Color(0xFFF5F5F7),     // --surface-2
    surfaceContainerHigh = Color(0xFFF7F7F9),
    surfaceContainerHighest = Color(0xFFE8E8ED),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7D88),              // --accent
    onPrimary = Color(0xFF1A1A1D),
    primaryContainer = Color(0xFF4A2229),     // --accent-soft over --bg
    onPrimaryContainer = Color(0xFFFF9AA3),   // --accent-strong
    inversePrimary = Color(0xFFB8323F),
    secondary = Color(0xFFA0A0A7),            // --text-muted
    onSecondary = Color(0xFF1A1A1D),
    secondaryContainer = Color(0xFF313136),
    onSecondaryContainer = Color(0xFFE4E4E9),
    tertiary = Color(0xFFFF9AA3),
    onTertiary = Color(0xFF1A1A1D),
    tertiaryContainer = Color(0xFF4A2229),
    onTertiaryContainer = Color(0xFFFF9AA3),
    background = Color(0xFF1A1A1D),           // --bg
    onBackground = Color(0xFFE4E4E9),         // --text
    surface = Color(0xFF1A1A1D),              // --bg
    onSurface = Color(0xFFE4E4E9),            // --text
    surfaceVariant = Color(0xFF2C2C31),
    onSurfaceVariant = Color(0xFFA0A0A7),     // --text-muted
    surfaceTint = Color(0xFFFF7D88),
    inverseSurface = Color(0xFFE4E4E9),
    inverseOnSurface = Color(0xFF1A1A1D),
    outline = Color(0xFF6E6E75),
    outlineVariant = Color(0xFF313136),       // --border
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF313136),
    surfaceDim = Color(0xFF121214),
    surfaceContainerLowest = Color(0xFF121214), // --canvas
    surfaceContainerLow = Color(0xFF222226),  // --surface: cards are raised
    surfaceContainer = Color(0xFF26262B),     // --surface-2
    surfaceContainerHigh = Color(0xFF2A2A2F),
    surfaceContainerHighest = Color(0xFF313136),
)

@Composable
fun NextUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
