package io.github.saikumar1.nextup.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The user's theme choice: `true` = dark, `false` = light, `null` = follow the system
 * (the default until the user taps the theme toggle for the first time).
 */
class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _darkMode = MutableStateFlow(read())
    val darkMode: StateFlow<Boolean?> = _darkMode.asStateFlow()

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putString(KEY, if (dark) DARK else LIGHT).apply()
        _darkMode.value = dark
    }

    private fun read(): Boolean? = when (prefs.getString(KEY, null)) {
        DARK -> true
        LIGHT -> false
        else -> null
    }

    private companion object {
        const val KEY = "theme"
        const val DARK = "dark"
        const val LIGHT = "light"
    }
}
