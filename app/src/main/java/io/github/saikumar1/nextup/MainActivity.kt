package io.github.saikumar1.nextup

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.saikumar1.nextup.ui.TaskScreen
import io.github.saikumar1.nextup.ui.theme.NextUpTheme
import io.github.saikumar1.nextup.ui.theme.ThemeRevealHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = (application as NextUpApp).container.preferences
        setContent {
            val storedDark by preferences.darkMode.collectAsStateWithLifecycle()
            val isDark = storedDark ?: isSystemInDarkTheme()
            val confirmPriorityChange by preferences.confirmPriorityChange.collectAsStateWithLifecycle()

            // Keep status/navigation bar icons readable for the app's (not the system's) theme.
            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { isDark },
                )
                onDispose {}
            }

            ThemeRevealHost(isDark = isDark, onThemeChange = preferences::setDarkMode) { toggle ->
                NextUpTheme(darkTheme = isDark) {
                    TaskScreen(
                        isDark = isDark,
                        onToggleTheme = toggle,
                        confirmPriorityChange = confirmPriorityChange,
                        onConfirmPriorityChangeSet = preferences::setConfirmPriorityChange,
                    )
                }
            }
        }
    }

    private companion object {
        // Same defaults androidx.activity uses for the 3-button navigation bar scrim.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xff, 0xff, 0xff)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
